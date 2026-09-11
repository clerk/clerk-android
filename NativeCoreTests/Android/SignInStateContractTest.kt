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
class SignInStateContractTest {
  @Test fun nestedSignInPreservesClientTrustStatus() = status("needs_client_trust", false)

  @Test fun nestedSignInPreservesUnknownStatus() = status("future_sign_in_status", false)

  @Test
  fun serverUpdatesReplaceUnknownStatusOnTheSameResource() = status("future_sign_in_status", true)

  @Test fun resetEmailCodeSendsSelectedIdentifierAndStrategy() = resetCode(false)

  @Test fun resetPhoneCodeSendsSelectedIdentifierAndStrategy() = resetCode(true)

  @Test
  fun browserSsoStartsFromNeedsIdentifierWithoutAnEmail() = fixture { base, connect ->
    val clerk = connect(base)
    try {
      val signIn = clerk.signIn
      check(signIn.status == SignInStatus.NeedsIdentifier)
      signIn.sso(SignInSSOParams(strategy = SignInSSOParamsStrategy.OauthGoogle))
      val requests =
        base.requests.filter {
          Uri.parse(it.getValue("url").requireString()).path!!.contains("/sign_ins")
        }
      check(requests.size == 2)
      val first = requests.first()
      check(Uri.parse(first.getValue("url").requireString()).path == "/v1/client/sign_ins")
      check(first["method"] == JsonPrimitive("POST"))
      val body = Uri.parse("https://fixture.test/?" + first.getValue("body").requireString())
      check(body.getQueryParameter("strategy") == "oauth_google")
      check(body.getQueryParameter("redirect_url") == "clerk-test://sso-callback")
      check(body.getQueryParameter("identifier") == null)
      check(clerk.signIn === signIn && signIn.status == SignInStatus.Complete)
      check(clerk.session == null && clerk.user == null)
    } finally {
      clerk.close()
    }
  }

  private fun status(initial: String, update: Boolean) = fixture { base, connect ->
    fun signIn(value: String) =
      JsonObject(
        base.fixtures.getValue("signIn").jsonObject +
          mapOf(
            "status" to JsonPrimitive(value),
            "identifier" to JsonPrimitive("person@example.com"),
            "supported_identifiers" to JsonArray(listOf(JsonPrimitive("email_address"))),
            "created_session_id" to
              if (value == "complete") JsonPrimitive("sess_native") else JsonNull,
          )
      )
    base.clientResponse =
      JsonObject(base.fixtures.getValue("client").jsonObject + ("sign_in" to signIn(initial)))
    var returned = initial
    val host =
      object : NativeCapabilities {
        override val supported = base.supported

        override suspend fun perform(capability: String, arguments: JsonElement): JsonElement {
          val args = arguments.jsonObject
          if (
            capability == "http" &&
              Uri.parse(args.getValue("url").requireString()).path!!.contains("/sign_ins")
          ) {
            check(args["method"] == JsonPrimitive("POST"))
            return buildJsonObject {
              put("status", 200)
              put("headers", buildJsonObject {})
              put("body", buildJsonObject { put("response", signIn(returned)) }.toString())
            }
          }
          return base.perform(capability, arguments)
        }
      }
    val clerk = connect(host)
    try {
      val resource = clerk.signIn
      check(resource.status.rawValue == initial && resource.identifier == "person@example.com")
      if (initial == "needs_client_trust") check(resource.status == SignInStatus.NeedsClientTrust)
      else check(resource.status == SignInStatus.Unrecognized(initial))
      check(resource.createdSessionId == null && clerk.session == null && clerk.user == null)
      if (update) {
        for (value in listOf("another_future_status", "complete")) {
          returned = value
          resource.create(SignInCreateParams(identifier = "person@example.com"))
          check(clerk.signIn === resource && resource.status.rawValue == value)
          check(
            resource.status ==
              if (value == "complete") SignInStatus.Complete else SignInStatus.Unrecognized(value)
          )
          check(resource.createdSessionId == if (value == "complete") "sess_native" else null)
          check(clerk.session == null && clerk.user == null)
        }
      }
    } finally {
      clerk.close()
    }
  }

  private fun resetCode(phone: Boolean) = fixture { base, connect ->
    val strategy = if (phone) "reset_password_phone_code" else "reset_password_email_code"
    val field = if (phone) "phone_number_id" else "email_address_id"
    val signIn =
      JsonObject(
        base.fixtures.getValue("signIn").jsonObject +
          mapOf(
            "status" to JsonPrimitive("needs_first_factor"),
            "supported_first_factors" to
              buildJsonArray {
                for (id in listOf("first", "selected")) add(
                  buildJsonObject {
                    put("strategy", strategy)
                    put(field, id)
                    put("safe_identifier", id)
                  }
                )
              },
          )
      )
    base.clientResponse =
      JsonObject(base.fixtures.getValue("client").jsonObject + ("sign_in" to signIn))
    val clerk = connect(base)
    try {
      val resource = clerk.signIn
      if (phone)
        resource.resetPasswordPhoneCode.sendCode(
          SignInResetPasswordPhoneCodeSendParams(phoneNumberId = "selected")
        )
      else
        resource.resetPasswordEmailCode.sendCode(
          SignInResetPasswordEmailCodeSendParams(emailAddressId = "selected")
        )
      val requests =
        base.requests.filter {
          Uri.parse(it.getValue("url").requireString()).path!!.contains("/sign_ins")
        }
      check(requests.size == 1)
      val request = requests.single()
      check(
        Uri.parse(request.getValue("url").requireString()).path ==
          "/v1/client/sign_ins/${resource.id}/prepare_first_factor"
      )
      check(request["method"] == JsonPrimitive("POST"))
      val body = Uri.parse("https://fixture.test/?" + request.getValue("body").requireString())
      check(
        body.getQueryParameter("strategy") == strategy &&
          body.getQueryParameter(field) == "selected"
      )
      check(
        body.getQueryParameter("emailAddressId") == null &&
          body.getQueryParameter("phoneNumberId") == null
      )
      check(clerk.signIn === resource && clerk.session == null && clerk.user == null)
    } finally {
      clerk.close()
    }
  }

  private fun fixture(
    block: suspend (PackagedFixtures, suspend (NativeCapabilities) -> Clerk) -> Unit
  ) = runBlocking {
    withTimeout(15000) {
      withContext(Dispatchers.Main.immediate) {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val base = PackagedFixtures(instrumentation.context)
        block(base) { host ->
          Clerk.connect(
            instrumentation.targetContext,
            ClerkConfiguration(
              "pk_test_" +
                Base64.getEncoder().encodeToString("native-core.clerk.accounts.dev$".toByteArray()),
              "clerk-test://sso-callback",
            ),
            host,
          )
        }
      }
    }
  }
}
