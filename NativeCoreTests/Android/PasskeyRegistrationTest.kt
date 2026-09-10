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
class PasskeyRegistrationTest {
  @Test fun registrationSupportsRenameAndDeletionReceipt() = verify(false)

  @Test fun cancellationDoesNotSubmitCredential() = verify(true)

  private fun verify(cancelled: Boolean) = runBlocking {
    withContext(Dispatchers.Main.immediate) {
      val instrumentation = InstrumentationRegistry.getInstrumentation()
      val base = PackagedFixtures(instrumentation.context)
      base.clientResponse = base.fixtures.getValue("authenticatedClient")
      val options =
        """{"challenge":"Y2hhbGxlbmdl","rp":{"id":"example.com","name":"Example"},"user":{"id":"dXNlcl9uYXRpdmU","name":"test@example.com","displayName":"Test User"},"pubKeyCredParams":[{"type":"public-key","alg":-7}],"authenticatorSelection":{"authenticatorAttachment":"platform","userVerification":"required"},"excludeCredentials":[{"type":"public-key","id":"b2xkX2NyZWRlbnRpYWw"}]}"""
      val credential =
        """{"id":"Y3JlZGVudGlhbA","type":"public-key","rawId":"Y3JlZGVudGlhbA","authenticatorAttachment":"platform","response":{"clientDataJSON":"e30","attestationObject":"YXR0ZXN0YXRpb24","transports":["internal"]}}"""
      var passkey = buildJsonObject {
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
            put("nonce", options)
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
          override val supported = base.supported

          override suspend fun perform(capability: String, arguments: JsonElement): JsonElement {
            val args = arguments.jsonObject
            if (capability == "passkeys.create") {
              presentations++
              check(args.getValue("rp").jsonObject.getValue("id").requireString() == "example.com")
              check(
                args.getValue("challenge").jsonObject.getValue("base64url").requireString() ==
                  "Y2hhbGxlbmdl"
              )
              check(
                args
                  .getValue("user")
                  .jsonObject
                  .getValue("id")
                  .jsonObject
                  .getValue("base64url")
                  .requireString() == "dXNlcl9uYXRpdmU"
              )
              if (cancelled) throw CoreException("user_cancelled")
              return Json.parseToJsonElement(credential)
            }
            if (capability == "http") {
              val url = Uri.parse(args.getValue("url").requireString())
              val path = url.path!!
              if (path.contains("/passkeys")) {
                paths += path
                check(args.getValue("method").requireString() == "POST")
                val method = url.getQueryParameter("_method")
                val body =
                  Uri.parse(
                    "https://fixture.test/?" + (args["body"] as? JsonPrimitive)?.content.orEmpty()
                  )
                val response =
                  when {
                    path.endsWith("/attempt_verification") -> {
                      check(body.getQueryParameter("strategy") == "passkey")
                      val submitted =
                        Json.parseToJsonElement(
                            checkNotNull(body.getQueryParameter("public_key_credential"))
                          )
                          .jsonObject
                      check(
                        submitted
                          .getValue("response")
                          .jsonObject
                          .getValue("attestationObject")
                          .requireString() == "YXR0ZXN0YXRpb24"
                      )
                      passkey =
                        JsonObject(
                          passkey +
                            ("verification" to
                              JsonObject(
                                passkey.getValue("verification").jsonObject +
                                  ("status" to JsonPrimitive("verified"))
                              ))
                        )
                      passkey
                    }
                    method == "PATCH" -> {
                      check(body.getQueryParameter("name") == "New Name")
                      passkey = JsonObject(passkey + ("name" to JsonPrimitive("New Name")))
                      passkey
                    }
                    method == "DELETE" ->
                      buildJsonObject {
                        put("object", "passkey")
                        put("id", "passkey_native")
                        put("deleted", true)
                      }
                    else -> passkey
                  }
                return buildJsonObject {
                  put("status", 200)
                  put("headers", buildJsonObject {})
                  put("body", buildJsonObject { put("response", response) }.toString())
                }
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
        val user = checkNotNull(clerk.user)
        if (cancelled) {
          try {
            user.createPasskey()
            error("Expected cancellation")
          } catch (failure: CoreException) {
            check(failure.code == "user_cancelled")
          }
          check(paths == listOf("/v1/me/passkeys"))
        } else {
          val created = user.createPasskey()
          check(created.id == "passkey_native")
          check(created.verification?.status?.rawValue == "verified")
          val updated = created.update(UpdatePasskeyParams(name = Field.Value("New Name")))
          check(updated === created)
          check(created.name == "New Name")
          val deleted = created.delete()
          check(deleted.id == created.id && deleted.deleted)
          check(created.name == "New Name")
          check(
            paths ==
              listOf(
                "/v1/me/passkeys",
                "/v1/me/passkeys/passkey_native/attempt_verification",
                "/v1/me/passkeys/passkey_native",
                "/v1/me/passkeys/passkey_native",
              )
          )
        }
        check(presentations == 1)
      } finally {
        clerk.close()
      }
    }
  }
}
