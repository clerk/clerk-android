package com.clerk.sdk.sso

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.browser.customtabs.CustomTabsIntent
import androidx.lifecycle.lifecycleScope
import com.clerk.sdk.log.ClerkLog
import com.clerk.sdk.service.SSOService
import kotlinx.coroutines.launch

internal class SSOManagerActivity : AppCompatActivity() {
  private var authorizationStarted = false
  private lateinit var desiredUri: Uri

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    if (savedInstanceState == null) {
      hydrateState(intent.extras)
    } else {
      hydrateState(savedInstanceState)
    }
  }

  override fun onResume() {
    ClerkLog.e("onResume called")
    super.onResume()
    // on first run, launch the intent to start the OAuth/SSO flow in the browser
    if (!authorizationStarted) {
      try {
        ClerkLog.e("Launching custom tab with uri: $desiredUri")
        CustomTabsIntent.Builder().build().launchUrl(this, desiredUri)
        authorizationStarted = true
      } catch (_: UninitializedPropertyAccessException) {
        noUriFound()
        finish()
      } catch (_: ActivityNotFoundException) {
        noBrowserFound()
        finish()
      }
      return
    }
    // subsequent runs, we either got the response back from SSOReceiverActivity or it was cancelled
    intent.data?.let { authorizationComplete(it) } ?: authorizationCanceled()
    finish()
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    setIntent(intent)
  }

  override fun onSaveInstanceState(outState: Bundle) {
    super.onSaveInstanceState(outState)
    outState.putBoolean(KEY_AUTHORIZATION_STARTED, authorizationStarted)
  }

  private fun hydrateState(state: Bundle?) {
    ClerkLog.e("hydrateState called with state: $state")
    if (state == null) return finish()
    authorizationStarted = state.getBoolean(KEY_AUTHORIZATION_STARTED, false)
    state.getString(URI_KEY)?.let { desiredUri = Uri.parse(it) }
  }

  private fun authorizationComplete(uri: Uri) {
    lifecycleScope.launch { SSOService.completeAuthenticateWithRedirect(uri) }
  }

  private fun authorizationCanceled() {
    val response = Intent()
    setResult(RESULT_CANCELED, response)
  }

  private fun noBrowserFound() {
    val response = Intent()
    setResult(RESULT_CANCELED, response)
  }

  private fun noUriFound() {
    val response = Intent()
    setResult(RESULT_CANCELED, response)
  }

  internal companion object {
    internal fun createResponseHandlingIntent(context: Context, responseUri: Uri?): Intent {
      val intent = createBaseIntent(context)
      intent.data = responseUri
      // Add the URI to the extras Bundle as well
      responseUri?.let { intent.putExtra(URI_KEY, it.toString()) }
      intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
      return intent
    }

    internal fun createBaseIntent(context: Context): Intent =
      Intent(context, SSOManagerActivity::class.java)

    internal const val URI_KEY = "uri"
    private const val KEY_AUTHORIZATION_STARTED = "authStarted"
  }
}
