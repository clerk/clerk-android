package com.clerk.api

import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.IOException
import java.util.Base64
import java.util.Locale
import kotlinx.coroutines.*
import kotlinx.serialization.json.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BrowserSSOContractTest {
  @Test fun signInUsesGoogleBrowserStrategyAndConfiguredCallback() = verify("signin-google")

  @Test fun signInPreservesCustomProviderStrategy() = verify("signin-custom")

  @Test fun signUpPreservesMetadataAndBrowserStrategy() = verify("signup-google")

  @Test fun signUpPreservesCustomProviderStrategy() = verify("signup-custom")

  @Test fun enterpriseSignInPreparesConnectionBeforeOpeningBrowser() = verify("enterprise-new")

  @Test
  fun enterpriseSignInRefreshesPreparedRedirectOnExistingAttempt() = verify("enterprise-prepared")

  @Test fun oauthRetryDoesNotReplayStalePreparedRedirect() = verify("oauth-stale")

  @Test
  fun missingRedirectLeavesAnIncompleteAttemptWithoutOpeningBrowser() = verify("missing-redirect")

  @Test fun cancelledSignInDoesNotReconcileOrActivate() = verify("cancel-signin")

  @Test fun cancelledSignUpDoesNotReconcileOrActivate() = verify("cancel-signup")

  @Test fun unrelatedCallbackDoesNotSubmitNonce() = verify("mismatch")

  @Test fun rejectedPreparationPreservesStructuredError() = verify("preparation-rejected")

  @Test
  fun browserTransferCreatesSignUpWithMetadataWithoutSecondPrompt() = verify("transfer-allowed")

  @Test fun disabledBrowserTransferDoesNotCreateSignUp() = verify("transfer-disabled")

  @Test fun signInTransportFailureDoesNotBecomeSuccess() = verify("transport-signin")

  @Test fun signUpTransportFailureDoesNotBecomeSuccess() = verify("transport-signup")

  private fun verify(scenario: String) = runBlocking {
    withTimeout(15000) {
      withContext(Dispatchers.Main.immediate) {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val host = BrowserSSOHost(PackagedFixtures(instrumentation.context), scenario)
        val key =
          "pk_test_" +
            Base64.getEncoder().encodeToString("native-core.clerk.accounts.dev$".toByteArray())
        val clerk =
          Clerk.connect(instrumentation.targetContext, ClerkConfiguration(key, host.callback), host)
        try {
          if (host.seeded)
            clerk.signIn.create(SignInCreateParams(identifier = "engineer@example.com"))
          val before = host.requests.size
          val result = runCatching {
            when {
              scenario.startsWith("transfer-") || scenario == "enterprise-new" -> {
                val auth =
                  clerk.authenticateWithSSO(
                    MobileSSOParams(
                      strategy = host.signInStrategy,
                      start = MobileSSOParamsStart.SignIn,
                      transferable = scenario == "transfer-allowed",
                      identifier = "engineer@example.com",
                      enterpriseConnectionId = if (host.enterprise) "ec_fixture" else null,
                      oidcPrompt = "login consent",
                      unsafeMetadata = host.metadata,
                      legalAccepted = true,
                      locale = "fr",
                    )
                  )
                if (scenario == "transfer-allowed")
                  check(
                    auth is MobileAuthenticationResult.Case2 && auth.value.signUp === clerk.signUp
                  )
                else
                  check(
                    auth is MobileAuthenticationResult.Case1 && auth.value.signIn === clerk.signIn
                  )
              }
              host.signup ->
                clerk.signUp.sso(
                  SignUpSSOParams(
                    strategy = host.strategy,
                    oidcPrompt = "login consent",
                    emailAddress = "engineer@example.com",
                    unsafeMetadata = host.metadata,
                    legalAccepted = true,
                    locale = "fr",
                  )
                )
              else ->
                clerk.signIn.sso(
                  SignInSSOParams(
                    strategy = host.signInStrategy,
                    identifier = "engineer@example.com",
                    oidcPrompt = "login consent",
                    enterpriseConnectionId = if (host.enterprise) "ec_fixture" else null,
                  )
                )
            }
          }
          val error = result.exceptionOrNull()
          when {
            scenario.startsWith("transport-") -> check(error is CoreException)
            scenario.startsWith("cancel-") ->
              check(error is CoreException && error.code == "user_cancelled")
            scenario == "mismatch" ->
              check(error is CoreException && error.code == "oauth_transport_callback_mismatch")
            scenario == "preparation-rejected" -> {
              check(error is CoreException && error.status == 422)
              check(error.errors.single().code == "sign_up_mode_restricted")
              check(error.clerkTraceId == "browser_fixture_trace")
            }
            scenario == "transfer-disabled" -> check(error is CoreException)
            else -> check(error == null) { "Unexpected $scenario failure: $error" }
          }
          val requests = host.requests.drop(before)
          val expectedPaths =
            when {
              scenario == "enterprise-new" ->
                listOf(
                  "/v1/client/sign_ins",
                  "/v1/client/sign_ins/sia_native/prepare_first_factor",
                  "/v1/client/sign_ins/sia_native",
                )
              scenario == "enterprise-prepared" ->
                listOf(
                  "/v1/client/sign_ins/sia_seed/prepare_first_factor",
                  "/v1/client/sign_ins/sia_seed",
                )
              scenario == "transfer-allowed" ->
                listOf(
                  "/v1/client/sign_ins",
                  "/v1/client/sign_ins/sia_native",
                  "/v1/client/sign_ups",
                )
              error != null && scenario != "transfer-disabled" || scenario == "missing-redirect" ->
                listOf("/v1/client/" + if (host.signup) "sign_ups" else "sign_ins")
              else -> {
                val root = if (host.signup) "sign_ups" else "sign_ins"
                val id = if (host.signup) "sua_native" else "sia_native"
                listOf("/v1/client/$root", "/v1/client/$root/$id")
              }
            }
          check(requests.map { host.url(it).path } == expectedPaths) {
            "Unexpected $scenario paths: ${requests.map { host.url(it).path }}"
          }
          val firstBody = host.body(requests.first())
          if (host.url(requests.first()).path!!.endsWith("/sign_ins"))
            check(firstBody.getQueryParameter("locale") == Locale.getDefault().toLanguageTag())
          check(firstBody.getQueryParameter("strategy") == host.strategy)
          check(firstBody.getQueryParameter("redirect_url") == host.callback)
          check(firstBody.getQueryParameter("action_complete_redirect_url") == host.callback)
          check(firstBody.getQueryParameter("oidc_prompt") == "login consent")
          if (host.enterprise) {
            val prepare = requests.single { host.url(it).path!!.endsWith("/prepare_first_factor") }
            check(host.body(prepare).getQueryParameter("enterprise_connection_id") == "ec_fixture")
          }
          val signups = requests.filter { host.url(it).path!!.endsWith("/sign_ups") }
          if (signups.isNotEmpty()) {
            val body = host.body(signups.single())
            check(
              Json.parseToJsonElement(checkNotNull(body.getQueryParameter("unsafe_metadata"))) ==
                host.metadata
            )
            check(
              body.getQueryParameter("legal_accepted") == "true" &&
                body.getQueryParameter("locale") == "fr"
            )
            if (scenario == "transfer-allowed") {
              check(body.getQueryParameter("transfer") == "true")
              check(body.getQueryParameter("strategy") == null)
            } else check(body.getQueryParameter("email_address") == "engineer@example.com")
          }
          check(
            host.opened ==
              if (
                scenario in listOf("missing-redirect", "preparation-rejected") ||
                  scenario.startsWith("transport-")
              )
                emptyList<String>()
              else listOf(host.freshRedirect)
          )
          check(clerk.session == null && clerk.user == null)
          if (error == null && scenario != "missing-redirect") {
            if (host.signup || scenario == "transfer-allowed")
              check(
                clerk.signUp.status.rawValue == "complete" &&
                  clerk.signUp.createdSessionId == "sess_native"
              )
            else
              check(
                clerk.signIn.status.rawValue == "complete" &&
                  clerk.signIn.createdSessionId == "sess_native"
              )
          }
          if (scenario == "missing-redirect")
            check(clerk.signIn.status.rawValue == "needs_first_factor")
          if (scenario == "transfer-disabled")
            check(clerk.signIn.isTransferable && signups.isEmpty())
          if (scenario == "enterprise-prepared") check(clerk.signIn.id == "sia_seed")
          if (scenario == "oauth-stale") check(clerk.signIn.id == "sia_native")
        } finally {
          clerk.close()
        }
      }
    }
  }
}

