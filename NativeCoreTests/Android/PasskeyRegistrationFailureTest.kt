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
class PasskeyRegistrationFailureTest {
  @Test
  fun creationFailurePreservesAPIErrorWithoutPresentingCredential() = verify("creation-rejected")

  @Test
  fun verificationFailurePreservesAPIErrorWithoutRetryingCredential() =
    verify("verification-rejected")

  @Test
  fun providerFailureDoesNotSubmitCredentialOrExposePrivateDetails() = verify("provider-failed")

  @Test fun invalidCredentialDoesNotSubmitVerification() = verify("invalid-credential")

  @Test fun missingOptionsFailBeforeCredentialPresentation() = verify("missing-options")

  @Test fun unsupportedPasskeysDoNotCreateServerResource() = verify("unsupported")

  private fun verify(scenario: String) = runBlocking {
    withTimeout(15000) {
      withContext(Dispatchers.Main.immediate) {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val base = PackagedFixtures(instrumentation.context)
        base.clientResponse = base.fixtures.getValue("authenticatedClient")
        val options =
          """{
          "challenge":"Y2hhbGxlbmdl","rp":{"id":"example.com","name":"Example"},
          "user":{"id":"dXNlcl9uYXRpdmU","name":"test@example.com","displayName":"Test User"},
          "pubKeyCredParams":[{"type":"public-key","alg":-7}]
        }"""
        val credential =
          Json.parseToJsonElement(
            """{
          "id":"Y3JlZGVudGlhbA","type":"public-key","rawId":"Y3JlZGVudGlhbA","authenticatorAttachment":"platform",
          "response":{"clientDataJSON":"e30","attestationObject":"YXR0ZXN0YXRpb24","transports":["internal"]}
        }"""
          )
        val passkey = buildJsonObject {
          put("object", "passkey")
          put("id", "passkey_native")
          put("name", JsonNull)
          put("last_used_at", JsonNull)
          put("created_at", 1700000000000L)
          put("updated_at", 1700000000000L)
          put(
            "verification",
            buildJsonObject {
              put("status", "unverified")
              put("strategy", "passkey")
              put("nonce", if (scenario == "missing-options") JsonNull else JsonPrimitive(options))
              put("attempts", JsonNull)
              put("expire_at", JsonNull)
              put("error", JsonNull)
              put("verified_at_client", JsonNull)
            },
          )
        }
        var presentations = 0
        val paths = mutableListOf<String>()
        val host =
          object : NativeCapabilities {
            override val supported =
              if (scenario == "unsupported") base.supported - "passkeys" else base.supported

            override suspend fun perform(capability: String, arguments: JsonElement): JsonElement {
              if (capability == "passkeys.create") {
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
              if (!path.contains("/passkeys")) return base.perform(capability, arguments)
              paths += path
              check(args.getValue("method").requireString() == "POST")
              val attempting = path.endsWith("/attempt_verification")
              check(
                path ==
                  if (attempting) "/v1/me/passkeys/passkey_native/attempt_verification"
                  else "/v1/me/passkeys"
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
                scenario == "creation-rejected" ||
                  (scenario == "verification-rejected" && attempting)
              check(rejected || !attempting) { "Unexpected credential submission" }
              val payload =
                if (rejected)
                  buildJsonObject {
                    put("clerk_trace_id", "passkey-fixture-trace")
                    put(
                      "errors",
                      buildJsonArray {
                        add(
                          buildJsonObject {
                            put("code", scenario)
                            put("message", "Registration rejected")
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
                else buildJsonObject { put("response", passkey) }
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
          val failure = runCatching { user.createPasskey() }.exceptionOrNull()
          check(failure is CoreException) { "Expected structured failure, got $failure" }
          if (scenario.endsWith("-rejected")) {
            check(failure.status == 422)
            check(failure.clerkTraceId == "passkey-fixture-trace")
            val apiError = failure.errors.single()
            check(apiError.code == scenario)
            check(apiError.message == "Registration rejected")
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
              if (
                scenario in setOf("verification-rejected", "provider-failed", "invalid-credential")
              )
                1
              else 0
          )
          check(
            paths ==
              if (scenario == "unsupported") emptyList<String>()
              else
                listOf("/v1/me/passkeys") +
                  if (scenario == "verification-rejected")
                    listOf("/v1/me/passkeys/passkey_native/attempt_verification")
                  else emptyList()
          )
          check(clerk.user === user && clerk.session === session)
          check(user.passkeys.isEmpty())
        } finally {
          clerk.close()
        }
      }
    }
  }
}
