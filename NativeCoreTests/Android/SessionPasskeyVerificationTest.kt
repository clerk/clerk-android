package com.clerk.api

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.net.URI
import java.net.URLDecoder
import java.util.Base64
import kotlinx.coroutines.*
import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SessionPasskeyVerificationTest {
  @Test fun generatedPasskeyVerificationSubmitsTheHostCredential() = verify(false)

  @Test fun cancelledPasskeyVerificationDoesNotSubmitAnAttempt() = verify(true)

  private fun verify(cancel: Boolean) = runBlocking {
    withContext(Dispatchers.Main.immediate) {
      val instrumentation = InstrumentationRegistry.getInstrumentation()
      val base = PackagedFixtures(instrumentation.context)
      base.clientResponse = base.fixtures.getValue("authenticatedClient")
      val sessionJSON = base.clientResponse!!.jsonObject.getValue("sessions").jsonArray.single()
      val challenge = "Y2hhbGxlbmdl"
      val credential =
        Json.parseToJsonElement(
          """{"type":"public-key","id":"Y3JlZGVudGlhbA","rawId":"Y3JlZGVudGlhbA","authenticatorAttachment":"platform","response":{"clientDataJSON":"e30","authenticatorData":"YXV0aA","signature":"c2ln","userHandle":null}}"""
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
              assertEquals(buildJsonObject { put("base64url", challenge) }, args["challenge"])
              assertEquals(JsonPrimitive(false), args["conditionalUI"])
              assertEquals(JsonPrimitive("native-core.clerk.accounts.dev"), args["rpId"])
              if (cancel)
                throw CoreException("passkey_operation_cancelled", kind = CoreFailureKind.Cancelled)
              return credential
            }
            if (capability != "http") return base.perform(capability, arguments)
            val path = URI(args.getValue("url").requireString()).path
            if (!path.contains("/verify/")) return base.perform(capability, arguments)
            assertEquals("POST", args.getValue("method").requireString())
            paths += path
            val body =
              args.getValue("body").requireString().split("&").associate {
                val pair = it.split("=", limit = 2)
                URLDecoder.decode(pair[0], "UTF-8") to
                  URLDecoder.decode(pair.getOrElse(1) { "" }, "UTF-8")
              }
            val attempt = path.endsWith("/attempt_first_factor")
            assertEquals("passkey", body["strategy"])
            if (attempt) {
              assertEquals(setOf("strategy", "public_key_credential"), body.keys)
              val submitted =
                Json.parseToJsonElement(body.getValue("public_key_credential")).jsonObject
              assertEquals(credential.jsonObject["id"], submitted["id"])
              assertEquals(credential.jsonObject["response"], submitted["response"])
            } else {
              assertEquals("/v1/client/sessions/sess_native/verify/prepare_first_factor", path)
              assertEquals(mapOf("strategy" to "passkey"), body)
            }
            val verification = buildJsonObject {
              put("object", "session_verification")
              put("id", "sv_passkey")
              put("status", if (attempt) "complete" else "needs_first_factor")
              put("level", "first_factor")
              put("session", sessionJSON)
              put(
                "first_factor_verification",
                buildJsonObject {
                  put("status", if (attempt) "verified" else "unverified")
                  put("strategy", "passkey")
                  put("attempts", if (attempt) 1 else 0)
                  put("expire_at", JsonNull)
                  put("error", JsonNull)
                  put("verified_at_client", JsonNull)
                  if (!attempt)
                    put(
                      "nonce",
                      buildJsonObject {
                          put("challenge", challenge)
                          put("rpId", "native-core.clerk.accounts.dev")
                          put("userVerification", "required")
                        }
                        .toString(),
                    )
                },
              )
              put("second_factor_verification", JsonNull)
              put(
                "supported_first_factors",
                buildJsonArray { add(buildJsonObject { put("strategy", "passkey") }) },
              )
              put("supported_second_factors", JsonNull)
            }
            return buildJsonObject {
              put("status", 200)
              put("headers", buildJsonObject {})
              put("body", buildJsonObject { put("response", verification) }.toString())
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
        val session = clerk.session!!
        val result = runCatching { session.verifyWithPasskey() }
        if (cancel) {
          val error = result.exceptionOrNull()
          assertTrue(error is CoreException)
          assertEquals("passkey_operation_cancelled", (error as CoreException).code)
        } else {
          val verification = result.getOrThrow()
          assertEquals(SessionVerificationStatus.Complete, verification.status)
          assertEquals(SessionVerificationLevel.FirstFactor, verification.level)
          assertEquals(VerificationStatus.Verified, verification.firstFactorVerification.status)
          assertEquals(session.id, verification.session.id)
        }
        assertEquals(1, presentations)
        assertEquals(
          listOf("/v1/client/sessions/sess_native/verify/prepare_first_factor") +
            if (cancel) emptyList()
            else listOf("/v1/client/sessions/sess_native/verify/attempt_first_factor"),
          paths,
        )
        assertSame(session, clerk.session)
      } finally {
        clerk.close()
      }
    }
  }
}
