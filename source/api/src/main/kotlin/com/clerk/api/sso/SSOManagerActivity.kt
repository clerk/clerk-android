package com.clerk.api.sso

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.net.toUri
import androidx.lifecycle.lifecycleScope
import com.clerk.api.Constants.Storage.KEY_AUTHORIZATION_STARTED
import com.clerk.api.hostedauth.HostedAuthService
import com.clerk.api.log.ClerkLog
import com.clerk.api.log.SafeUriLog
import com.clerk.api.magiclink.NativeMagicLinkService
import com.clerk.api.magiclink.canHandleNativeMagicLink
import com.clerk.api.network.serialization.ClerkResult
import kotlinx.coroutines.launch

/**
 * Activity that manages the OAuth/SSO browser flow. This activity is responsible for launching the
 * CustomTabs browser session and handling its lifecycle.
 *
 * The authentication flow follows these steps:
 * 1. Activity receives the authentication URL in its extras
 * 2. On first resume, launches CustomTabs browser with the authentication URL
 * 3. When the browser returns (either via success or cancellation), processes the result
 * 4. Completes the authentication flow and finishes itself
 *
 * This activity is designed to handle configuration changes and process death, preserving the
 * authentication state throughout the flow.
 */
internal class SSOManagerActivity : AppCompatActivity() {
  /** Which completion, if any, has claimed the pending callback. Survives process death. */
  private enum class Completion {
    NONE,
    SSO,
    HOSTED_AUTH,
  }

