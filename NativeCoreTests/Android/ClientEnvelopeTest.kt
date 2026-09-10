package com.clerk.api

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.net.URI
import java.util.Base64
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ClientEnvelopeTest {
  @Test fun nullPiggybackDoesNotOverrideTheCanonicalClientResponse() = verify(true)

  @Test fun directClientResponsePublishesClearedSelection() = verify(false)

  private fun verify(wrapped: Boolean) = runBlocking {
    withContext(Dispatchers.Main.immediate) {
      val instrumentation = InstrumentationRegistry.getInstrumentation()
      val base = PackagedFixtures(instrumentation.context)
      val authenticated = base.fixtures.getValue("authenticatedClient").jsonObject
      base.clientResponse = authenticated
      val emptyClient =
        JsonObject(
          authenticated +
            mapOf(
              "sessions" to JsonArray(emptyList()),
              "last_active_session_id" to JsonNull,
            )
        )
      var refresh = false
      var clientRequests = 0
      val host =
        object : NativeCapabilities {
          override val supported = base.supported

          override suspend fun perform(capability: String, arguments: JsonElement): JsonElement {
            val args = arguments.jsonObject
            if (
              capability != "http" ||
                !refresh ||
                !URI(args.getValue("url").requireString()).path.endsWith("/client")
            )
              return base.perform(capability, arguments)
            clientRequests++
            assertEquals("GET", args.getValue("method").requireString())
            val headers = args.getValue("headers").jsonObject
            assertEquals(
              base.credential,
              headers.entries.first { it.key.equals("authorization", true) }.value.requireString(),
            )
            val payload =
              if (wrapped)
                buildJsonObject {
                  put("response", emptyClient)
                  put("client", JsonNull)
                }
              else emptyClient
            return buildJsonObject {
              put("status", 200)
              put("headers", buildJsonObject { put("date", "Thu, 10 Sep 2026 15:00:00 GMT") })
              put("body", payload.toString())
            }
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
        val selected = clerk.session!!
        val credential = base.credential
        val cleared =
          async(start = CoroutineStart.UNDISPATCHED) {
            withTimeout(5_000) {
              clerk.changes.first { it.session == null && it.user == null && it.sessions.isEmpty() }
            }
          }
        refresh = true
        clerk.context.requireRuntime().setApplicationActive(false)
        clerk.context.requireRuntime().setApplicationActive(true)
        val state = cleared.await()
        assertEquals(authenticated.getValue("id").requireString(), state.clientId)
        assertEquals(state.clientId, clerk.clientId)
        assertNull(clerk.session)
        assertNull(clerk.user)
        assertTrue(clerk.sessions.isEmpty())
        assertTrue(selected.isInvalidated)
        assertEquals(credential, base.credential)
        assertEquals(1, clientRequests)
      } finally {
        clerk.close()
      }
    }
  }
}