private class BrowserSSOHost(val base: PackagedFixtures, val scenario: String) :
  NativeCapabilities {
  override val supported = base.supported
  val signup = scenario.contains("signup") || scenario == "preparation-rejected"
  val enterprise = scenario.startsWith("enterprise-")
  val seeded = scenario in listOf("enterprise-prepared", "oauth-stale")
  val strategy =
    when {
      enterprise -> "enterprise_sso"
      scenario.endsWith("custom") -> "oauth_custom_patreon"
      else -> "oauth_google"
    }
  val signInStrategy =
    when {
      enterprise -> SignInSSOParamsStrategy.EnterpriseSso
      strategy == "oauth_google" -> SignInSSOParamsStrategy.OauthGoogle
      else -> SignInSSOParamsStrategy.Unrecognized(strategy)
    }
  val callback = "clerk-test://sso-callback"
  val freshRedirect = "https://provider.example/fresh-authorize"
  val metadata = buildJsonObject {
    put("test", "test")
    putJsonObject("nested") { put("enabled", true) }
  }
  val requests = mutableListOf<JsonObject>()
  val opened = mutableListOf<String>()

  fun url(args: JsonObject): Uri = Uri.parse(args.getValue("url").requireString())

  fun body(args: JsonObject): Uri =
    Uri.parse("https://fixture.test/?" + (args["body"] as? JsonPrimitive)?.content.orEmpty())

  override suspend fun perform(capability: String, arguments: JsonElement): JsonElement {
    val args = arguments.jsonObject
    if (capability == "browser") {
      opened += args.getValue("url").requireString()
      check(args["callbackUrl"] == JsonPrimitive(callback))
      if (scenario.startsWith("cancel-")) throw CoreException("user_cancelled")
      return buildJsonObject {
        put(
          "callbackUrl",
          if (scenario == "mismatch") "unrelated-app://callback?rotating_token_nonce=wrong"
          else "$callback?rotating_token_nonce=native_nonce",
        )
      }
    }
    if (capability != "http") return base.perform(capability, arguments)
    val url = url(args)
    if (!url.path!!.contains("/sign_ins") && !url.path!!.contains("/sign_ups"))
      return base.perform(capability, arguments)
    requests += args
    if (scenario.startsWith("transport-")) throw IOException("Fixture transport unavailable")
    val isSignup = url.path!!.contains("/sign_ups")
    val get = args["method"] == JsonPrimitive("GET")
    check(get || args["method"] == JsonPrimitive("POST"))
    if (get) check(url.getQueryParameter("rotating_token_nonce") == "native_nonce")
    if (scenario == "preparation-rejected")
      return buildJsonObject {
        put("status", 422)
        put("headers", buildJsonObject {})
        put(
          "body",
          """{"errors":[{"code":"sign_up_mode_restricted","message":"Sign-up unavailable"}],"clerk_trace_id":"browser_fixture_trace"}""",
        )
      }
    val seed = seeded && !get && body(args).getQueryParameter("strategy") == null
    val id =
      if (isSignup) "sua_native"
      else if (seed || scenario == "enterprise-prepared") "sia_seed" else "sia_native"
    val transferred = isSignup && body(args).getQueryParameter("transfer") == "true"
    val transferable = !isSignup && get && scenario.startsWith("transfer-")
    val resource =
      base.fixtures.getValue(if (isSignup) "signUp" else "signIn").jsonObject.toMutableMap()
    resource["id"] = JsonPrimitive(id)
    if (get || transferred) {
      resource["status"] = JsonPrimitive(if (transferable) "needs_identifier" else "complete")
      resource["created_session_id"] = if (transferable) JsonNull else JsonPrimitive("sess_native")
    }
    val originalVerification =
      if (isSignup)
        resource.getValue("verifications").jsonObject.getValue("external_account").jsonObject
      else resource.getValue("first_factor_verification").jsonObject
    val verification =
      JsonObject(
        originalVerification +
          mapOf(
            "strategy" to JsonPrimitive(strategy),
            "status" to JsonPrimitive(if (transferable) "transferable" else "unverified"),
            "external_verification_redirect_url" to
              if (scenario == "missing-redirect") JsonNull
              else
                JsonPrimitive(
                  if (seed) "https://provider.example/stale-authorize" else freshRedirect
                ),
            "error" to
              if (transferable)
                Json.parseToJsonElement(
                  """{"code":"external_account_not_found","message":"Account does not exist"}"""
                )
              else JsonNull,
          )
      )
    if (isSignup) {
      resource["unsafe_metadata"] = metadata
      resource["verifications"] =
        JsonObject(
          resource.getValue("verifications").jsonObject + ("external_account" to verification)
        )
    } else resource["first_factor_verification"] = verification
    return buildJsonObject {
      put("status", 200)
      put("headers", buildJsonObject { put("authorization", "browser-client-credential") })
      put("body", buildJsonObject { put("response", JsonObject(resource)) }.toString())
    }
  }
}
