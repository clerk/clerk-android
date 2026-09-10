package com.clerk.api

import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.net.URI
import java.util.Base64
import kotlinx.coroutines.*
import kotlinx.serialization.json.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GoogleIdentityContractTest {
  @Test fun existingAccountRequiresFinalization() = verify("existing")

  @Test fun missingAccountCreatesSignupWithMetadata() = verify("new")

  @Test fun signInOnlyDoesNotCreateAccount() = verify("sign-in-only")

  @Test fun rejectedIdentityDoesNotOpenBrowser() = verify("rejected")

  @Test fun failedSignupPreservesStructuredError() = verify("signup-rejected")

  @Test fun emptyPickerFallsBackToBrowser() = verify("empty-picker")

  @Test fun cancelledPickerDoesNotOpenBrowser() = verify("cancelled")

  @Test fun blankIdentityTokenDoesNotAuthenticate() = verify("blank-token")

  @Test fun resetIgnoresLateIdentityToken() = verify("reset")

  private fun verify(scenario: String) = runBlocking {
    withTimeout(15000) {
      withContext(Dispatchers.Main.immediate) {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val base = PackagedFixtures(instrumentation.context)
        val host = GoogleIdentityHost(base, scenario)
        val key =
          "pk_test_" +
            Base64.getEncoder().encodeToString("native-core.clerk.accounts.dev$".toByteArray())
        val clerk =
          Clerk.connect(
            instrumentation.targetContext,
            ClerkConfiguration(key, "clerk-test://sso-callback"),
            host,
          )
        val metadata = buildJsonObject {
          put("test", "test")
          putJsonObject("nested") { put("active", true) }
        }
        val params =
          MobileSSOParams(
            strategy = SignInSSOParamsStrategy.OauthGoogle,
            start = MobileSSOParamsStart.SignIn,
            transferable = scenario != "sign-in-only",
            preferGoogleOneTap = true,
            unsafeMetadata = metadata,
            locale = "fr",
            legalAccepted = true,
          )
        try {
          if (scenario == "reset") {
            val call = async { runCatching { clerk.authenticateWithSSO(params) } }
            host.opened.await()
            clerk.signIn.reset()
            val error = call.await().exceptionOrNull()
            check(error is CoreException)
            host.release.complete(Unit)
            host.replied.await()
            // Drain queued work after the cancelled host operation finishes.
            clerk.handleAuthCallback(URI("other-app://callback"))
            check(host.requests.isEmpty() && host.browsers == 0 && clerk.session == null)
            return@withContext
          }
          val result = runCatching { clerk.authenticateWithSSO(params) }
          val error = result.exceptionOrNull()
          val expectedCode =
            when (scenario) {
              "sign-in-only" -> "external_account_not_found"
              "rejected" -> "verification_failed"
              "signup-rejected" -> "sign_up_mode_restricted"
              "cancelled" -> "user_cancelled"
              "blank-token" -> "invalid_credential_result"
              else -> null
            }
          if (expectedCode != null) {
            check(error is CoreException) { "Expected structured failure for $scenario: $error" }
            if (scenario in listOf("cancelled", "blank-token")) check(error.code == expectedCode)
            else {
              check(error.status == 422 && error.errors.single().code == expectedCode)
              check(error.errors.single().longMessage == "Choose another sign-in method")
              check(error.clerkTraceId == "google_fixture_trace")
            }
          } else {
            check(error == null) { "Unexpected $scenario failure: $error" }
            val auth = result.getOrThrow()
            if (scenario == "new") {
              check(auth is MobileAuthenticationResult.Case2 && auth.value.signUp === clerk.signUp)
              check(clerk.signUp.status.rawValue == "complete")
            } else {
              check(auth is MobileAuthenticationResult.Case1 && auth.value.signIn === clerk.signIn)
              check(clerk.signIn.status.rawValue == "complete")
            }
          }
          check(clerk.session == null && clerk.user == null)
          check(host.prompts == 1 && host.browsers == if (scenario == "empty-picker") 1 else 0)
          val signups = host.requests.filter { host.path(it).contains("/sign_ups") }
          check(signups.size == if (scenario in listOf("new", "signup-rejected")) 1 else 0)
          if (signups.isNotEmpty()) {
            val body = host.body(signups.single())
            check(body.getQueryParameter("token") == "google-fixture-token")
            check(body.getQueryParameter("strategy") == "google_one_tap")
            check(
              Json.parseToJsonElement(checkNotNull(body.getQueryParameter("unsafe_metadata"))) ==
                metadata
            )
            check(
              body.getQueryParameter("locale") == "fr" &&
                body.getQueryParameter("legal_accepted") == "true"
            )
          }
          if (scenario in listOf("cancelled", "blank-token")) check(host.requests.isEmpty())
          else if (scenario != "empty-picker") {
            val body = host.body(host.requests.first())
            check(body.getQueryParameter("strategy") == "google_one_tap")
            check(body.getQueryParameter("token") == "google-fixture-token")
            check(host.requests.size == if (signups.isEmpty()) 1 else 2)
          } else {
            check(host.requests.none { host.body(it).getQueryParameter("token") != null })
            check(
              host.requests.any { host.body(it).getQueryParameter("strategy") == "oauth_google" }
            )
          }
          if (expectedCode == null) {
            if (scenario == "new") clerk.signUp.finalize() else clerk.signIn.finalize()
            check(clerk.session?.id == "sess_native" && clerk.user?.id == "user_native")
            check(base.requests.count { host.path(it).endsWith("/touch") } == 1)
          }
        } finally {
          host.release.complete(Unit)
          clerk.close()
        }
      }
    }
  }
}

