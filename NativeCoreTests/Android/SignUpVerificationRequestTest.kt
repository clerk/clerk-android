package com.clerk.api

import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.security.MessageDigest
import java.util.Base64
import kotlinx.coroutines.*
import kotlinx.serialization.json.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SignUpVerificationRequestTest {
  @Test fun emailLinkSendsConfiguredCallbackAndMatchingPkceChallenge() = verify("email_link")

  @Test fun emailCodeUsesExplicitVerificationStrategy() = verify("email_code")

  @Test fun phoneCodeUsesExplicitVerificationStrategy() = verify("phone_code")

  private fun verify(strategy: String) = runBlocking {
    withTimeout(15000) {
      withContext(Dispatchers.Main.immediate) {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val host = PackagedFixtures(instrumentation.context)
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
          clerk.signUp.create(
            SignUpCreateParams(emailAddress = "sam@clerk.dev", phoneNumber = "+15555550123")
          )
          val before = host.requests.size
          when (strategy) {
            "email_link" -> clerk.signUp.verifications.sendEmailLink(SignUpEmailLinkSendParams())
            "email_code" -> clerk.signUp.verifications.sendEmailCode()
            "phone_code" -> clerk.signUp.verifications.sendPhoneCode()
          }
          val request = host.requests.drop(before).single()
          check(
            Uri.parse(request.getValue("url").requireString()).path ==
              "/v1/client/sign_ups/sua_native/prepare_verification"
          )
          check(request["method"] == JsonPrimitive("POST"))
          val body = Uri.parse("https://fixture.test/?" + request.getValue("body").requireString())
          check(body.getQueryParameter("strategy") == strategy)
          if (strategy == "email_link") {
            check(body.getQueryParameter("redirect_uri") == "clerk-test://sso-callback")
            check(body.getQueryParameter("code_challenge_method") == "S256")
            val pending = Json.parseToJsonElement(checkNotNull(host.authRecord)).jsonObject
            val verifier = pending.getValue("codeVerifier").requireString()
            check(verifier.isNotBlank())
            val challenge =
              Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(MessageDigest.getInstance("SHA-256").digest(verifier.toByteArray()))
            check(body.getQueryParameter("code_challenge") == challenge)
            check(body.getQueryParameter("code_verifier") == null)
          } else check(host.authRecord == null)
          check(clerk.session == null && clerk.user == null)
        } finally {
          clerk.close()
        }
      }
    }
  }
}
