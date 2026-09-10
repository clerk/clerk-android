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
class SignInCredentialContractTest {
  @Test fun passkeyForwardsOptionsAndRequiresFinalization() = passkey(null)

  @Test fun cancelledPasskeyDoesNotSubmitCredential() = passkey("requestingAuthorization")

  @Test
  fun passkeyProviderFailureDoesNotSubmitCredential() = passkey("requestingAuthorization", true)

  @Test fun passkeyPreparationPreservesFailureStage() = passkey("preparingFirstFactor")

  @Test fun passkeySubmissionPreservesFailureStage() = passkey("attemptingFirstFactor")

  private fun passkey(failedStage: String?, unknownProviderFailure: Boolean = false) = runBlocking {
    withTimeout(15000) {
      withContext(Dispatchers.Main.immediate) {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val base = PackagedFixtures(instrumentation.context)
        val challenge =
          Json.parseToJsonElement(
            """{"challenge":"Y2hhbGxlbmdl","rpId":"native-core.clerk.accounts.dev","userVerification":"required","allowCredentials":[{"id":"Y3JlZGVudGlhbA","type":"public-key"}]}"""
          )
        val credential =
          Json.parseToJsonElement(
            """{"type":"public-key","id":"Y3JlZGVudGlhbA","rawId":"Y3JlZGVudGlhbA","authenticatorAttachment":"platform","response":{"clientDataJSON":"e30","authenticatorData":"YXV0aA","signature":"c2ln","userHandle":null}}"""
          )
        val attempt =
          JsonObject(
            base.fixtures.getValue("signIn").jsonObject +
              mapOf(
                "status" to JsonPrimitive("needs_first_factor"),
                "supported_first_factors" to
                  Json.parseToJsonElement("""[{"strategy":"passkey"}]"""),
                "first_factor_verification" to
                  JsonObject(
                    base.fixtures
                      .getValue("signIn")
                      .jsonObject
                      .getValue("first_factor_verification")
                      .jsonObject +
                      mapOf(
                        "strategy" to JsonPrimitive("passkey"),
                        "nonce" to JsonPrimitive(challenge.toString()),
                      )
                  ),
              )
          )
        val paths = mutableListOf<String>()
        var presentations = 0
        val host =
          object : NativeCapabilities {
            override val supported = base.supported

            override suspend fun perform(capability: String, arguments: JsonElement): JsonElement {
              val args = arguments.jsonObject
              if (capability == "passkeys.get") {
                presentations++
                check(args["conditionalUI"] == JsonPrimitive(false))
                check(args["preferImmediatelyAvailableCredentials"] == JsonPrimitive(true))
                check(args["rpId"] == JsonPrimitive("native-core.clerk.accounts.dev"))
                check(
                  args.getValue("challenge").jsonObject["base64url"] ==
                    JsonPrimitive("Y2hhbGxlbmdl")
                )
                check(
                  args
                    .getValue("allowCredentials")
                    .jsonArray
                    .single()
                    .jsonObject
                    .getValue("id")
                    .jsonObject["base64url"] == JsonPrimitive("Y3JlZGVudGlhbA")
                )
                if (unknownProviderFailure) throw IllegalStateException("private provider detail")
                if (failedStage == "requestingAuthorization") throw CoreException("user_cancelled")
                return credential
              }
              if (capability == "http") {
                val url = Uri.parse(args.getValue("url").requireString())
                if (url.path!!.contains("/sign_ins")) {
                  paths += url.path!!
                  check(args["method"] == JsonPrimitive("POST"))
                  val body =
                    Uri.parse("https://fixture.test/?" + args.getValue("body").requireString())
                  check(body.getQueryParameter("strategy") == "passkey")
                  val submitting = url.path!!.endsWith("/attempt_first_factor")
                  if (submitting)
                    check(
                      Json.parseToJsonElement(
                        checkNotNull(body.getQueryParameter("public_key_credential"))
                      ) == credential
                    )
                  val rejected =
                    failedStage == "preparingFirstFactor" ||
                      (submitting && failedStage == "attemptingFirstFactor")
                  val payload =
                    if (rejected)
                      Json.parseToJsonElement(
                        """{"clerk_trace_id":"passkey-signin-trace","errors":[{"code":"passkey_verification_failed","message":"Credential rejected","long_message":"Try another passkey."}]}"""
                      )
                    else
                      buildJsonObject {
                        put(
                          "response",
                          if (submitting)
                            JsonObject(
                              attempt +
                                mapOf(
                                  "status" to JsonPrimitive("complete"),
                                  "created_session_id" to JsonPrimitive("sess_native"),
                                )
                            )
                          else attempt,
                        )
                      }
                  return buildJsonObject {
                    put("status", if (rejected) 400 else 200)
                    put("headers", buildJsonObject {})
                    put("body", payload.toString())
                  }
                }
              }
              return base.perform(capability, arguments)
            }
          }
        val clerk = Clerk.connect(instrumentation.targetContext, configuration(), host)
        try {
          val result = runCatching {
            clerk.signIn.passkey(
              SignInPasskeyParams(
                flow = SignInPasskeyParamsFlow.Discoverable,
                preferImmediatelyAvailableCredentials = true,
              )
            )
          }
          if (failedStage == null) {
            result.getOrThrow()
            check(
              clerk.signIn.status.rawValue == "complete" &&
                clerk.signIn.createdSessionId == "sess_native"
            )
          } else {
            val error = result.exceptionOrNull()
            check(error is CoreException && error.passkeyStage == failedStage) {
              "Unexpected failure: $error"
            }
            if (failedStage == "requestingAuthorization") {
              check(error.code == if (unknownProviderFailure) "host_failure" else "user_cancelled")
              check(!error.message.contains("private provider detail"))
            } else {
              check(
                error.status == 400 && error.errors.single().code == "passkey_verification_failed"
              )
              check(error.clerkTraceId == "passkey-signin-trace")
              check(error.errors.single().longMessage == "Try another passkey.")
            }
          }
          check(clerk.session == null && clerk.user == null)
          check(presentations == if (failedStage == "preparingFirstFactor") 0 else 1)
          check(
            paths.count { it.endsWith("/attempt_first_factor") } ==
              if (failedStage == null || failedStage == "attemptingFirstFactor") 1 else 0
          )
        } finally {
          clerk.close()
        }
      }
    }
  }

