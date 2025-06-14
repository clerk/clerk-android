package com.clerk.sso

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.browser.customtabs.CustomTabsIntent
import androidx.lifecycle.lifecycleScope
import com.clerk.log.ClerkLog
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
    super.onResume()
    // on first run, launch the intent to start the OAuth/SSO flow in the browser
    if (!authorizationStarted) {
      try {
        ClerkLog.d("Launching custom tab with uri: $desiredUri")
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
    // subsequent runs, we either got the response back from OAuthReceiverActivity or it was
    // cancelled
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

  /**
   * Restores the activity state from a bundle. This is called both for new instances (from intent
   * extras) and restored instances (from saved instance state).
   *
   * @param state Bundle containing the activity state
   */
  private fun hydrateState(state: Bundle?) {
    ClerkLog.d("hydrateState called with state: $state")
    if (state == null) return finish()
    authorizationStarted = state.getBoolean(KEY_AUTHORIZATION_STARTED, false)
    state.getString(URI_KEY)?.let { desiredUri = Uri.parse(it) }
  }

  /**
   * Handles successful authentication callback by completing the authentication flow.
   *
   * @param uri The callback URI containing authentication results
   */
  private fun authorizationComplete(uri: Uri) {
    lifecycleScope.launch { SSOService.completeAuthenticateWithRedirect(uri) }
  }

  /** Handles authentication cancellation by the user. */
  private fun authorizationCanceled() {
    val response = Intent()
    setResult(RESULT_CANCELED, response)
  }

  /** Handles the case where no compatible browser is found to handle the authentication. */
  private fun noBrowserFound() {
    val response = Intent()
    setResult(RESULT_CANCELED, response)
  }

  /** Handles the case where no authentication URI is found in the activity's state. */
  private fun noUriFound() {
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
      // Add the URI to the extras Bundle as well
      responseUri?.let { intent.putExtra(URI_KEY, it.toString()) }
      intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
      return intent
    }

    /**
     * Creates a base intent for the SSO manager activity.
     *
     * @param context The context to create the intent from
     * @return Basic intent for the SSO manager activity
     */
    internal fun createBaseIntent(context: Context): Intent =
      Intent(context, SSOManagerActivity::class.java)

    internal const val URI_KEY = "uri"
    private const val KEY_AUTHORIZATION_STARTED = "authStarted"
  }
}
