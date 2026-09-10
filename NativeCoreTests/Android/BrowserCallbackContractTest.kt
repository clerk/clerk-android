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
class BrowserCallbackContractTest {
  @Test fun missingSuccessMarkerReloadsRemainingServerRequirements() = verify("")

  @Test
  fun providerQueryErrorDoesNotReplaceServerState() =
    verify("?error=access_denied&error_description=Cancelled")

  @Test
  fun transferMarkerAloneCannotCreateSignUp() =
    verify("?__clerk_status=failed&__clerk_error_code=external_account_not_found")

  @Test
  fun nativeCallbackFailurePreservesErrorAndReconciledState() =
    verify("?__clerk_status=failed&__clerk_error_code=oauth_access_denied")

  @Test fun signInPreparationPreservesStructuredFailure() = verify("", rejectPreparation = true)

  private fun verify(suffix: String, rejectPreparation: Boolean = false) = runBlocking {
    withTimeout(15000) {
      withContext(Dispatchers.Main.immediate) {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val base = PackagedFixtures(instrumentation.context)
        val callback = "clerk-test://sso-callback"
        val requests = mutableListOf<JsonObject>()
        var opened = 0
        val host =
          object : NativeCapabilities {
            override val supported = base.supported

            override suspend fun perform(capability: String, arguments: JsonElement): JsonElement {
              val args = arguments.jsonObject
              if (capability == "browser") {
                opened++
                check(args["callbackUrl"] == JsonPrimitive(callback))
                return buildJsonObject { put("callbackUrl", callback + suffix) }
              }
              if (capability != "http") return base.perform(capability, arguments)
              val url = Uri.parse(args.getValue("url").requireString())
              if (!url.path!!.contains("/sign_ins") && !url.path!!.contains("/sign_ups"))
                return base.perform(capability, arguments)
              requests += args
              check(!url.path!!.contains("/sign_ups")) {
                "Callback query must not create a sign-up"
              }
              if (rejectPreparation)
                return buildJsonObject {
                  put("status", 422)
                  put("headers", buildJsonObject {})
                  put(
                    "body",
                    """{"errors":[{"code":"form_identifier_not_found","message":"Account unavailable"}],"clerk_trace_id":"browser_host_trace"}""",
                  )
                }
              val reload = args["method"] == JsonPrimitive("GET")
              val original = base.fixtures.getValue("signIn").jsonObject
              val verification =
                JsonObject(
                  original.getValue("first_factor_verification").jsonObject +
                    mapOf(
                      "status" to JsonPrimitive(if (reload) "verified" else "unverified"),
                      "strategy" to JsonPrimitive("oauth_google"),
                      "error" to JsonNull,
                      "external_verification_redirect_url" to
                        JsonPrimitive("https://provider.example/authorize"),
                    )
                )
              val resource =
                JsonObject(
                  original +
                    mapOf(
                      "status" to
                        JsonPrimitive(if (reload) "needs_second_factor" else "needs_first_factor"),
                      "first_factor_verification" to verification,
                      "created_session_id" to JsonNull,
                    )
                )
              return buildJsonObject {
                put("status", 200)
                put("headers", buildJsonObject {})
                put("body", buildJsonObject { put("response", resource) }.toString())
              }
            }
          }
        val key =
          "pk_test_" +
            Base64.getEncoder().encodeToString("native-core.clerk.accounts.dev$".toByteArray())
        val clerk =
          Clerk.connect(instrumentation.targetContext, ClerkConfiguration(key, callback), host)
        try {
          val result = runCatching {
            clerk.authenticateWithSSO(
              MobileSSOParams(
                strategy = SignInSSOParamsStrategy.OauthGoogle,
                start = MobileSSOParamsStart.SignIn,
                transferable = true,
              )
            )
          }
          if (rejectPreparation) {
            val failure = result.exceptionOrNull() as? CoreException
            check(
              failure?.status == 422 && failure.errors.single().code == "form_identifier_not_found"
            )
            check(failure.clerkTraceId == "browser_host_trace")
            check(
              opened == 0 &&
                requests.size == 1 &&
                requests.single()["method"] == JsonPrimitive("POST")
            )
            check(
              Uri.parse(requests.single().getValue("url").requireString()).path ==
                "/v1/client/sign_ins"
            )
            check(clerk.session == null && clerk.user == null)
            return@withContext
          }
          if (suffix.contains("__clerk_error_code=oauth_access_denied"))
            check((result.exceptionOrNull() as? CoreException)?.code == "oauth_access_denied")
          else {
            val auth = result.getOrThrow()
            check(auth is MobileAuthenticationResult.Case1 && auth.value.signIn === clerk.signIn)
          }
          check(opened == 1)
          check(
            requests.map { Uri.parse(it.getValue("url").requireString()).path } ==
              listOf("/v1/client/sign_ins", "/v1/client/sign_ins/sia_native")
          )
          check(requests.last()["method"] == JsonPrimitive("GET"))
          check(
            Uri.parse(requests.last().getValue("url").requireString())
              .getQueryParameter("rotating_token_nonce") == null
          )
          check(
            clerk.signIn.status.rawValue == "needs_second_factor" && !clerk.signIn.isTransferable
          )
          check(
            clerk.signIn.createdSessionId == null && clerk.session == null && clerk.user == null
          )
        } finally {
          clerk.close()
        }
      }
    }
  }
}
