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
class PasskeyRecoveryTest {
  @Test fun emailSignInFollowsCancelledPasskey() = recover("user_cancelled")

  @Test fun emailSignInFollowsProviderFailure() = recover("host_failure")

  @Test fun emailSignInFollowsMissingPresentation() = recover("presentation_unavailable")

  @Test fun emailSignInFollowsInvalidCredential() = recover("invalid_credential_response")

  private fun recover(code: String) = runBlocking {
    withTimeout(15000) {
      withContext(Dispatchers.Main.immediate) {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val base = PackagedFixtures(instrumentation.context)
        base.clientResponse = base.fixtures.getValue("client")
        var presentations = 0
        val paths = mutableListOf<String>()
        val host =
          object : NativeCapabilities {
            override val supported = base.supported

            override suspend fun perform(capability: String, arguments: JsonElement): JsonElement {
              if (capability == "passkeys.get") {
                presentations++
                check(
                  arguments.jsonObject["preferImmediatelyAvailableCredentials"] ==
                    JsonPrimitive(true)
                )
                if (code == "host_failure") throw IllegalStateException("private provider detail")
                throw CoreException(code)
              }
              if (capability != "http") return base.perform(capability, arguments)
              val args = arguments.jsonObject
              val path = checkNotNull(Uri.parse(args.getValue("url").requireString()).path)
              if (!path.contains("/sign_ins")) return base.perform(capability, arguments)
              paths += path
              check(args["method"] == JsonPrimitive("POST"))
              val body = Uri.parse("https://fixture.test/?" + args.getValue("body").requireString())
              val passkey = body.getQueryParameter("strategy") == "passkey"
              if (passkey) check(path == "/v1/client/sign_ins")
              else if (path.endsWith("/prepare_first_factor")) {
                check(path == "/v1/client/sign_ins/si_after_passkey/prepare_first_factor")
                check(body.getQueryParameter("strategy") == "email_code")
                check(body.getQueryParameter("email_address_id") == "idn_email")
                check(body.getQueryParameter("public_key_credential") == null)
              } else {
                check(path == "/v1/client/sign_ins")
                check(body.getQueryParameter("identifier") == "person@example.com")
                check(body.getQueryParameter("strategy") == null)
              }
              val signIn =
                JsonObject(
                  base.fixtures.getValue("signIn").jsonObject +
                    mapOf(
                      "id" to
                        JsonPrimitive(if (passkey) "si_passkey_prompt" else "si_after_passkey"),
                      "identifier" to
                        if (passkey) JsonNull else JsonPrimitive("person@example.com"),
                      "status" to JsonPrimitive("needs_first_factor"),
                      "supported_first_factors" to
                        if (passkey) Json.parseToJsonElement("""[{"strategy":"passkey"}]""")
                        else
                          Json.parseToJsonElement(
                            """[{"strategy":"email_code","email_address_id":"idn_email","safe_identifier":"person@example.com"}]"""
                          ),
                      "first_factor_verification" to
                        JsonObject(
                          base.fixtures
                            .getValue("signIn")
                            .jsonObject
                            .getValue("first_factor_verification")
                            .jsonObject +
                            mapOf(
                              "strategy" to JsonPrimitive(if (passkey) "passkey" else "email_code"),
                              "nonce" to
                                if (passkey)
                                  JsonPrimitive(
                                    """{"challenge":"Y2hhbGxlbmdl","rpId":"native-core.clerk.accounts.dev","userVerification":"required"}"""
                                  )
                                else JsonNull,
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
          val failure =
            runCatching {
                clerk.signIn.passkey(
                  SignInPasskeyParams(
                    flow = SignInPasskeyParamsFlow.Discoverable,
                    preferImmediatelyAvailableCredentials = true,
                  )
                )
              }
              .exceptionOrNull()
          check(
            failure is CoreException &&
              failure.code == code &&
              failure.passkeyStage == "requestingAuthorization"
          )
          check(!failure.message.contains("private provider detail"))
          check(clerk.signIn.id == "si_passkey_prompt" && clerk.session == null)
          val result =
            clerk.startAuthentication(
              MobileIdentifierParams(
                mode = MobileIdentifierParamsMode.SignIn,
                identifier = "person@example.com",
                identifierType = MobileIdentifierParamsIdentifierType.EmailAddress,
              )
            )
          check(result is MobileAuthenticationResult.Case1)
          val signIn = result.value.signIn
          check(signIn === clerk.signIn && signIn.id == "si_after_passkey")
          signIn.emailCode.sendCode(
            SignInEmailCodeSendParams.Case2(
              SignInEmailCodeSendCodeParamsCase2(emailAddressId = "idn_email")
            )
          )
          check(
            paths ==
              listOf(
                "/v1/client/sign_ins",
                "/v1/client/sign_ins",
                "/v1/client/sign_ins/si_after_passkey/prepare_first_factor",
              )
          )
          check(presentations == 1 && clerk.session == null && clerk.user == null)
          check(signIn.status.rawValue == "needs_first_factor")
        } finally {
          clerk.close()
        }
      }
    }
  }
}
