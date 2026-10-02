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
import com.clerk.api.log.ClerkLog
import com.clerk.api.log.SafeUriLog
import com.clerk.api.redirect.CallbackOutcome
import com.clerk.api.redirect.RedirectCoordinator
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

internal class SSOManagerActivity : AppCompatActivity() {
  private var authorizationStarted = false
  private var completionAttached = false
  private lateinit var desiredUri: Uri

  /** The callback being completed. Saved so a recreated activity re-attaches to the completion. */
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
    val callbackUri = pendingCallbackUri ?: intent.data?.takeIf(::isCallbackUri)
    if (callbackUri != null) {
      // The completion runs in RedirectCoordinator's process-wide scope and is idempotent, so a
      // recreated activity simply attaches to it again.
      if (!completionAttached) {
        authorizationStarted = true
        completionAttached = true
        pendingCallbackUri = callbackUri
        intent = Intent(intent).apply { data = null }
        authorizationComplete(callbackUri)
      }
      return
    }

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
    // The browser came back without a callback: the user dismissed it.
    authorizationFailed()
    finish()
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    if (intent.data?.let(::isCallbackUri) != true) {
      authorizationStarted = false
      completionAttached = false
      pendingCallbackUri = null
      hydrateState(intent.extras)
    }
    setIntent(intent)
  }

  override fun onSaveInstanceState(outState: Bundle) {
    super.onSaveInstanceState(outState)
    outState.putBoolean(KEY_AUTHORIZATION_STARTED, authorizationStarted)
    outState.putString(KEY_PENDING_CALLBACK_URI, pendingCallbackUri?.toString())
    if (::desiredUri.isInitialized) {
      outState.putString(URI_KEY, desiredUri.toString())
    }
  }

  private fun hydrateState(state: Bundle?) {
    if (state == null) return finish()
    authorizationStarted = state.getBoolean(KEY_AUTHORIZATION_STARTED, false)
    state.getString(URI_KEY)?.let { desiredUri = it.toUri() }
    pendingCallbackUri = state.getString(KEY_PENDING_CALLBACK_URI)?.toUri()
  }

  private fun authorizationComplete(uri: Uri) {
    lifecycleScope.launch {
      try {
        val outcome = RedirectCoordinator.dispatch(uri)
        pendingCallbackUri = null
        val succeeded = outcome is CallbackOutcome.Completed && outcome.success
        setResult(if (succeeded) RESULT_OK else RESULT_CANCELED, Intent())
      } catch (cancellation: CancellationException) {
        // This activity is going away; the completion keeps running and a recreated activity
        // attaches to it again.
        throw cancellation
      } catch (t: Throwable) {
        ClerkLog.e("authorizationComplete failed: ${t.message}")
        setResult(RESULT_CANCELED, Intent())
      } finally {
        finish()
      }
    }
  }

  private fun authorizationFailed() {
    RedirectCoordinator.cancelPendingUnlessCompleting()
    setResult(RESULT_CANCELED, Intent())
  }

  internal companion object {
    internal fun isCallbackUri(uri: Uri): Boolean = RedirectCoordinator.looksLikeCallback(uri)

    internal fun createResponseHandlingIntent(context: Context, responseUri: Uri?): Intent {
      val intent = createBaseIntent(context)
      intent.data = responseUri
      responseUri?.let { intent.putExtra(URI_KEY, it.toString()) }
      intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
      return intent
    }

    internal fun createAuthorizationIntent(context: Context, authorizationUri: Uri): Intent =
      createBaseIntent(context).apply { putExtra(URI_KEY, authorizationUri.toString()) }

    internal fun createBaseIntent(context: Context): Intent =
      Intent(context, SSOManagerActivity::class.java)

    internal const val URI_KEY = "uri"
    internal const val KEY_PENDING_CALLBACK_URI = "pending_callback_uri"
  }
}
