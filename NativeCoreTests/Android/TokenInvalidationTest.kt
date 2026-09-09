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
class TokenInvalidationTest {
  @Test fun olderTokenReplyArrivesBeforeReplacement() = verifyInvalidation(true)
  @Test fun olderTokenReplyArrivesAfterReplacement() = verifyInvalidation(false)

  private fun verifyInvalidation(oldReplyFirst: Boolean) = runBlocking {
    withContext(Dispatchers.Main.immediate) {
      val instrumentation = InstrumentationRegistry.getInstrumentation()
      val base = PackagedFixtures(instrumentation.context)
      base.clientResponse = base.fixtures.getValue("authenticatedClient")
      val now = System.currentTimeMillis() / 1000
      fun encoded(value: String) = Base64.getUrlEncoder().withoutPadding().encodeToString(value.toByteArray())
      val tokens = listOf(200, 100).map { origin ->
        encoded("""{"alg":"none","typ":"JWT","oiat":$origin}""") + "." +
          encoded("""{"sid":"sess_native","iat":$now,"exp":${now + 3600}}""") + ".fixture"
      }
      val started = List(2) { CompletableDeferred<Unit>() }
      val releases = List(2) { CompletableDeferred<Unit>() }
      var requests = 0
      val host = object : NativeCapabilities {
        override val supported = base.supported
        override suspend fun perform(capability: String, arguments: JsonElement): JsonElement {
          if (capability != "http" || !URI(arguments.jsonObject.getValue("url").requireString()).path.endsWith("/sessions/sess_native/tokens/firebase")) {
            return base.perform(capability, arguments)
          }
          val index = requests++
          check(index < 2) { "the post-clear token should remain cached" }
          started[index].complete(Unit)
          releases[index].await()
          return buildJsonObject {
            put("status", 200); put("headers", buildJsonObject {})
            put("body", buildJsonObject { put("object", "token"); put("jwt", tokens[index]) }.toString())
          }
        }
      }
      val key = "pk_test_" + Base64.getEncoder().encodeToString("native-core.clerk.accounts.dev$".toByteArray())
      val clerk = Clerk.connect(instrumentation.targetContext, ClerkConfiguration(key, "clerk-test://sso-callback"), host)
      try {
        val session = checkNotNull(clerk.session)
        val first = async { session.getToken(GetTokenOptions(template = "firebase")) }
        withTimeout(3000) { started[0].await() }
        session.clearCache()
        val second = async { session.getToken(GetTokenOptions(template = "firebase")) }
        withTimeout(3000) { started[1].await() }
        if (oldReplyFirst) {
          releases[0].complete(Unit)
          check(first.await() == tokens[0])
          releases[1].complete(Unit)
          check(second.await() == tokens[1])
        } else {
          releases[1].complete(Unit)
          check(second.await() == tokens[1])
          releases[0].complete(Unit)
          check(first.await() == tokens[0])
        }
        check(session.getToken(GetTokenOptions(template = "firebase")) == tokens[1])
        check(requests == 2 && clerk.session?.id == session.id)
      } finally {
        releases.forEach { it.complete(Unit) }
        clerk.close()
      }
    }
  }
}
