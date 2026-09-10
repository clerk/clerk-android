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
class EmptyClientResponseTest {
  @Test fun emptyRefreshPreservesIdentity() = verify(false)

  @Test fun emptyRefreshPreservesANewerSessionTask() = verify(true)

  private fun verify(newerSessionResponse: Boolean) = runBlocking {
    withContext(Dispatchers.Main.immediate) {
      val instrumentation = InstrumentationRegistry.getInstrumentation()
      val base = PackagedFixtures(instrumentation.context)
      val client = base.fixtures.getValue("authenticatedClient").jsonObject.toMutableMap()
      base.clientResponse = JsonObject(client.toMap())
      val sessionJson = client.getValue("sessions").jsonArray[0].jsonObject.toMutableMap()
      sessionJson["status"] = JsonPrimitive("pending")
      sessionJson["tasks"] = buildJsonArray {
        add(buildJsonObject { put("key", "choose-organization") })
      }
      client["sessions"] = buildJsonArray { add(JsonObject(sessionJson)) }
      base.sessionReloadResponse = buildJsonObject {
        put("response", JsonObject(sessionJson))
        put("client", JsonObject(client))
      }
      var holdClient = false
      val started = CompletableDeferred<Unit>()
      val release = CompletableDeferred<Unit>()
      val host =
        object : NativeCapabilities {
          override val supported = base.supported

          override suspend fun perform(capability: String, arguments: JsonElement): JsonElement {
            val args = arguments.jsonObject
            if (
              capability == "http" &&
                holdClient &&
                URI(args.getValue("url").requireString()).path.endsWith("/client")
            ) {
              check(args.getValue("method").requireString() == "GET")
              started.complete(Unit)
              release.await()
              return buildJsonObject {
                put("status", 200)
                put("headers", buildJsonObject {})
                put("body", """{"response":null}""")
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
        val runtime = clerk.context.requireRuntime()
        val session = checkNotNull(clerk.session)
        val credential = base.credential
        val clientId = clerk.clientId
        holdClient = true
        runtime.setApplicationActive(false)
        runtime.setApplicationActive(true)
        withTimeout(3000) { started.await() }
        if (newerSessionResponse) session.reload()
        delay(20)
        val revision = runtime.revision
        release.complete(Unit)
        withTimeout(3000) {
          while (runtime.revision <= revision && runtime.lastLifecycleError.value == null) delay(5)
        }
        check(runtime.lastLifecycleError.value == null)
        check(clerk.clientId == clientId) { "Empty refresh lost the client" }
        check(clerk.session === session)
        check(clerk.session?.status?.rawValue == if (newerSessionResponse) "pending" else "active")
        if (newerSessionResponse)
          check(clerk.session?.currentTask?.key?.rawValue == "choose-organization")
        check(base.credential == credential)
      } finally {
        release.complete(Unit)
        clerk.close()
      }
    }
  }
}
