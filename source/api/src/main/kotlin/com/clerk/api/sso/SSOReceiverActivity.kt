package com.clerk.api.sso

import android.app.Activity
import android.os.Bundle
import com.clerk.api.hostedauth.HostedAuthService
import com.clerk.api.log.ClerkLog
import com.clerk.api.log.SafeUriLog

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
    if (HostedAuthService.isForgedCallback(callbackUri)) {
      ClerkLog.w("Ignoring invalid hosted auth callback")
      finish()
      return
    }
    startActivity(SSOManagerActivity.createResponseHandlingIntent(this, callbackUri))
    finish()
  }
}
