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
class TokenCancellationTest {
  @Test fun cancellingFirstCallerPreservesSecond() = verifyCancellation(setOf(0))
  @Test fun cancellingSecondCallerPreservesFirst() = verifyCancellation(setOf(1))
  @Test fun cancellingBothCallersAllowsLaterCachedRead() = verifyCancellation(setOf(0, 1))
  @Test fun sharedFailureRecoversAfterFirstCallerCancels() = verifyCancellation(setOf(0), true)
  @Test fun sharedFailureRecoversAfterSecondCallerCancels() = verifyCancellation(setOf(1), true)

  private fun verifyCancellation(cancelled: Set<Int>, failRequest: Boolean = false) = runBlocking {
    withContext(Dispatchers.Main.immediate) {
      val instrumentation = InstrumentationRegistry.getInstrumentation()
      val base = PackagedFixtures(instrumentation.context)
      base.clientResponse = base.fixtures.getValue("authenticatedClient")
      val now = System.currentTimeMillis() / 1000
      fun encoded(value: String) = Base64.getUrlEncoder().withoutPadding().encodeToString(value.toByteArray())
      val token = encoded("""{"alg":"none","typ":"JWT"}""") + "." +
        encoded("""{"sid":"sess_native","iat":$now,"exp":${now + 3600}}""") + ".fixture"
      val started = CompletableDeferred<Unit>()
      val release = CompletableDeferred<Unit>()
      var requests = 0
      val host = object : NativeCapabilities {
        override val supported = base.supported
        override suspend fun perform(capability: String, arguments: JsonElement): JsonElement {
          if (capability != "http" || !URI(arguments.jsonObject.getValue("url").requireString()).path.endsWith("/sessions/sess_native/tokens/firebase")) {
            return base.perform(capability, arguments)
          }
          val first = ++requests == 1
          if (first) { started.complete(Unit); release.await() }
          val failed = first && failRequest
          return buildJsonObject {
            put("status", if (failed) 403 else 200); put("headers", buildJsonObject {})
            put("body", (if (failed) buildJsonObject {
              put("errors", buildJsonArray { add(buildJsonObject { put("code", "token_denied"); put("message", "Token denied") }) })
            } else buildJsonObject { put("object", "token"); put("jwt", token) }).toString())
          }
        }
      }
      val key = "pk_test_" + Base64.getEncoder().encodeToString("native-core.clerk.accounts.dev$".toByteArray())
      val clerk = Clerk.connect(instrumentation.targetContext, ClerkConfiguration(key, "clerk-test://sso-callback"), host)
      try {
        val session = checkNotNull(clerk.session)
        val first = async { runCatching { session.getToken(GetTokenOptions(template = "firebase")) } }
        withTimeout(3000) { started.await() }
        val second = async { runCatching { session.getToken(GetTokenOptions(template = "firebase")) } }
        delay(20)
        check(requests == 1)
        val calls = listOf(first, second)
        for (index in cancelled) {
          calls[index].cancelAndJoin()
          check(runCatching { calls[index].await() }.exceptionOrNull() is CancellationException)
        }
        release.complete(Unit)
        for (index in (0..1).filter { it !in cancelled }) {
          val result = calls[index].await()
          if (failRequest) {
            val error = result.exceptionOrNull() as? CoreException ?: error("Expected the shared token error")
            check(error.status == 403 && error.errors.first().code == "token_denied")
          } else check(result.getOrThrow() == token)
        }
        delay(20)
        check(session.getToken(GetTokenOptions(template = "firebase")) == token)
        check(requests == (if (failRequest) 2 else 1) && clerk.session?.id == session.id)
      } finally {
        release.complete(Unit)
        clerk.close()
      }
    }
  }
}
