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
class EmailLinkCompletionTest {
  @Test fun unknownAndroidStateCannotRedeemVerifier() = verify("bad-state")

  @Test fun nullAndroidStateCannotRedeemVerifier() = verify("null-state")

  @Test fun missingLegacyIOSKindStillSignsIn() = verify("missing-ios-kind")

  @Test fun nullLegacyIOSKindStillSignsIn() = verify("null-ios-kind")

  @Test fun validAndroidSignInStateStillSignsIn() = verify("valid-android")

  @Test fun incompleteSignInPublishesContinuation() = verify("incomplete-signIn")

  @Test fun incompleteSignUpPublishesContinuation() = verify("incomplete-signUp")

  @Test fun signUpCannotConsumeSignInTicket() = verify("signup-ticket")

  private fun verify(scenario: String) = runBlocking {
    withContext(Dispatchers.Main.immediate) {
      val instrumentation = InstrumentationRegistry.getInstrumentation()
      val host = EmailLinkCompletionHost(PackagedFixtures(instrumentation.context), scenario)
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
        if (!host.imported) {
          if (host.signup) clerk.signUp.verifications.sendEmailLink(SignUpEmailLinkSendParams())
          else
            clerk.signIn.emailLink.sendLink(
              SignInEmailLinkSendParams.Case2(SignInEmailLinkSendLinkParamsCase2("idn_email"))
            )
        }
        val flow = if (host.signup) "sua_native" else "sia_native"
        val expectedError =
          when (scenario) {
            "bad-state",
            "null-state" -> "no_pending_email_link"
            "signup-ticket" -> "invalid_email_link_response"
            else -> null
          }
        try {
          val result =
            checkNotNull(
              clerk.handleAuthCallback(
                URI("clerk-test://sso-callback?flow_id=$flow&approval_token=fixture_approval")
              )
            )
          check(expectedError == null)
          if (host.signup) {
            check(
              result is MobileAuthenticationResult.Case2 && result.value.signUp === clerk.signUp
            )
            check(clerk.signUp.status.rawValue == "missing_requirements")
            check(clerk.signUp.missingFields.map { it.rawValue } == listOf("first_name"))
            check(clerk.signUp.createdSessionId == null)
          } else {
            check(
              result is MobileAuthenticationResult.Case1 && result.value.signIn === clerk.signIn
            )
            check(
              clerk.signIn.status.rawValue ==
                if (scenario == "incomplete-signIn") "needs_second_factor" else "complete"
            )
          }
          check(clerk.authCallback?.result?.kind == if (host.signup) "signUp" else "signIn")
        } catch (error: CoreException) {
          check(expectedError != null)
          check(error.code == expectedError)
          check(clerk.authCallback == null)
        }
        check(host.base.authRecord == null && clerk.session == null)
        check(host.completions == if (scenario in listOf("bad-state", "null-state")) 0 else 1)
        check(host.tickets == if (host.signup || expectedError != null) 0 else 1)
      } finally {
        clerk.close()
      }
    }
  }
}

private class EmailLinkCompletionHost(val base: PackagedFixtures, val scenario: String) :
  NativeCapabilities {
  override val supported = base.supported
  val imported = !scenario.startsWith("incomplete") && scenario != "signup-ticket"
  val signup = scenario in listOf("incomplete-signUp", "signup-ticket")
  var completions = 0
  var tickets = 0
  val signIn =
    JsonObject(
      base.fixtures.getValue("signIn").jsonObject +
        ("supported_first_factors" to
          buildJsonArray {
            add(
              buildJsonObject {
                put("strategy", "email_link")
                put("email_address_id", "idn_email")
                put("safe_identifier", "test@example.com")
              }
            )
          })
    )
  val signUp =
    JsonObject(
      base.fixtures.getValue("signUp").jsonObject +
        ("email_address" to JsonPrimitive("test@example.com"))
    )

  init {
    base.clientResponse =
      JsonObject(
        base.fixtures.getValue("client").jsonObject +
          mapOf("sign_in" to signIn, "sign_up" to signUp)
      )
    if (imported) {
      val now = System.currentTimeMillis()
      base.authRecord =
        buildJsonObject {
            if (scenario.contains("ios-kind")) {
              put("flow_id", "sia_native")
              put("code_verifier", "v".repeat(43))
              put("created_at", now)
              put("expires_at", now + 600000)
              if (scenario == "null-ios-kind") put("kind", JsonNull)
            } else {
              put(
                "state",
                if (scenario == "null-state") JsonNull
                else JsonPrimitive(if (scenario == "valid-android") "SIGN_IN" else "UNSUPPORTED"),
              )
              put("flowId", "sia_native")
              put("codeVerifier", "v".repeat(43))
              put("createdAtEpochMs", now)
              put("expiresAtEpochMs", now + 600000)
            }
          }
          .toString()
    }
  }

  override suspend fun perform(capability: String, arguments: JsonElement): JsonElement {
    if (capability != "http") return base.perform(capability, arguments)
    val path = URI(arguments.jsonObject.getValue("url").requireString()).path
    val resource: JsonElement =
      when {
        path.endsWith("/magic_links/complete") -> {
          completions++
          if (signup && scenario != "signup-ticket")
            JsonObject(
              signUp +
                mapOf(
                  "status" to JsonPrimitive("missing_requirements"),
                  "created_session_id" to JsonNull,
                  "missing_fields" to buildJsonArray { add("first_name") },
                )
            )
          else buildJsonObject { put("ticket", "fixture_ticket") }
        }
        path.endsWith("/sign_ins") -> {
          tickets++
          JsonObject(
            signIn +
              mapOf(
                "status" to
                  JsonPrimitive(
                    if (scenario == "incomplete-signIn") "needs_second_factor" else "complete"
                  ),
                "created_session_id" to
                  if (scenario == "incomplete-signIn") JsonNull else JsonPrimitive("sess_native"),
              )
          )
        }
        path.contains("/sign_ins/") -> signIn
        path.contains("/sign_ups/") -> signUp
        else -> return base.perform(capability, arguments)
      }
    return buildJsonObject {
      put("status", 200)
      put("headers", buildJsonObject {})
      put("body", buildJsonObject { put("response", resource) }.toString())
    }
  }
}
