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
class SessionVerificationTest {
  @Test fun generatedVerificationReturnsUsableFactorState() = runBlocking {
    withContext(Dispatchers.Main.immediate) {
      val instrumentation = InstrumentationRegistry.getInstrumentation()
      val base = PackagedFixtures(instrumentation.context)
      base.clientResponse = base.fixtures.getValue("authenticatedClient")
      val paths = mutableListOf<String>()
      val bodies = mutableListOf<Map<String, String>>()
      var failNext = false
      val host = object : NativeCapabilities {
        override val supported = base.supported
        override suspend fun perform(capability: String, arguments: JsonElement): JsonElement {
          if (capability != "http") return base.perform(capability, arguments)
          val args = arguments.jsonObject
          val path = URI(args.getValue("url").requireString()).path
          if (!path.contains("/verify")) return base.perform(capability, arguments)
          check(args.getValue("method").requireString() == "POST")
          paths += path
          bodies += args.getValue("body").requireString().split("&").associate {
            val pair = it.split("=", limit = 2)
            URLDecoder.decode(pair[0], "UTF-8") to URLDecoder.decode(pair.getOrElse(1) { "" }, "UTF-8")
          }
          if (failNext) {
            failNext = false
            return buildJsonObject { put("status", 422); put("headers", buildJsonObject {}); put("body", """{"errors":[{"code":"form_code_incorrect","message":"Incorrect code"}]}""") }
          }
          val verification = Json.parseToJsonElement("""{"object":"session_verification","id":"sv_fixture","status":"needs_first_factor","level":"multi_factor","first_factor_verification":{"status":"unverified","strategy":"enterprise_sso","attempts":0,"expire_at":null,"error":null,"verified_at_client":null},"second_factor_verification":null,"supported_first_factors":[{"strategy":"enterprise_sso","enterprise_connection_id":"econn_123","enterprise_connection_name":"Acme"}],"supported_second_factors":[{"strategy":"phone_code","phone_number_id":"idn_phone","safe_identifier":"+15555550123","primary":true,"default":true}]}""").jsonObject.toMutableMap()
          verification["session"] = base.fixtures.getValue("authenticatedClient").jsonObject.getValue("sessions").jsonArray[0]
          verification["status"] = JsonPrimitive(if (path.contains("attempt")) "complete" else if (path.contains("second")) "needs_second_factor" else "needs_first_factor")
          return buildJsonObject { put("status", 200); put("headers", buildJsonObject {}); put("body", buildJsonObject { put("response", JsonObject(verification)) }.toString()) }
        }
      }
      val key = "pk_test_" + Base64.getEncoder().encodeToString("native-core.clerk.accounts.dev$".toByteArray())
      val clerk = Clerk.connect(instrumentation.targetContext, ClerkConfiguration(key, "clerk-test://sso-callback"), host)
      try {
        val session = checkNotNull(clerk.session)
        fun verify(result: SessionVerification, status: SessionVerificationStatus, path: String, body: Map<String, String>) {
          check(result.status == status && result.level == SessionVerificationLevel.MultiFactor)
          check(result.session.id == session.id)
          check(result.firstFactorVerification.status == VerificationStatus.Unverified && result.secondFactorVerification.status == null)
          val enterprise = (result.supportedFirstFactors!!.first() as SessionVerificationFirstFactor.Case5).value
          val phone = (result.supportedSecondFactors!!.first() as SessionVerificationSecondFactor.Case1).value
          check(enterprise.enterpriseConnectionId == "econn_123" && enterprise.enterpriseConnectionName == "Acme")
          check(phone.phoneNumberId == "idn_phone" && phone.safeIdentifier == "+15555550123" && phone.primary == true && phone.default == true)
          check(paths.last() == "/v1/client/sessions/sess_native/$path" && bodies.last() == body)
        }
        for (level in listOf(SessionVerificationLevel.FirstFactor, SessionVerificationLevel.SecondFactor, SessionVerificationLevel.MultiFactor)) {
          verify(session.startVerification(SessionVerifyCreateParams(level)), SessionVerificationStatus.NeedsFirstFactor, "verify", mapOf("level" to level.rawValue))
        }
        val first = listOf(
          SessionVerifyPrepareFirstFactorParams.Case1(PasskeyFactor()) to mapOf("strategy" to "passkey"),
          SessionVerifyPrepareFirstFactorParams.Case2(EmailCodeConfig(emailAddressId = "idn_email")) to mapOf("strategy" to "email_code", "email_address_id" to "idn_email"),
          SessionVerifyPrepareFirstFactorParams.Case3(PhoneCodeConfig(phoneNumberId = "idn_phone", default = true)) to mapOf("strategy" to "phone_code", "phone_number_id" to "idn_phone", "default" to "true"),
          SessionVerifyPrepareFirstFactorParams.Case4(OmitEnterpriseSSOConfigAndactionCompleteRedirectUrl(emailAddressId = "idn_email", enterpriseConnectionId = "econn_123", redirectUrl = "clerk-test://sso-callback")) to mapOf("strategy" to "enterprise_sso", "email_address_id" to "idn_email", "enterprise_connection_id" to "econn_123", "redirect_url" to "clerk-test://sso-callback")
        )
        for ((params, body) in first) verify(session.prepareFirstFactorVerification(params), SessionVerificationStatus.NeedsFirstFactor, "verify/prepare_first_factor", body)
        val attempts = listOf(
          SessionVerifyAttemptFirstFactorParams.Case1(EmailCodeAttempt("123456")) to mapOf("strategy" to "email_code", "code" to "123456"),
          SessionVerifyAttemptFirstFactorParams.Case2(PhoneCodeAttempt("123456")) to mapOf("strategy" to "phone_code", "code" to "123456"),
          SessionVerifyAttemptFirstFactorParams.Case3(PasswordAttempt("fixture-password")) to mapOf("strategy" to "password", "password" to "fixture-password")
        )
        for ((params, body) in attempts) verify(session.attemptFirstFactorVerification(params), SessionVerificationStatus.Complete, "verify/attempt_first_factor", body)
        verify(session.prepareSecondFactorVerification(PhoneCodeSecondFactorConfig("idn_phone")), SessionVerificationStatus.NeedsSecondFactor, "verify/prepare_second_factor", mapOf("strategy" to "phone_code", "phone_number_id" to "idn_phone"))
        val second = listOf(SessionVerifyAttemptSecondFactorParams.Case1(PhoneCodeAttempt("123456")), SessionVerifyAttemptSecondFactorParams.Case2(TOTPAttempt("123456")), SessionVerifyAttemptSecondFactorParams.Case3(BackupCodeAttempt("123456")))
        for (params in second) verify(session.attemptSecondFactorVerification(params), SessionVerificationStatus.Complete, "verify/attempt_second_factor", mapOf("strategy" to params.strategy, "code" to "123456"))
        failNext = true
        val failed = runCatching { session.attemptFirstFactorVerification(SessionVerifyAttemptFirstFactorParams.Case1(EmailCodeAttempt("123456"))) }.exceptionOrNull()
        check(failed is CoreException && failed.status == 422 && failed.errors.first().code == "form_code_incorrect")
        check(session.attemptFirstFactorVerification(SessionVerifyAttemptFirstFactorParams.Case1(EmailCodeAttempt("123456"))).status == SessionVerificationStatus.Complete)
        check(clerk.session?.id == session.id && paths.size == 16)
      } finally { clerk.close() }
    }
  }
}
