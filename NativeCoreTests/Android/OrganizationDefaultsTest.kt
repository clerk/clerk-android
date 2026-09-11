package com.clerk.api

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.net.URI
import java.util.Base64
import kotlinx.coroutines.*
import kotlinx.serialization.json.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OrganizationDefaultsTest {
  @Test fun absentFormUsesCanonicalEmptyDefaults() = verify("no-form")

  @Test fun absentSeverityDefaultsToWarning() = verify("no-severity")

  @Test fun absentSlugUsesCanonicalEmptyString() = verify("no-slug")

  @Test fun partialBrandingPreservesLogoAndAdvisory() = verify("partial-branding")

  private fun verify(scenario: String) = runBlocking {
    withContext(Dispatchers.Main.immediate) {
      val instrumentation = InstrumentationRegistry.getInstrumentation()
      val base = PackagedFixtures(instrumentation.context)
      base.clientResponse = base.fixtures.getValue("authenticatedClient")
      val payload =
        when (scenario) {
          "partial-branding" -> """{"advisory":{"code":"organization_already_exists","meta":{"organization_domain":"acme.test","organization_name":"Acme"}},"form":{"name":"Acme","logo":"https://img.clerk.com/acme.png"}}"""
          "no-form" -> """{"advisory":null,"form":null}"""
          "no-severity" ->
            """{"advisory":{"code":"organization_already_exists","meta":{"organization_domain":"clerk.dev","organization_name":"Clerk"}},"form":{"name":"My Organization","slug":"my-organization","logo":null,"blur_hash":null}}"""
          else ->
            """{"advisory":null,"form":{"name":"My Organization","logo":null,"blur_hash":null}}"""
        }
      var reads = 0
      val host =
        object : NativeCapabilities {
          override val supported = base.supported

          override suspend fun perform(capability: String, arguments: JsonElement): JsonElement {
            val args = arguments.jsonObject
            if (
              capability == "http" &&
                URI(args.getValue("url").requireString())
                  .path
                  .endsWith("/organization_creation_defaults")
            ) {
              reads++
              check(args.getValue("method").requireString() == "GET")
              return buildJsonObject {
                put("status", 200)
                put("headers", buildJsonObject {})
                put("body", "{\"response\":$payload}")
              }
            }
            return base.perform(capability, arguments)
          }
        }
      val key =
        "pk_test_" +
          Base64.getEncoder().encodeToString("native-core.clerk.accounts.dev$".toByteArray())
      val clerk =
        Clerk.connect(
          instrumentation.targetContext,
          ClerkConfiguration(key, "clerk-test://sso-callback"),
          host,
        )
      try {
        val defaults = checkNotNull(clerk.user).getOrganizationCreationDefaults()
        check(reads == 1)
        check(defaults.form.name == when (scenario) { "no-form" -> ""; "partial-branding" -> "Acme"; else -> "My Organization" })
        check(defaults.form.slug == if (scenario == "no-severity") "my-organization" else "")
        check(defaults.form.logo == if (scenario == "partial-branding") "https://img.clerk.com/acme.png" else null)
        check(defaults.form.blurHash == null)
        if (scenario == "no-severity" || scenario == "partial-branding") {
          check(defaults.advisory?.code == "organization_already_exists")
          check(defaults.advisory?.severity == "warning")
          check(defaults.advisory?.meta?.get("organization_domain") == if (scenario == "partial-branding") "acme.test" else "clerk.dev")
          check(defaults.advisory?.meta?.get("organization_name") == if (scenario == "partial-branding") "Acme" else "Clerk")
        } else check(defaults.advisory == null)
      } finally {
        clerk.close()
      }
    }
  }
}
