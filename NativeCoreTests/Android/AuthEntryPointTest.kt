package com.clerk.api

import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.net.URI
import java.util.Base64
import java.util.Locale
import kotlinx.coroutines.*
import kotlinx.serialization.json.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AuthEntryPointTest {
  @Test fun emailCodeUsesExplicitPreparation() = codeFlow(false)

  @Test fun phoneCodeUsesExplicitPreparation() = codeFlow(true)

  private fun codeFlow(phone: Boolean) = fixture { clerk, base ->
    val strategy = if (phone) "phone_code" else "email_code"
    val field = if (phone) "phone_number_id" else "email_address_id"
    val identifier = if (phone) "+15555550123" else "user@example.com"
    base.signInFirstFactors = buildJsonArray {
      add(
        buildJsonObject {
          put("strategy", strategy)
          put(field, "idn_selected")
          put("safe_identifier", identifier)
        }
      )
    }
    val signIn = clerk.signIn
    signIn.create(SignInCreateParams(identifier = identifier))
    var requests = authRequests(base)
    check(requests.size == 1 && path(requests.single()).endsWith("/sign_ins"))
    val create = body(requests.single())
    check(create.getQueryParameter("identifier") == identifier)
    check(create.getQueryParameter("locale") == Locale.getDefault().toLanguageTag())
    check(create.getQueryParameter("strategy") == null)
    check(signIn === clerk.signIn && signIn.status.rawValue == "needs_first_factor")
    if (phone)
      signIn.phoneCode.sendCode(
        SignInPhoneCodeSendParams.Case2(
          SignInPhoneCodeSendCodeParamsCase2(phoneNumberId = "idn_selected")
        )
      )
    else
      signIn.emailCode.sendCode(
        SignInEmailCodeSendParams.Case2(
          SignInEmailCodeSendCodeParamsCase2(emailAddressId = "idn_selected")
        )
      )
    requests = authRequests(base)
    check(
      requests.size == 2 &&
        path(requests.last()).endsWith("/sign_ins/${signIn.id}/prepare_first_factor")
    )
    val prepare = body(requests.last())
    check(
      prepare.getQueryParameter("strategy") == strategy &&
        prepare.getQueryParameter(field) == "idn_selected"
    )
    for (request in requests) {
      check(body(request).getQueryParameter("redirect_uri") == null)
      check(body(request).getQueryParameter("code_challenge") == null)
      check(body(request).getQueryParameter("redirect_url") == null)
    }
    check(base.authRecord == null && clerk.session == null && clerk.user == null)
  }

  @Test
  fun signupPreservesNestedMetadata() = fixture { clerk, base ->
    val metadata = buildJsonObject {
      put("test", "test")
      putJsonObject("nested") { put("active", true) }
    }
    clerk.signUp.create(
      SignUpCreateParams(emailAddress = "user@example.com", unsafeMetadata = metadata)
    )
    val request = authRequests(base).single()
    check(path(request).endsWith("/sign_ups"))
    val body = body(request)
    check(body.getQueryParameter("email_address") == "user@example.com")
    check(
      Json.parseToJsonElement(checkNotNull(body.getQueryParameter("unsafe_metadata"))) == metadata
    )
    check(clerk.session == null && clerk.user == null)
  }

  @Test
  fun unrelatedAndOAuthCallbacksAreNotRedeemedAsEmailLinks() = fixture { clerk, base ->
    val before = base.requests.size
    for (url in
      listOf(
        "https://example.com/path",
        "clerk://callback?rotating_token_nonce=fixture_nonce",
        "clerk-test://sso-callback?rotating_token_nonce=fixture_nonce",
        "clerk-test://sso-callback?approval_token=fixture_approval",
      )) check(clerk.handleAuthCallback(URI(url)) == null)
    check(base.requests.size == before && base.authRecord == null)
    check(clerk.authCallback == null && clerk.session == null)
  }

  private fun fixture(block: suspend (Clerk, PackagedFixtures) -> Unit) = runBlocking {
    withTimeout(15000) {
      withContext(Dispatchers.Main.immediate) {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val base = PackagedFixtures(instrumentation.context)
        val key =
          "pk_test_" +
            Base64.getEncoder().encodeToString("native-core.clerk.accounts.dev$".toByteArray())
        val clerk =
          Clerk.connect(
            instrumentation.targetContext,
            ClerkConfiguration(key, "clerk-test://sso-callback"),
            base,
          )
        try {
          block(clerk, base)
        } finally {
          clerk.close()
        }
      }
    }
  }

  private fun path(args: JsonObject): String = URI(args.getValue("url").requireString()).path

  private fun body(args: JsonObject): Uri =
    Uri.parse("https://fixture.invalid/?" + (args["body"]?.jsonPrimitive?.contentOrNull ?: ""))

  private fun authRequests(base: PackagedFixtures) =
    base.requests.filter { path(it).contains("/sign_ins") || path(it).contains("/sign_ups") }
}