private class GoogleIdentityHost(val base: PackagedFixtures, val scenario: String) :
  NativeCapabilities {
  override val supported = base.supported + "googleIdentity"
  val requests = mutableListOf<JsonObject>()
  val opened = CompletableDeferred<Unit>()
  val release = CompletableDeferred<Unit>()
  val replied = CompletableDeferred<Unit>()
  var prompts = 0
  var browsers = 0

  init {
    val environment = base.fixtures.getValue("environment").jsonObject
    base.environmentResponse =
      JsonObject(
        environment +
          ("display_config" to
            JsonObject(
              environment.getValue("display_config").jsonObject +
                ("google_one_tap_client_id" to JsonPrimitive("configured_google_client"))
            ))
      )
  }

  fun path(args: JsonObject): String = URI(args.getValue("url").requireString()).path

  fun body(args: JsonObject): Uri =
    Uri.parse("https://fixture.invalid/?" + (args["body"]?.jsonPrimitive?.contentOrNull ?: ""))

  override suspend fun perform(capability: String, arguments: JsonElement): JsonElement {
    if (capability == "googleIdentity") {
      prompts++
      check(arguments.jsonObject["clientId"] == JsonPrimitive("configured_google_client"))
      opened.complete(Unit)
      if (scenario == "reset") {
        withContext(NonCancellable) { release.await() }
        replied.complete(Unit)
      }
      if (scenario == "cancelled") throw CoreException("user_cancelled")
      if (scenario == "empty-picker") throw CoreException("google_account_unavailable")
      return buildJsonObject {
        put("token", if (scenario == "blank-token") "  " else "google-fixture-token")
      }
    }
    if (capability == "browser") browsers++
    if (capability != "http") return base.perform(capability, arguments)
    val args = arguments.jsonObject
    val path = path(args)
    if (!path.contains("/sign_ins") && !path.contains("/sign_ups"))
      return base.perform(capability, arguments)
    requests += args
    if (scenario == "empty-picker") return base.perform(capability, arguments)
    val signup = path.contains("/sign_ups")
    val code =
      when {
        signup && scenario == "signup-rejected" -> "sign_up_mode_restricted"
        !signup && scenario in listOf("new", "sign-in-only", "signup-rejected") ->
          "external_account_not_found"
        !signup && scenario == "rejected" -> "verification_failed"
        else -> null
      }
    val document =
      if (code != null)
        buildJsonObject {
          put("clerk_trace_id", "google_fixture_trace")
          putJsonArray("errors") {
            add(
              buildJsonObject {
                put("code", code)
                put("message", "Google authentication rejected")
                put("long_message", "Choose another sign-in method")
              }
            )
          }
        }
      else
        buildJsonObject {
          val resource =
            JsonObject(
              base.fixtures.getValue(if (signup) "signUp" else "signIn").jsonObject +
                mapOf(
                  "status" to JsonPrimitive("complete"),
                  "created_session_id" to JsonPrimitive("sess_native"),
                )
            )
          put("response", resource)
          if (signup)
            put(
              "client",
              JsonObject(
                base.fixtures.getValue("authenticatedClient").jsonObject +
                  mapOf(
                    "sign_up" to resource,
                    "last_active_session_id" to JsonNull,
                  )
              ),
            )
        }
    return buildJsonObject {
      put("status", if (code != null) 422 else 200)
      putJsonObject("headers") { put("x-clerk-trace-id", "google_fixture_trace") }
      put("body", document.toString())
    }
  }
}
