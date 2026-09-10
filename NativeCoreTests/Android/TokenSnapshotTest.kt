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
class TokenSnapshotTest {
  @Test fun initialSnapshotAvoidsMinting() = verify("initial")

  @Test fun clearingCacheRequiresANewMint() = verify("clear")

  @Test fun mintedTimestampTieRemainsCached() = verify("mintTie")

  @Test fun staleSnapshotDoesNotReplaceMintedToken() = verify("stale")

  @Test fun freshSnapshotReplacesMintedToken() = verify("fresh")

  @Test fun timestampTieAcceptsIncomingSnapshot() = verify("tie")

  @Test fun expiredSnapshotPreservesFresherMintInput() = verify("expired")

  private fun verify(scenario: String) = runBlocking {
    withContext(Dispatchers.Main.immediate) {
      val instrumentation = InstrumentationRegistry.getInstrumentation()
      val base = PackagedFixtures(instrumentation.context)
      val now = System.currentTimeMillis() / 1000
      fun token(origin: Int, name: String, expired: Boolean = false): String {
        fun encode(value: String) =
          Base64.getUrlEncoder().withoutPadding().encodeToString(value.toByteArray())
        return encode("""{"alg":"none","typ":"JWT","oiat":$origin}""") +
          "." +
          encode(
            """{"sid":"sess_native","iat":${now - 20},"exp":${now + if (expired) -10 else 3600}}"""
          ) +
          ".$name"
      }
      val initial = token(100, "initial")
      val minted = token(if (scenario == "mintTie") 100 else 200, "minted", scenario == "expired")
      val snapshot =
        token(
          if (scenario == "fresh") 300 else if (scenario == "tie") 200 else 50,
          "snapshot",
          scenario == "expired",
        )
      val final = token(400, "final")
      fun tokenJson(jwt: String) = buildJsonObject {
        put("object", "token")
        put("jwt", jwt)
      }
      val client = base.fixtures.getValue("authenticatedClient").jsonObject.toMutableMap()
      val sessionJson = client.getValue("sessions").jsonArray[0].jsonObject.toMutableMap()
      sessionJson["last_active_token"] = tokenJson(initial)
      client["sessions"] = buildJsonArray { add(JsonObject(sessionJson.toMap())) }
      base.clientResponse = JsonObject(client.toMap())
      sessionJson["last_active_token"] = tokenJson(snapshot)
      client["sessions"] = buildJsonArray { add(JsonObject(sessionJson.toMap())) }
      base.sessionReloadResponse = buildJsonObject {
        put("response", JsonObject(sessionJson))
        put("client", JsonObject(client))
      }
      val environment = base.fixtures.getValue("environment").jsonObject.toMutableMap()
      val auth = environment.getValue("auth_config").jsonObject.toMutableMap()
      auth["session_minter"] = JsonPrimitive(true)
      environment["auth_config"] = JsonObject(auth)
      base.environmentResponse = JsonObject(environment)
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
            previousTokens +=
              Uri.parse("https://fixture.invalid/?" + args.getValue("body").requireString())
                .getQueryParameter("token")
            check(previousTokens.size <= 2) { "Unexpected token fetch" }
            return buildJsonObject {
              put("status", 200)
              put("headers", buildJsonObject {})
              put("body", tokenJson(if (previousTokens.size == 1) minted else final).toString())
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
        check(session.getToken() == initial)
        check(previousTokens.isEmpty())
        when (scenario) {
          "initial" -> Unit
          "clear" -> {
            session.clearCache()
            check(session.getToken() == minted)
            check(previousTokens == listOf(initial))
          }
          else -> {
            check(session.getToken(GetTokenOptions(skipCache = true)) == minted)
            check(previousTokens == listOf(initial))
            if (scenario == "mintTie") {
              check(session.getToken() == minted)
              check(previousTokens == listOf(initial))
              return@withContext
            }
            session.reload()
            check(clerk.session?.handle == session.handle)
            if (scenario == "expired") {
              check(session.getToken() == final)
              check(previousTokens == listOf(initial, minted))
            } else {
              check(session.getToken() == if (scenario == "stale") minted else snapshot)
              check(previousTokens == listOf(initial))
            }
          }
        }
      } finally {
        clerk.close()
      }
    }
  }
}
