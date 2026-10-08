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
      var lifecycleScopeCancelled = false
      val flowThisActivityHosted = RedirectCoordinator.current()
      try {
        val outcome = RedirectCoordinator.dispatch(uri)
        pendingCallbackUri = null
        val succeeded = outcome is CallbackOutcome.Completed && outcome.success
        setResult(if (succeeded) RESULT_OK else RESULT_CANCELED, Intent())
      } catch (cancellation: CancellationException) {
        lifecycleScopeCancelled = true
        throw cancellation
      } catch (t: Throwable) {
        ClerkLog.e("authorizationComplete failed: ${t.message}")
        setResult(RESULT_CANCELED, Intent())
      } finally {
        // Delivering the callback cleared the Custom Tab, so a flow this callback did not complete
        // can no longer receive one; fail it rather than leave its caller waiting.
        if (!lifecycleScopeCancelled && flowThisActivityHosted != null) {
          RedirectCoordinator.cancelPendingUnlessCompleting(flowThisActivityHosted)
        }
        finish()
      }
    }
  }

  private fun authorizationFailed() {
    RedirectCoordinator.cancelPendingUnlessCompleting()
    setResult(RESULT_CANCELED, Intent())
  }

  internal companion object {
    internal fun isCallbackUri(uri: Uri): Boolean = RedirectCoordinator.isCallbackIntentData(uri)

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
