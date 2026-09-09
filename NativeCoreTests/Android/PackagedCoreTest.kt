package com.clerk.api

import android.content.Context
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.net.URI
import java.util.Base64
import java.security.MessageDigest
import kotlinx.coroutines.*
import kotlinx.serialization.json.*
import org.junit.Test
import org.junit.runner.RunWith

internal class PackagedFixtures(context: Context) : NativeCapabilities {
  override val supported = setOf("http", "storage", "timer", "random", "browser", "passkeys", "authStorage", "crypto.sha256", "biometrics")
  private val fixtures = Json.parseToJsonElement(context.assets.open("fapi.json").bufferedReader().use { it.readText() }).jsonObject
  var biometricRecords: String? = null
  var biometricCleanup: String? = null
  var biometricSignCount = 0
  var authRecord: String? = null
  var credential: String? = null
  var signedOut = false
  var nextAuthError: JsonElement? = null
  var clientReads = 0
  val requests = mutableListOf<JsonObject>()
  override suspend fun perform(capability: String, arguments: JsonElement): JsonElement {
    val args = arguments.jsonObject
    when (capability) {
      "biometrics.appIdentifier" -> return JsonPrimitive("com.example.native")
      "biometrics.storage.read" -> return (if (args["key"] == JsonPrimitive("credentials")) biometricRecords else biometricCleanup)?.let(::JsonPrimitive) ?: JsonNull
      "biometrics.storage.write" -> {
        if (args["key"] == JsonPrimitive("credentials")) biometricRecords = args.getValue("value").requireString()
        else biometricCleanup = args.getValue("value").requireString()
        return JsonNull
      }
      "biometrics.supports", "biometrics.hasKey" -> return JsonPrimitive(true)
      "biometrics.deleteKey" -> return JsonNull
      "biometrics.createKey" -> return buildJsonObject { put("localKeyId", "tdlk_native"); put("publicKeyJwk", "{\"kty\":\"EC\"}") }
      "biometrics.sign" -> {
        biometricSignCount++
        check(args["clientData"] == JsonPrimitive("native_fixture_client_data"))
        return buildJsonObject { put("clientData", "native_fixture_client_data"); put("signature", "native_fixture_signature"); put("algorithm", "ES256") }
      }
      "authStorage.read" -> return authRecord?.let(::JsonPrimitive) ?: JsonNull
      "authStorage.write" -> { authRecord = args.getValue("value").requireString(); return JsonNull }
      "authStorage.remove" -> { authRecord = null; return JsonNull }
      "crypto.sha256" -> return JsonPrimitive(Base64.getUrlEncoder().withoutPadding().encodeToString(MessageDigest.getInstance("SHA-256").digest(args.getValue("value").requireString().toByteArray())))
      "storage.read" -> return credential?.let(::JsonPrimitive) ?: JsonNull
      "storage.write" -> { credential = args.getValue("value").requireString(); return JsonNull }
      "storage.remove" -> { credential = null; return JsonNull }
      "timer" -> { delay(args.getValue("milliseconds").jsonPrimitive.long); return JsonNull }
      "browser" -> return buildJsonObject { put("callbackUrl", "clerk-test://sso-callback?rotating_token_nonce=native_nonce") }
    }
    check(capability == "http")
    requests += args
    val url = URI(args.getValue("url").requireString())
    if (url.path.contains("sign_ins")) nextAuthError?.let { failure ->
      nextAuthError = null
      return buildJsonObject { put("status", 422); put("headers", buildJsonObject {}); put("body", failure.toString()) }
    }
    var raw = false
    var client: JsonElement? = null
    val response = when {
      url.path.endsWith("/magic_links/complete") -> JsonObject(fixtures.getValue("signUp").jsonObject + mapOf("status" to JsonPrimitive("complete"), "created_session_id" to JsonPrimitive("sess_native")))
      url.path.endsWith("/environment") -> {
        val environment = fixtures.getValue("environment").jsonObject
        JsonObject(environment + ("auth_config" to JsonObject(environment.getValue("auth_config").jsonObject + ("native_settings" to buildJsonObject { put("api_enabled", true); put("trusted_device_sign_in_enabled", true) }))))
      }
      url.path.endsWith("/biometric_credentials/prepare") -> biometricChallenge
      url.path.endsWith("/biometric_credentials/attempt") -> buildJsonObject {
        put("id", "td_native"); put("object", "trusted_device"); put("platform", "android"); put("app_identifier", "com.example.native"); put("name", JsonNull); put("algorithm", "ES256"); put("status", "active"); put("created_at", System.currentTimeMillis()); put("updated_at", System.currentTimeMillis()); put("last_used_at", JsonNull); put("revoked_at", JsonNull)
      }
      url.path.endsWith("/client") -> fixtures.getValue(if (++clientReads > 1 && !signedOut) "authenticatedClient" else "client")
      url.path.endsWith("/sessions") -> { signedOut = true; fixtures.getValue("client") }
      url.path.endsWith("/tokens") -> { raw = true; fixtures.getValue("token") }
      url.path.endsWith("/touch") -> { client = fixtures.getValue("authenticatedClient"); fixtures.getValue("session") }
      url.path.contains("/phone_numbers") -> {
        val phone = Json.parseToJsonElement("""{"object":"phone_number","id":"phone_native","phone_number":"+15555550123","reserved_for_second_factor":false,"default_second_factor":false,"linked_to":[],"verification":{"status":"verified","strategy":"phone_code","attempts":null,"expire_at":null,"error":null,"verified_at_client":null}}""").jsonObject.toMutableMap()
        if (url.path.endsWith("phone_native")) {
          phone["reserved_for_second_factor"] = JsonPrimitive(true)
          phone["backup_codes"] = JsonArray(listOf(JsonPrimitive("fixture-recovery-code")))
        }
        JsonObject(phone)
      }
      else -> {
        val resource = fixtures.getValue(if (url.path.contains("sign_ins")) "signIn" else "signUp").jsonObject.toMutableMap()
        if (args["method"] == JsonPrimitive("GET")) {
          check(url.rawQuery.contains("rotating_token_nonce=native_nonce"))
          resource["status"] = JsonPrimitive("complete")
          resource["created_session_id"] = JsonPrimitive("sess_native")
        }
        if (args["body"]?.jsonPrimitive?.content?.contains("trusted_device") == true) {
          if (url.path.endsWith("/attempt_first_factor")) {
            check(args.getValue("body").requireString().contains("native_fixture_signature"))
            resource["status"] = JsonPrimitive("complete"); resource["created_session_id"] = JsonPrimitive("sess_native")
          } else resource["first_factor_verification"] = JsonObject(resource.getValue("first_factor_verification").jsonObject + ("trusted_device_challenge" to biometricChallenge))
        }
        JsonObject(resource)
      }
    }
    val body = if (raw) response else buildJsonObject { put("response", response); client?.let { put("client", it) } }
    return buildJsonObject {
      put("status", 200)
      put("headers", buildJsonObject { put("authorization", "fixture-client-credential") })
      put("body", body.toString())
    }
  }
  private val biometricChallenge get() = buildJsonObject {
    put("object", "trusted_device_challenge"); put("challenge", "native_fixture_nonce"); put("challenge_id", "tdc_native"); put("trusted_device_id", "td_native"); put("client_data", "native_fixture_client_data"); put("expires_at", System.currentTimeMillis() + 600000); put("algorithm", "ES256")
  }

}

