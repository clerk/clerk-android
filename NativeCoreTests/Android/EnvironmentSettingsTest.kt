package com.clerk.api

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.net.URI
import java.util.Base64
import kotlinx.coroutines.*
import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class EnvironmentSettingsTest {
  @Test fun sessionMinterEnabled() = auth(true)

  @Test fun sessionMinterOmittedDefaultsFalse() = auth(null)

  @Test fun developmentWarningEnabled() = display(true)

  @Test fun developmentWarningOmittedDefaultsFalse() = display(null)

  @Test fun supportEmailPreserved() = display(false, "help@example.com")

  @Test fun organizationDefaults() = organization(buildJsonObject {})

  @Test
  fun organizationNullChildrenUseDefaults() =
    organization(
      buildJsonObject {
        for (key in listOf("actions", "domains", "slug", "organization_creation_defaults")) put(
          key,
          JsonNull,
        )
      }
    )

  @Test
  fun completeOrganizationSettings() =
    organization(
      Json.parseToJsonElement(
          """{
    "enabled":true,"max_allowed_memberships":5,"force_organization_selection":true,
    "actions":{"admin_delete":true},"domains":{"enabled":true,"enrollment_modes":["automatic_invitation"],"default_role":"org:member"},
    "slug":{"disabled":true},"organization_creation_defaults":{"enabled":true}
  }"""
        )
        .jsonObject,
      true,
    )

  @Test fun immutableTruePreservesFactorSettings() = immutable(true)

  @Test fun immutableFalsePreservesFactorSettings() = immutable(false)

  @Test fun immutableOmittedPreservesFactorSettings() = immutable(null)

  @Test fun firstFactorPasskeyPreserved() = passkey(true, true)

  @Test fun registrationOnlyPasskeyPreserved() = passkey(true, false)

  @Test fun disabledPasskeyPreserved() = passkey(false, true)

  private fun auth(minter: Boolean?) =
    verify(
      "auth_config",
      {
        buildJsonObject {
          put("single_session_mode", false)
          minter?.let { put("session_minter", it) }
        }
      },
    ) { environment ->
      assertFalse(environment.authConfig.singleSessionMode)
      assertEquals(minter ?: false, environment.authConfig.sessionMinter)
    }

  private fun display(warning: Boolean?, email: String? = null) =
    verify(
      "display_config",
      { original ->
        JsonObject(
          original.toMutableMap().apply {
            if (warning == null) remove("show_devmode_warning")
            else put("show_devmode_warning", JsonPrimitive(warning))
            if (email == null) remove("support_email")
            else put("support_email", JsonPrimitive(email))
          }
        )
      },
    ) { environment ->
      assertEquals(warning ?: false, environment.displayConfig.showDevModeWarning)
      assertEquals(email ?: "", environment.displayConfig.supportEmail)
    }

  private fun organization(settings: JsonObject, enabled: Boolean = false) =
    verify("organization_settings", { settings }) { environment ->
      val actual = environment.organizationSettings
      assertEquals(enabled, actual.enabled)
      assertEquals(if (enabled) 5.0 else 1.0, actual.maxAllowedMemberships, 0.0)
      assertEquals(enabled, actual.forceOrganizationSelection)
      assertEquals(enabled, actual.actions.adminDelete)
      assertEquals(enabled, actual.domains.enabled)
      assertEquals(
        if (enabled) listOf("automatic_invitation") else emptyList<String>(),
        actual.domains.enrollmentModes.map { it.rawValue },
      )
      assertEquals(if (enabled) "org:member" else null, actual.domains.defaultRole)
      assertEquals(enabled, actual.slug.disabled)
      assertEquals(enabled, actual.organizationCreationDefaults.enabled)
    }

  private fun immutable(value: Boolean?) =
    verify(
      "user_settings",
      { settings ->
        val attribute = buildJsonObject {
          put("enabled", true)
          put("required", true)
          put("used_for_first_factor", true)
          put("first_factors", JsonArray(listOf(JsonPrimitive("email_code"))))
          put("used_for_second_factor", false)
          put("second_factors", JsonArray(emptyList()))
          put("verifications", JsonArray(listOf(JsonPrimitive("email_code"))))
          put("verify_at_sign_up", true)
          value?.let { put("immutable", it) }
        }
        JsonObject(
          settings +
            ("attributes" to
              JsonObject(
                settings.getValue("attributes").jsonObject + ("email_address" to attribute)
              ))
        )
      },
    ) { environment ->
      val actual = environment.userSettings.attributes.getValue("email_address")
      assertEquals(value, actual.immutable)
      assertTrue(actual.enabled)
      assertTrue(actual.required)
      assertTrue(actual.usedForFirstFactor)
      assertFalse(actual.usedForSecondFactor)
      assertTrue(actual.verifyAtSignUp)
      assertEquals(listOf("email_code"), actual.firstFactors.map { it.rawValue })
      assertEquals(listOf("email_code"), actual.verifications.map { it.rawValue })
      assertTrue(actual.secondFactors.isEmpty())
      assertEquals("email_address", actual.name.rawValue)
    }

  private fun passkey(enabled: Boolean, firstFactor: Boolean) =
    verify(
      "user_settings",
      { settings ->
        val attributes = settings.getValue("attributes").jsonObject
        val passkey =
          JsonObject(
            attributes.getValue("passkey").jsonObject +
              mapOf(
                "enabled" to JsonPrimitive(enabled),
                "used_for_first_factor" to JsonPrimitive(firstFactor),
              )
          )
        JsonObject(settings + ("attributes" to JsonObject(attributes + ("passkey" to passkey))))
      },
    ) { environment ->
      val actual = environment.userSettings.attributes.getValue("passkey")
      assertEquals(enabled, actual.enabled)
      assertEquals(firstFactor, actual.usedForFirstFactor)
    }

  private fun verify(
    section: String,
    change: (JsonObject) -> JsonObject,
    checkEnvironment: (EnvironmentResource) -> Unit,
  ) = runBlocking {
    withTimeout(30_000) {
      withContext(Dispatchers.Main.immediate) {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val base = PackagedFixtures(instrumentation.context)
        val original = base.fixtures.getValue("environment").jsonObject
        val environment =
          JsonObject(original + (section to change(original.getValue(section).jsonObject)))
        val host =
          object : NativeCapabilities {
            override val supported = base.supported

            override suspend fun perform(capability: String, arguments: JsonElement): JsonElement {
              if (
                capability == "http" &&
                  URI(arguments.jsonObject.getValue("url").requireString())
                    .path
                    .endsWith("/environment")
              ) {
                assertEquals(JsonPrimitive("GET"), arguments.jsonObject["method"])
                return buildJsonObject {
                  put("status", 200)
                  put("headers", buildJsonObject {})
                  put("body", buildJsonObject { put("response", environment) }.toString())
                }
              }
              return base.perform(capability, arguments)
            }
          }
        val clerk =
          Clerk.connect(
            instrumentation.targetContext,
            ClerkConfiguration(
              "pk_test_" +
                Base64.getEncoder().encodeToString("native-core.clerk.accounts.dev$".toByteArray()),
              "clerk-test://sso-callback",
            ),
            host,
          )
        try {
          assertTrue(clerk.loaded)
          checkEnvironment(clerk.environment)
          val resource = clerk.environment
          resource.reload()
          assertSame(resource, clerk.environment)
          checkEnvironment(resource)
        } finally {
          clerk.close()
        }
      }
    }
  }
}