  private var authorizationStarted = false
  private var completion = Completion.NONE
  private var completionObserverAttached = false
  private lateinit var desiredUri: Uri
  private var pendingCallbackUri: Uri? = null

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    if (savedInstanceState == null) {
      hydrateState(intent.extras)
    } else {
      hydrateState(savedInstanceState)
    }
  }

  override fun onResume() {
    super.onResume()
    if (resumeCallbackIfPresent()) return

    // on first run, launch the intent to start the OAuth/SSO flow in the browser
    if (!authorizationStarted) {
      try {
        ClerkLog.d("Launching custom tab with uri: ${SafeUriLog.describe(desiredUri)}")
        CustomTabsIntent.Builder().build().launchUrl(this, desiredUri)
        authorizationStarted = true
      } catch (_: UninitializedPropertyAccessException) {
        authorizationFailed()
        finish()
      } catch (_: ActivityNotFoundException) {
        authorizationFailed()
        finish()
      }
      return
    }
    // subsequent runs, we either got the response back from OAuthReceiverActivity or it was
    // cancelled. If we have a response, complete the flow and only finish after completion to
    // avoid cancelling the in-flight network request.
    intent.data?.let {
      if (completion == Completion.NONE) {
        completion = Completion.SSO
        pendingCallbackUri = it
        intent = Intent(intent).apply { data = null }
        authorizationComplete(it)
      }
      // Do not call finish() here; authorizationComplete will finish when done
    }
      ?: run {
        authorizationFailed()
        finish()
      }
  }

  private fun resumeCallbackIfPresent(): Boolean {
    val callbackUri = pendingCallbackUri ?: intent.data?.takeIf(::isCallbackUri)
    // Hosted auth completion re-attaches after activity recreation because
    // HostedAuthService.complete() idempotently re-joins the pending flow; SSO completion is a
    // one-shot network call that must never re-run.
    val shouldAttachObserver = !completionObserverAttached && completion != Completion.SSO
    if (callbackUri != null && shouldAttachObserver) {
      if (completion == Completion.NONE) {
        authorizationStarted = true
        completion =
          if (HostedAuthService.canHandle(callbackUri)) Completion.HOSTED_AUTH else Completion.SSO
        pendingCallbackUri = callbackUri
        intent = Intent(intent).apply { data = null }
      }
      completionObserverAttached = true
      authorizationComplete(callbackUri)
    }
    return callbackUri != null
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    if (intent.data?.let(::isCallbackUri) != true) {
      authorizationStarted = false
      completion = Completion.NONE
      completionObserverAttached = false
      pendingCallbackUri = null
      hydrateState(intent.extras)
    }
    setIntent(intent)
  }

  override fun onSaveInstanceState(outState: Bundle) {
    super.onSaveInstanceState(outState)
    outState.putBoolean(KEY_AUTHORIZATION_STARTED, authorizationStarted)
    outState.putString(KEY_COMPLETION_KIND, completion.name)
    outState.putString(KEY_PENDING_CALLBACK_URI, pendingCallbackUri?.toString())
    if (::desiredUri.isInitialized) {
      outState.putString(URI_KEY, desiredUri.toString())
    }
  }

  /**
   * Restores the activity state from a bundle. This is called both for new instances (from intent
   * extras) and restored instances (from saved instance state).
   *
   * @param state Bundle containing the activity state
   */
  private fun hydrateState(state: Bundle?) {
    if (state == null) return finish()
    authorizationStarted = state.getBoolean(KEY_AUTHORIZATION_STARTED, false)
    completion =
      state.getString(KEY_COMPLETION_KIND)?.let { name ->
        Completion.entries.firstOrNull { it.name == name }
      } ?: Completion.NONE
    state.getString(URI_KEY)?.let { desiredUri = it.toUri() }
    pendingCallbackUri = state.getString(KEY_PENDING_CALLBACK_URI)?.toUri()
  }

  /**
   * Handles successful authentication callback by completing the authentication flow.
   *
   * @param uri The callback URI containing authentication results
   */
  private fun authorizationComplete(uri: Uri) {
    lifecycleScope.launch {
      try {
        if (canHandleNativeMagicLink(uri)) {
          ClerkLog.d("authorizationComplete called with native magic link redirect: $uri")
          when (NativeMagicLinkService.handleMagicLinkDeepLink(uri)) {
            is com.clerk.api.network.serialization.ClerkResult.Success -> {
              ClerkLog.i("event=native_magic_link_activity_completion_success")
              pendingCallbackUri = null
              setResult(RESULT_OK, Intent())
            }
            is com.clerk.api.network.serialization.ClerkResult.Failure -> {
              ClerkLog.w("event=native_magic_link_activity_completion_failure")
              setResult(RESULT_CANCELED, Intent())
            }
          }
          return@launch
        }
        val hostedAuthResult = HostedAuthService.complete(uri)
        if (completion == Completion.HOSTED_AUTH || hostedAuthResult != null) {
          ClerkLog.d("authorizationComplete called with hosted auth redirect")
          pendingCallbackUri = null
          when (hostedAuthResult) {
            is ClerkResult.Success -> setResult(RESULT_OK, Intent())
            is ClerkResult.Failure,
            null -> setResult(RESULT_CANCELED, Intent())
          }
          return@launch
        }
        if (SSOService.hasPendingExternalAccountConnection()) {
          ClerkLog.d("authorizationComplete called with external connection")
          SSOService.completeExternalConnection()
        } else {
          ClerkLog.d("authorizationComplete called with redirect: $uri")
          SSOService.completeAuthenticateWithRedirect(uri)
        }
        pendingCallbackUri = null
        setResult(RESULT_OK, Intent())
      } catch (t: Throwable) {
        ClerkLog.e("authorizationComplete failed: ${t.message}")
        setResult(RESULT_CANCELED, Intent())
      } finally {
        finish()
      }
    }
  }

  private fun isCallbackUri(uri: Uri): Boolean {
    return HostedAuthService.canHandle(uri) ||
      uri.scheme?.startsWith("clerk") == true ||
      canHandleNativeMagicLink(uri) ||
      uri.getQueryParameter("rotating_token_nonce") != null
  }

  /** Finishes an authentication attempt that could not be completed. */
  private fun authorizationFailed() {
    HostedAuthService.cancelPendingAuthentication()
    val response = Intent()
    setResult(RESULT_CANCELED, response)
  }

  internal companion object {
    /**
     * Creates an intent to handle the OAuth/SSO response.
     *
     * @param context The context to create the intent from
     * @param responseUri The URI received from the authentication provider
     * @return Intent configured to handle the authentication response
     */
    internal fun createResponseHandlingIntent(context: Context, responseUri: Uri?): Intent {
      val intent = createBaseIntent(context)
      intent.data = responseUri
      responseUri?.let { intent.putExtra(URI_KEY, it.toString()) }
      intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
      return intent
    }

    internal fun createAuthorizationIntent(context: Context, authorizationUri: Uri): Intent =
      createBaseIntent(context).apply { putExtra(URI_KEY, authorizationUri.toString()) }

    /**
     * Creates a base intent for the SSO manager activity.
     *
     * @param context The context to create the intent from
     * @return Basic intent for the SSO manager activity
     */
    internal fun createBaseIntent(context: Context): Intent =
      Intent(context, SSOManagerActivity::class.java)

    internal const val URI_KEY = "uri"
    internal const val KEY_COMPLETION_KIND = "completion_kind"
    internal const val KEY_PENDING_CALLBACK_URI = "pending_callback_uri"
  }
}
