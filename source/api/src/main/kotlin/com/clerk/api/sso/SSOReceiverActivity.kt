package com.clerk.api.sso

import android.app.Activity
import android.os.Bundle
import com.clerk.api.hostedauth.HostedAuthService
import com.clerk.api.log.ClerkLog
import com.clerk.api.log.SafeUriLog

/**
 * Activity that receives OAuth/SSO callbacks via deep links. This activity serves as the entry
 * point for authentication provider callbacks and immediately forwards them to [SSOManagerActivity]
 * for processing.
 *
 * The activity is declared in the manifest to handle the OAuth callback URL scheme, making it the
 * destination for authentication provider redirects.
 *
 * This activity is intentionally minimal, only responsible for:
 * 1. Receiving the callback URI from the authentication provider
 * 2. Forwarding it to SSOManagerActivity for processing
 * 3. Finishing itself to maintain a clean back stack
 */
internal class SSOReceiverActivity : Activity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    ClerkLog.d("OAuthReceiverActivity started with uri: ${SafeUriLog.describe(intent?.data)}")
    super.onCreate(savedInstanceState)
    val callbackUri = intent?.data
    if (callbackUri != null && HostedAuthService.canHandle(callbackUri)) {
      if (!HostedAuthService.isValidCallback(callbackUri)) {
        ClerkLog.w("Ignoring invalid hosted auth callback")
        finish()
        return
      }
    }
    startActivity(SSOManagerActivity.createResponseHandlingIntent(this, callbackUri))
    finish()
  }
}
