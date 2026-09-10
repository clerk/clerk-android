package com.clerk.api

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.net.URI
import java.net.URLDecoder
import java.util.Base64
import kotlinx.coroutines.*
import kotlinx.serialization.json.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppleSSOErrorTest {
  @Test fun waitlistAllowsExistingUser() = verify("waitlist-existing")

  @Test fun waitlistKeepsNewUserBlocked() = verify("waitlist-new")

  @Test fun transferPreservesVerificationError() = verify("transfer-verification-error")

  @Test fun transferPreservesRequestError() = verify("transfer-request-error")

  @Test fun unrelatedFailureDoesNotFallback() = verify("unrelated-signup-error")

  @Test fun explicitSignupDoesNotFallback() = verify("signup-only-restricted")

  private fun verify(scenario: String) = runBlocking {
    withContext(Dispatchers.Main.immediate) {
      val instrumentation = InstrumentationRegistry.getInstrumentation()
      val host = AppleSSOErrorHost(PackagedFixtures(instrumentation.context), scenario)
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
        try {
          val result =
            clerk.authenticateWithSSO(
              MobileSSOParams(
                strategy = SignInSSOParamsStrategy.OauthTokenApple,
                start =
                  if (scenario == "signup-only-restricted") MobileSSOParamsStart.SignUp
                  else MobileSSOParamsStart.Auto,
                transferable = true,
              )
            )
          check(scenario == "waitlist-existing")
          check(result is MobileAuthenticationResult.Case1 && result.value.signIn === clerk.signIn)
          check(clerk.signIn.status.rawValue == "complete")
        } catch (error: CoreException) {
          check(scenario != "waitlist-existing")
          val expected =
            when (scenario) {
              "waitlist-new" -> "sign_up_restricted_waitlist"
              "signup-only-restricted" -> "sign_up_mode_restricted"
              "unrelated-signup-error" -> "form_param_invalid"
              else -> "account_locked"
            }
          val detail = checkNotNull(error.errors.firstOrNull())
          check(detail.code == expected)
          check(detail.message == "Apple authentication rejected")
          check(detail.longMessage == "Choose another sign-in method")
          check(detail.meta?.paramName == "token")
          check(error.status == if (scenario == "transfer-verification-error") null else 403)
        }
        check(clerk.session == null)
        check(host.prompts == 1 && host.browsers == 0)
        check(
          host.requests.size ==
            if (scenario in listOf("unrelated-signup-error", "signup-only-restricted")) 1 else 2
        )
        check(host.body(0)["token"] == "apple-fixture-token")
        if (scenario.startsWith("waitlist")) {
          check(host.body(1)["token"] == "apple-fixture-token")
          check(host.body(1)["transfer"] == null)
        }
        if (scenario.startsWith("transfer")) check(host.body(1)["transfer"] == "true")
      } finally {
        clerk.close()
      }
    }
  }
}

private class AppleSSOErrorHost(val base: PackagedFixtures, val scenario: String) :
  NativeCapabilities {
  override val supported = base.supported + "appleIdentity"
  val requests = mutableListOf<JsonObject>()
  var prompts = 0
  var browsers = 0

  fun body(index: Int): Map<String, String> =
    requests[index]["body"]?.jsonPrimitive?.contentOrNull?.split("&")?.associate {
      val pair = it.split("=", limit = 2)
      URLDecoder.decode(pair[0], "UTF-8") to URLDecoder.decode(pair.getOrElse(1) { "" }, "UTF-8")
    } ?: emptyMap()

  override suspend fun perform(capability: String, arguments: JsonElement): JsonElement {
    if (capability == "appleIdentity") {
      prompts++
      return buildJsonObject {
        put("token", "apple-fixture-token")
        put("firstName", "Apple")
        put("lastName", "User")
      }
    }
    if (capability == "browser") browsers++
    if (capability != "http") return base.perform(capability, arguments)
    val args = arguments.jsonObject
    val path = URI(args.getValue("url").requireString()).path
    if (!path.contains("/sign_ins") && !path.contains("/sign_ups"))
      return base.perform(capability, arguments)
    requests += args
    val signup = path.contains("/sign_ups")
    val failed =
      if (signup)
        scenario.startsWith("waitlist") ||
          scenario in listOf("unrelated-signup-error", "signup-only-restricted")
      else scenario == "transfer-request-error"
    val code =
      when {
        scenario.startsWith("waitlist") -> "sign_up_restricted_waitlist"
        scenario == "signup-only-restricted" -> "sign_up_mode_restricted"
        scenario == "unrelated-signup-error" -> "form_param_invalid"
        else -> "account_locked"
      }
    val error = buildJsonObject {
      put("code", code)
      put("message", "Apple authentication rejected")
      put("long_message", "Choose another sign-in method")
      putJsonObject("meta") {
        put("param_name", "token")
        put("password", "must-not-cross")
      }
    }
    val document =
      if (failed) buildJsonObject { putJsonArray("errors") { add(error) } }
      else {
        val resource =
          base.fixtures.getValue(if (signup) "signUp" else "signIn").jsonObject.toMutableMap()
        if (signup) {
          val verifications = resource.getValue("verifications").jsonObject.toMutableMap()
          val verification = verifications.getValue("external_account").jsonObject.toMutableMap()
          verification["status"] = JsonPrimitive("transferable")
          verification["error"] = buildJsonObject {
            put("code", "external_account_exists")
            put("message", "Account exists")
          }
          verification["strategy"] = JsonPrimitive("oauth_token_apple")
          verifications["external_account"] = JsonObject(verification)
          resource["verifications"] = JsonObject(verifications)
        } else if (scenario in listOf("transfer-verification-error", "waitlist-new")) {
          val verification =
            resource.getValue("first_factor_verification").jsonObject.toMutableMap()
          verification["status"] =
            JsonPrimitive(if (scenario == "waitlist-new") "transferable" else "failed")
          verification["strategy"] = JsonPrimitive("oauth_token_apple")
          verification["error"] = if (scenario == "waitlist-new") JsonNull else error
          resource["first_factor_verification"] = JsonObject(verification)
        } else {
          resource["status"] = JsonPrimitive("complete")
          resource["created_session_id"] = JsonPrimitive("sess_native")
        }
        buildJsonObject { put("response", JsonObject(resource)) }
      }
    return buildJsonObject {
      put("status", if (failed) 403 else 200)
      put("headers", buildJsonObject {})
      put("body", document.toString())
    }
  }
}