@RunWith(AndroidJUnit4::class)
class PackagedCoreTest {
  @Test fun previousNativeEmailLinkCallbackFormsCompleteWithoutActivation() = runBlocking {
    withTimeout(30000) {
      val instrumentation = InstrumentationRegistry.getInstrumentation()
      val capabilities = PackagedFixtures(instrumentation.context)
      val key = "pk_test_" + Base64.getEncoder().encodeToString("native-core.clerk.accounts.dev$".toByteArray())
      val clerk = Clerk.connect(instrumentation.targetContext, ClerkConfiguration(key, "clerk-test://sso-callback"), capabilities)
      try {
        clerk.signUp.create(SignUpCreateParams(emailAddress = "test@example.com"))
        clerk.signUp.verifications.sendEmailLink(SignUpEmailLinkSendParams())
        val result = clerk.handleAuthCallback(URI("clerk-test://sso-callback/#flow_id=sua_native&approval_token=fixture_approval"))
        check(result is MobileAuthenticationResult.Case2 && result.value.signUp === clerk.signUp)
        check(clerk.signUp.status == SignUpStatus.Complete && clerk.session == null)
        check(capabilities.authRecord == null)
      } finally { clerk.close() }
    }
  }

  @Test fun generatedApiUsesPackagedCore() = runBlocking {
    withTimeout(30000) {
      val instrumentation = InstrumentationRegistry.getInstrumentation()
      val capabilities = PackagedFixtures(instrumentation.context)
      val key = "pk_test_" + Base64.getEncoder().encodeToString("native-core.clerk.accounts.dev$".toByteArray())
      val clerk = Clerk.connect(instrumentation.targetContext, ClerkConfiguration(key, "clerk-test://sso-callback"), capabilities)
      try {
        capabilities.nextAuthError = Json.parseToJsonElement("""{"errors":[{"code":"form_identifier_not_found","message":"Account not found","long_message":"No account was found for this identifier.","meta":{"param_name":"identifier"}}]}""")
        try {
          clerk.signIn.create(SignInCreateParams(identifier = "missing@example.com"))
          error("Expected a structured Clerk error")
        } catch (error: CoreException) {
          check(error.errors.first().code == "form_identifier_not_found")
          check(error.errors.first().meta?.paramName == "identifier")
          check(error.localizedMessage == "No account was found for this identifier.")
        }
        capabilities.nextAuthError = Json.parseToJsonElement("""{"errors":[{"code":"passkey_verification_failed","message":"Credential rejected"}]}""")
        try {
          clerk.signIn.passkey(SignInPasskeyParams(flow = SignInPasskeyParamsFlow.Discoverable))
          error("Expected passkey preparation failure")
        } catch (error: CoreException) {
          check(error.passkeyStage == "preparingFirstFactor" && error.errors.first().code == "passkey_verification_failed")
        }
        clerk.signUp.create(SignUpCreateParams(emailAddress = "test@example.com"))
        clerk.signUp.verifications.sendEmailLink(SignUpEmailLinkSendParams())
        check(capabilities.authRecord != null)
        val emailResult = clerk.handleAuthCallback(URI("clerk-test://sso-callback?flow_id=sua_native&approval_token=fixture_approval"))
        check(emailResult is MobileAuthenticationResult.Case2 && emailResult.value.signUp === clerk.signUp)
        check(clerk.signUp.status.rawValue == "complete" && clerk.session == null && capabilities.authRecord == null)
        clerk.clearAuthCallback(clerk.authCallback!!.id)
        check(clerk.authCallback == null)
        clerk.signUp.reset()
        clerk.signIn.sso(SignInSSOParams(SignInSSOParamsStrategy.OauthGoogle))
        check(clerk.signIn.status.rawValue == "complete" && clerk.session == null)
        clerk.signUp.sso(SignUpSSOParams("oauth_google"))
        check(clerk.signUp.status.rawValue == "complete" && clerk.session == null)
        val sharedFlow = clerk.authenticateWithSSO(MobileSSOParams(
          strategy = SignInSSOParamsStrategy.OauthGoogle,
          start = MobileSSOParamsStart.SignIn,
          transferable = false,
        ))
        check(sharedFlow is MobileAuthenticationResult.Case1 && sharedFlow.value.signIn === clerk.signIn)
        check(clerk.signIn.status.rawValue == "complete" && clerk.session == null)
        val group = clerk.signIn.emailCode
        val requestCount = capabilities.requests.size
        clerk.signIn.reset()
        check(group.isInvalidated && capabilities.requests.size == requestCount)
        try { group.verifyCode(SignInEmailCodeVerifyParams("123456")); error("Stale group accepted") }
        catch (error: CoreException) { check(error.code == "stale_resource") }
        clerk.signIn.sso(SignInSSOParams(SignInSSOParamsStrategy.OauthGoogle))
        clerk.signIn.finalize()
        check(clerk.session?.status?.rawValue == "active" && clerk.user?.id == "user_native")
        check(clerk.session?.getToken()?.contains("fixture_signature") == true)
        val phone = clerk.user!!.createPhoneNumber(CreatePhoneNumberParams("+15555550123"))
        check(phone.backupCodes() == null)
        val reservedPhone = phone.setReservedForSecondFactor(SetReservedForSecondFactorParams(true))
        check(reservedPhone === phone && phone.reservedForSecondFactor)
        check(phone.backupCodes() == listOf("fixture-recovery-code"))
        check(clerk.biometricCredentials.canEnroll)
        val biometric = clerk.biometricCredentials.enroll(BiometricCredentialEnrollmentParams(identifierHint = "test@example.com"))
        check(biometric.id == "td_native" && capabilities.biometricRecords != null)
        clerk.signOut()
        check(clerk.session == null && clerk.user == null)
        clerk.signIn.biometricCredential()
        check(clerk.signIn.status.rawValue == "complete" && clerk.session == null && capabilities.biometricSignCount == 2)
      } finally { withContext(Dispatchers.Main) { clerk.close() } }
    }
  }
}