  @Test
  fun biometricSignInForwardsSelectionAndPrompt() = runBlocking {
    withTimeout(15000) {
      withContext(Dispatchers.Main.immediate) {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val base = PackagedFixtures(instrumentation.context)
        base.clientResponse = base.fixtures.getValue("authenticatedClient")
        val prompts = mutableListOf<JsonObject>()
        val host =
          object : NativeCapabilities {
            override val supported = base.supported

            override suspend fun perform(capability: String, arguments: JsonElement): JsonElement {
              if (capability == "biometrics.sign") prompts += arguments.jsonObject
              return base.perform(capability, arguments)
            }
          }
        val clerk = Clerk.connect(instrumentation.targetContext, configuration(), host)
        try {
          val enrolled =
            clerk.biometricCredentials.enroll(
              BiometricCredentialEnrollmentParams(identifierHint = "user@example.com")
            )
          clerk.signOut()
          base.clientResponse = base.fixtures.getValue("client")
          val before = base.requests.size
          clerk.signIn.biometricCredential(
            SignInBiometricCredentialParams(
              id = enrolled.id,
              identifierHint = "user@example.com",
              reason = "Welcome back",
              promptSubtitle = "Use your screen lock",
            )
          )
          check(prompts.size == 2)
          check(prompts.last()["reason"] == JsonPrimitive("Welcome back"))
          check(prompts.last()["promptSubtitle"] == JsonPrimitive("Use your screen lock"))
          check(prompts.last()["localKeyId"] == JsonPrimitive("tdlk_native"))
          val requests =
            base.requests.drop(before).filter {
              Uri.parse(it.getValue("url").requireString()).path!!.contains("/sign_ins")
            }
          check(requests.isNotEmpty())
          val create =
            Uri.parse("https://fixture.test/?" + requests.first().getValue("body").requireString())
          check(create.getQueryParameter("trusted_device_id") == enrolled.id)
          // The hint selects local metadata; the server receives the credential ID.
          check(create.getQueryParameter("identifier") == null)
          check(
            clerk.signIn.status.rawValue == "complete" &&
              clerk.signIn.createdSessionId == "sess_native"
          )
          check(clerk.session == null && clerk.user == null)
        } finally {
          clerk.close()
        }
      }
    }
  }

  private fun configuration() =
    ClerkConfiguration(
      "pk_test_" +
        Base64.getEncoder().encodeToString("native-core.clerk.accounts.dev$".toByteArray()),
      "clerk-test://sso-callback",
    )
}
