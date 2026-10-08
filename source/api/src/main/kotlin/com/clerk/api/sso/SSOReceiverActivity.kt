package com.clerk.api.sso

import android.app.Activity
import android.os.Bundle
import com.clerk.api.log.ClerkLog
import com.clerk.api.log.SafeUriLog
import com.clerk.api.redirect.ReceiverDelivery
import com.clerk.api.redirect.RedirectCoordinator

/**
 * Exported entry point for redirect callbacks. Any app can start it, so while a flow is pending
 * only a callback that passes that flow's state check reaches [SSOManagerActivity]; see
 * [RedirectCoordinator.receiverDelivery].
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
    when (RedirectCoordinator.receiverDelivery(callbackUri)) {
      ReceiverDelivery.FORWARD ->
        startActivity(SSOManagerActivity.createResponseHandlingIntent(this, callbackUri))
      ReceiverDelivery.COMPLETE_IN_BACKGROUND ->
        RedirectCoordinator.dispatchInBackground(callbackUri)
      ReceiverDelivery.DROP -> Unit
    }
    finish()
  }
}
