package com.clerk.api

import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.net.URI
import java.util.Base64
import kotlinx.coroutines.*
import kotlinx.serialization.json.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TokenFreshnessContractTest {
  @Test fun freshResponseBeforeStaleKeepsCanonicalToken() = verify(true, false)

  @Test fun staleResponseBeforeFreshKeepsCanonicalToken() = verify(false, false)

  @Test fun equalOriginUsesIssuedAtDespiteResponseOrder() = verify(true, true)

  private fun verify(freshFirst: Boolean, equalOrigin: Boolean) = runBlocking {
    withContext(Dispatchers.Main.immediate) {
      val instrumentation = InstrumentationRegistry.getInstrumentation()
      val base = PackagedFixtures(instrumentation.context)
      base.clientResponse = base.fixtures.getValue("authenticatedClient")
      val environment = base.fixtures.getValue("environment").jsonObject.toMutableMap()
      val auth = environment.getValue("auth_config").jsonObject.toMutableMap()
      auth["session_minter"] = JsonPrimitive(true)
      environment["auth_config"] = JsonObject(auth)
      base.environmentResponse = JsonObject(environment)
      val now = System.currentTimeMillis() / 1000
      fun token(origin: Int, issued: Long, signature: String): String {
        fun encode(value: String) =
          Base64.getUrlEncoder().withoutPadding().encodeToString(value.toByteArray())
        return encode("""{"alg":"none","typ":"JWT","oiat":$origin}""") +
          "." +
          encode("""{"sid":"sess_native","iat":$issued,"exp":${now + 3600}}""") +
          ".$signature"
      }
      val stale = token(100, now - 10, "stale")
      val fresh = token(if (equalOrigin) 100 else 200, now - if (equalOrigin) 5 else 20, "fresh")
      val started = List(2) { CompletableDeferred<Unit>() }
      val release = List(2) { CompletableDeferred<Unit>() }
      val previousTokens = mutableListOf<String?>()
      val host =
        object : NativeCapabilities {
          override val supported = base.supported

          override suspend fun perform(capability: String, arguments: JsonElement): JsonElement {
            val args = arguments.jsonObject
            if (
              capability != "http" ||
                !URI(args.getValue("url").requireString())
                  .path
                  .endsWith("/sessions/sess_native/tokens")
            ) {
              return base.perform(capability, arguments)
            }
            val index = previousTokens.size
            check(index < 3) { "Cached reads must not mint another token" }
            val form =
              Uri.parse("https://fixture.invalid/?" + args.getValue("body").requireString())
            check(form.getQueryParameter("force_origin") == "true")
            previousTokens += form.getQueryParameter("token")
            if (index < 2) {
              started[index].complete(Unit)
              release[index].await()
            }
            return buildJsonObject {
              put("status", 200)
              put("headers", buildJsonObject {})
              put(
                "body",
                buildJsonObject {
                    put("object", "token")
                    put("jwt", if (index == 0) stale else fresh)
                  }
                  .toString(),
              )
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
        val session = checkNotNull(clerk.session)
        val first = async { session.getToken(GetTokenOptions(skipCache = true)) }
        withTimeout(3000) { started[0].await() }
        val second = async { session.getToken(GetTokenOptions(skipCache = true)) }
        withTimeout(3000) { started[1].await() }
        val calls = listOf(first, second)
        val tokens = listOf(stale, fresh)
        for (index in if (freshFirst) listOf(1, 0) else listOf(0, 1)) {
          release[index].complete(Unit)
          check(withTimeout(3000) { calls[index].await() } == tokens[index])
        }
        check(session.getToken() == fresh)
        check(previousTokens.size == 2)
        check(session.getToken(GetTokenOptions(skipCache = true)) == fresh)
        check(previousTokens.last() == fresh)
        check(session.getToken() == fresh)
        check(previousTokens.size == 3)
        check(clerk.session?.handle == session.handle)
      } finally {
        release.forEach { it.complete(Unit) }
        clerk.close()
      }
    }
  }
}
