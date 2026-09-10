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
class SessionPasskeyFailureTest {
  @Test
  fun preparationFailurePreservesAPIErrorWithoutPresentingCredential() =
    verify("preparation-rejected")

  @Test
  fun submissionFailurePreservesAPIErrorWithoutRetryingCredential() = verify("submission-rejected")

  @Test
  fun providerFailureDoesNotSubmitCredentialOrExposePrivateDetails() = verify("provider-failed")

  @Test fun invalidCredentialDoesNotSubmitVerification() = verify("invalid-credential")

  @Test fun missingOptionsFailBeforeCredentialPresentation() = verify("missing-options")

  @Test fun unsupportedPasskeysFailBeforeCredentialPresentation() = verify("unsupported")

  private fun verify(scenario: String) = runBlocking {
    withTimeout(15000) {
      withContext(Dispatchers.Main.immediate) {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val base = PackagedFixtures(instrumentation.context)
        base.clientResponse = base.fixtures.getValue("authenticatedClient")
        val credential =
          Json.parseToJsonElement(
            """{"id":"Y3JlZGVudGlhbA","rawId":"Y3JlZGVudGlhbA","type":"public-key","authenticatorAttachment":"platform","response":{"clientDataJSON":"e30","authenticatorData":"YXV0aA","signature":"c2ln","userHandle":null}}"""
          )
        val verification = buildJsonObject {
          put("object", "session_verification")
          put("id", "sv_passkey_failure")
          put("status", "needs_first_factor")
          put("level", "first_factor")
          put("session", base.clientResponse!!.jsonObject.getValue("sessions").jsonArray.single())
          put(
            "first_factor_verification",
            buildJsonObject {
              put("status", "unverified")
              put("strategy", "passkey")
              put(
                "nonce",
                if (scenario == "missing-options") JsonNull
                else
                  JsonPrimitive(
                    """{"challenge":"Y2hhbGxlbmdl","rpId":"native-core.clerk.accounts.dev","userVerification":"required"}"""
                  ),
              )
              put("attempts", 0)
              put("expire_at", JsonNull)
              put("error", JsonNull)
              put("verified_at_client", JsonNull)
            },
          )
          put("second_factor_verification", JsonNull)
          put(
            "supported_first_factors",
            buildJsonArray { add(buildJsonObject { put("strategy", "passkey") }) },
          )
          put("supported_second_factors", JsonNull)
        }
        var presentations = 0
        val paths = mutableListOf<String>()
        val host =
          object : NativeCapabilities {
            override val supported =
              if (scenario == "unsupported") base.supported - "passkeys" else base.supported

            override suspend fun perform(capability: String, arguments: JsonElement): JsonElement {
              if (capability == "passkeys.get") {
                presentations++
                if (scenario == "provider-failed")
                  throw IllegalStateException("private provider detail")
                if (scenario == "invalid-credential")
                  return JsonObject(credential.jsonObject + ("response" to JsonNull))
                return credential
              }
              if (capability != "http") return base.perform(capability, arguments)
              val args = arguments.jsonObject
              val url = Uri.parse(args.getValue("url").requireString())
              val path = checkNotNull(url.path)
              if (!path.contains("/verify/")) return base.perform(capability, arguments)
              paths += path
              check(args.getValue("method").requireString() == "POST")
              val attempting = path.endsWith("/attempt_first_factor")
              check(
                path ==
                  if (attempting) "/v1/client/sessions/sess_native/verify/attempt_first_factor"
                  else "/v1/client/sessions/sess_native/verify/prepare_first_factor"
              )
              if (attempting) {
                val body =
                  Uri.parse("https://fixture.test/?" + args.getValue("body").requireString())
                check(body.getQueryParameter("strategy") == "passkey")
                check(
                  Json.parseToJsonElement(
                    checkNotNull(body.getQueryParameter("public_key_credential"))
                  ) == credential
                )
              }
              val rejected =
                scenario == "preparation-rejected" ||
                  (scenario == "submission-rejected" && attempting)
              check(rejected || !attempting) { "Unexpected credential submission" }
              val payload =
                if (rejected)
                  buildJsonObject {
                    put("clerk_trace_id", "session-passkey-trace")
                    put(
                      "errors",
                      buildJsonArray {
                        add(
                          buildJsonObject {
                            put("code", scenario)
                            put("message", "Verification rejected")
                            put("long_message", "Try another passkey.")
                            put(
                              "meta",
                              buildJsonObject {
                                put("param_name", "public_key_credential")
                                put("password", "private response detail")
                              },
                            )
                          }
                        )
                      },
                    )
                  }
                else buildJsonObject { put("response", verification) }
              return buildJsonObject {
                put("status", if (rejected) 422 else 200)
                put("headers", buildJsonObject {})
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
          val user = checkNotNull(clerk.user)
          val session = checkNotNull(clerk.session)
          val failure = runCatching { session.verifyWithPasskey() }.exceptionOrNull()
          check(failure is CoreException) { "Expected structured failure, got $failure" }
          if (scenario.endsWith("-rejected")) {
            check(failure.status == 422)
            check(failure.clerkTraceId == "session-passkey-trace")
            val apiError = failure.errors.single()
            check(apiError.code == scenario)
            check(apiError.message == "Verification rejected")
            check(apiError.longMessage == "Try another passkey.")
            check(apiError.meta?.paramName == "public_key_credential")
            check(!failure.details.toString().contains("private response detail"))
          } else {
            check(
              failure.code ==
                when (scenario) {
                  "provider-failed" -> "host_failure"
                  "invalid-credential" -> "invalid_credential_result"
                  "missing-options" -> "operation_failed"
                  "unsupported" -> "passkey_not_supported"
                  else -> error("Unknown scenario")
                }
            )
          }
          check(!failure.message.contains("private provider detail"))
          check(
            presentations ==
              if (scenario in setOf("submission-rejected", "provider-failed", "invalid-credential"))
                1
              else 0
          )
          check(
            paths ==
              listOf("/v1/client/sessions/sess_native/verify/prepare_first_factor") +
                if (scenario == "submission-rejected")
                  listOf("/v1/client/sessions/sess_native/verify/attempt_first_factor")
                else emptyList()
          )
          check(clerk.user === user && clerk.session === session)
          check(session.status.rawValue == "active")
        } finally {
          clerk.close()
        }
      }
    }
  }
}
