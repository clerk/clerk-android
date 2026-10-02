package com.clerk.api.sso

import android.app.Activity
import android.os.Bundle
import com.clerk.api.log.ClerkLog
import com.clerk.api.log.SafeUriLog
import com.clerk.api.redirect.RedirectCoordinator

/**
 * Exported entry point for redirect callbacks. Any app can start it, so a callback that targets the
 * pending flow but fails its state check is dropped here and never reaches the flow.
 */
internal class SSOReceiverActivity : Activity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    ClerkLog.d("OAuthReceiverActivity started with uri: ${SafeUriLog.describe(intent?.data)}")
    super.onCreate(savedInstanceState)
    val callbackUri = intent?.data
    if (callbackUri == null || !SSOManagerActivity.isCallbackUri(callbackUri)) {
      ClerkLog.w("Ignoring unrecognized OAuth/SSO callback")
      finish()
      return
    }
    if (RedirectCoordinator.isRejected(callbackUri)) {
      ClerkLog.w("Ignoring redirect callback with invalid state")
      finish()
      return
    }
    startActivity(SSOManagerActivity.createResponseHandlingIntent(this, callbackUri))
    finish()
  }
}
