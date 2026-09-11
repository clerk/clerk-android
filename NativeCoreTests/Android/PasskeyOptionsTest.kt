package com.clerk.api

import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.util.Base64
import kotlinx.coroutines.*
import kotlinx.serialization.json.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PasskeyOptionsTest {
  @Test fun serverOptionsPreserveRelyingPartyAndCredentials() = verify("valid")

  @Test fun missingRelyingPartyFailsBeforePresentation() = verify("missing")

  @Test fun nestedRelyingPartyFailsBeforePresentation() = verify("nested")

  @Test fun emptyRelyingPartyFailsBeforePresentation() = verify("empty")

  @Test fun whitespaceRelyingPartyFailsBeforePresentation() = verify("whitespace")

  @Test fun nonStringRelyingPartyFailsBeforePresentation() = verify("number")

  @Test fun missingCredentialIdRejectsServerList() = verify("missing-id")

  @Test fun invalidCredentialIdRejectsServerList() = verify("invalid-id")

  private fun verify(scenario: String) = runBlocking {
    withTimeout(15000) {
      withContext(Dispatchers.Main.immediate) {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val base = PackagedFixtures(instrumentation.context)
        base.clientResponse = base.fixtures.getValue("client")
        val options =
          Json.parseToJsonElement(
              """{"challenge":"Y2hhbGxlbmdl","rpId":"passkeys.example.com","timeout":45000,"userVerification":"preferred","allowCredentials":[{"type":"public-key","id":"Y3JlZGVudGlhbDE","transports":["internal","hybrid"]},{"type":"public-key","id":"Y3JlZGVudGlhbDI"}]}"""
            )
            .jsonObject
            .toMutableMap()
        when (scenario) {
          "missing",
          "nested" -> options.remove("rpId")
          "empty" -> options["rpId"] = JsonPrimitive("")
          "whitespace" -> options["rpId"] = JsonPrimitive("  ")
          "number" -> options["rpId"] = JsonPrimitive(123)
        }
        if (scenario == "nested")
          options["rp"] = buildJsonObject { put("id", "passkeys.example.com") }
        if (scenario.endsWith("-id"))
          options["allowCredentials"] =
            JsonArray(
              listOf(
                options.getValue("allowCredentials").jsonArray[0],
                buildJsonObject {
                  put("type", "public-key")
                  if (scenario == "invalid-id") put("id", "!!!")
                },
              )
            )
        var presentations = 0
        val paths = mutableListOf<String>()
        val host =
          object : NativeCapabilities {
            override val supported = base.supported

            override suspend fun perform(capability: String, arguments: JsonElement): JsonElement {
              val args = arguments.jsonObject
              if (capability == "passkeys.get") {
                presentations++
                check(args["rpId"] == JsonPrimitive("passkeys.example.com"))
                check(args["timeout"] == JsonPrimitive(45000))
                check(args["userVerification"] == JsonPrimitive("preferred"))
                check(args["challenge"] == buildJsonObject { put("base64url", "Y2hhbGxlbmdl") })
                check(
                  args["allowCredentials"] ==
                    Json.parseToJsonElement(
                      """[{"type":"public-key","id":{"base64url":"Y3JlZGVudGlhbDE"},"transports":["internal","hybrid"]},{"type":"public-key","id":{"base64url":"Y3JlZGVudGlhbDI"}}]"""
                    )
                )
                throw CoreException("user_cancelled")
              }
              if (capability != "http") return base.perform(capability, arguments)
              val path = Uri.parse(args.getValue("url").requireString()).path!!
              if (!path.contains("/sign_ins")) return base.perform(capability, arguments)
              paths += path
              check(path == "/v1/client/sign_ins" && args["method"] == JsonPrimitive("POST"))
              val original = base.fixtures.getValue("signIn").jsonObject
              val signIn =
                JsonObject(
                  original +
                    mapOf(
                      "status" to JsonPrimitive("needs_first_factor"),
                      "supported_first_factors" to
                        Json.parseToJsonElement("""[{"strategy":"passkey"}]"""),
                      "first_factor_verification" to
                        JsonObject(
                          original.getValue("first_factor_verification").jsonObject +
                            mapOf(
                              "strategy" to JsonPrimitive("passkey"),
                              "nonce" to JsonPrimitive(JsonObject(options).toString()),
                            )
                        ),
                    )
                )
              return buildJsonObject {
                put("status", 200)
                put("headers", buildJsonObject {})
                put("body", buildJsonObject { put("response", signIn) }.toString())
              }
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
          val signIn = clerk.signIn
          val error =
            runCatching {
                signIn.passkey(SignInPasskeyParams(flow = SignInPasskeyParamsFlow.Discoverable))
              }
              .exceptionOrNull()
          check(error is CoreException)
          check(
            error.passkeyStage ==
              if (scenario.endsWith("-id")) "preparingFirstFactor" else "requestingAuthorization"
          )
          if (!scenario.endsWith("-id"))
            check(
              error.code ==
                if (scenario == "valid") "user_cancelled" else "invalid_credential_options"
            )
          check(presentations == if (scenario == "valid") 1 else 0)
          check(paths == listOf("/v1/client/sign_ins"))
          check(clerk.signIn === signIn && signIn.status.rawValue == "needs_first_factor")
          check(clerk.session == null && clerk.user == null)
        } finally {
          clerk.close()
        }
      }
    }
  }
}
