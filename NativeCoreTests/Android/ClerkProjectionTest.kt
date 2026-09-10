package com.clerk.api

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.util.Base64
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ClerkProjectionTest {
  private fun fixtures() = PackagedFixtures(InstrumentationRegistry.getInstrumentation().context)

  private suspend fun connect(fixtures: PackagedFixtures): Clerk {
    val key =
      "pk_test_" +
        Base64.getEncoder().encodeToString("native-core.clerk.accounts.dev$".toByteArray())
    return Clerk.connect(
      InstrumentationRegistry.getInstrumentation().targetContext,
      ClerkConfiguration(key, "clerk-test://sso-callback"),
      fixtures,
    )
  }

  @Test
  fun initialSessionSelectionAndPendingUsersFollowTheCore() = runBlocking {
    withContext(Dispatchers.Main.immediate) {
      data class Selection(val statuses: List<String>, val selected: String?, val expected: String?)
      for (case in
        listOf(
          Selection(emptyList(), null, null),
          Selection(listOf("active", "active"), null, "session_0"),
          Selection(listOf("active"), "missing", "session_0"),
          Selection(listOf("active", "active"), "session_1", "session_1"),
          Selection(listOf("pending"), "session_0", "session_0"),
          Selection(listOf("expired"), "session_0", null),
        )) {
        val base = fixtures()
        val sessions =
          case.statuses.mapIndexed { index, status ->
            JsonObject(
              base.fixtures.getValue("session").jsonObject +
                mapOf(
                  "id" to JsonPrimitive("session_$index"),
                  "status" to JsonPrimitive(status),
                  "tasks" to
                    if (status == "pending")
                      JsonArray(listOf(buildJsonObject { put("key", "choose-organization") }))
                    else JsonArray(emptyList()),
                )
            )
          }
        base.clientResponse =
          JsonObject(
            base.fixtures.getValue("client").jsonObject +
              mapOf(
                "sessions" to JsonArray(sessions),
                "last_active_session_id" to (case.selected?.let(::JsonPrimitive) ?: JsonNull),
              )
          )
        val clerk = connect(base)
        try {
          assertTrue(clerk.loaded)
          assertEquals(case.expected, clerk.session?.id)
          assertEquals(if (case.expected == null) null else "user_native", clerk.user?.id)
          assertEquals(sessions.indices.map { "session_$it" }, clerk.sessions.map { it.id })
          assertFalse(clerk.signIn.isInvalidated)
          assertFalse(clerk.signUp.isInvalidated)
          if (case.statuses.firstOrNull() == "pending") {
            assertEquals("pending", clerk.session?.status?.rawValue)
            assertEquals("choose-organization", clerk.session?.currentTask?.key?.rawValue)
          }
        } finally {
          clerk.close()
        }
      }
    }
  }

  @Test
  fun refreshRetainsSelectionAndSignOutPublishesACoherentState() = runBlocking {
    withContext(Dispatchers.Main.immediate) {
      for (selected in listOf<String?>(null, "missing")) {
        val base = fixtures()
        val client = base.fixtures.getValue("authenticatedClient")
        base.clientResponse = client
        val clerk = connect(base)
        try {
          val session = checkNotNull(clerk.session)
          val user = checkNotNull(clerk.user)
          base.sessionReloadResponse = buildJsonObject {
            put("response", base.fixtures.getValue("session"))
            put(
              "client",
              JsonObject(
                client.jsonObject +
                  ("last_active_session_id" to (selected?.let(::JsonPrimitive) ?: JsonNull))
              ),
            )
          }
          session.reload()
          assertSame(session, clerk.session)
          assertSame(user, clerk.user)
          val observed =
            async(start = CoroutineStart.UNDISPATCHED) {
              withTimeout(3000) { clerk.changes.first { it.session == null && it.user == null } }
            }
          clerk.signOut()
          assertNull(clerk.session)
          assertNull(clerk.user)
          val state = observed.await()
          assertNull(state.session)
          assertNull(state.user)
          assertTrue(session.isInvalidated)
        } finally {
          clerk.close()
        }
      }
    }
  }

  @Test
  fun environmentProjectionPreservesDisplaySocialAndIdentifierSettings() = runBlocking {
    withContext(Dispatchers.Main.immediate) {
      val base = fixtures()
      fun environment(enabled: Boolean): JsonObject {
        val original = base.fixtures.getValue("environment").jsonObject
        val settings = original.getValue("user_settings").jsonObject
        val attributes = settings.getValue("attributes").jsonObject.toMutableMap()
        for (name in listOf("email_address", "phone_number", "username")) {
          attributes[name] =
            JsonObject(
              attributes.getValue(name).jsonObject +
                mapOf(
                  "enabled" to JsonPrimitive(enabled),
                  "immutable" to JsonPrimitive(!enabled),
                )
            )
        }
        val social = if (enabled) settings.getValue("social") else buildJsonObject {}
        return JsonObject(
          original +
            mapOf(
              "display_config" to
                JsonObject(
                  original.getValue("display_config").jsonObject +
                    mapOf(
                      "application_name" to JsonPrimitive(if (enabled) "Configured App" else ""),
                      "logo_image_url" to
                        JsonPrimitive(if (enabled) "https://example.com/logo.png" else ""),
                      "support_email" to JsonPrimitive(if (enabled) "help@example.com" else ""),
                    )
                ),
              "auth_config" to
                JsonObject(
                  original.getValue("auth_config").jsonObject +
                    ("single_session_mode" to JsonPrimitive(!enabled))
                ),
              "user_settings" to
                JsonObject(
                  settings + mapOf("attributes" to JsonObject(attributes), "social" to social)
                ),
            )
        )
      }
      base.environmentResponse = environment(true)
      val clerk = connect(base)
      try {
        val resource = clerk.environment
        for (enabled in listOf(true, false)) {
          base.environmentResponse = environment(enabled)
          resource.reload()
          assertSame(resource, clerk.environment)
          assertEquals(
            if (enabled) "Configured App" else "",
            resource.displayConfig.applicationName,
          )
          assertEquals(
            if (enabled) "https://example.com/logo.png" else "",
            resource.displayConfig.logoImageUrl,
          )
          assertEquals(if (enabled) "help@example.com" else "", resource.displayConfig.supportEmail)
          assertEquals(!enabled, resource.authConfig.singleSessionMode)
          assertEquals(enabled, resource.userSettings.social.containsKey("oauth_google"))
          for (name in listOf("email_address", "phone_number", "username")) {
            assertEquals(enabled, resource.userSettings.attributes.getValue(name).enabled)
            assertEquals(!enabled, resource.userSettings.attributes.getValue(name).immutable)
          }
        }
      } finally {
        clerk.close()
      }
    }
  }
}
