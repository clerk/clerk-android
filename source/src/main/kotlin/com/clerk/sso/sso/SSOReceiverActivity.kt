package com.clerk.sso.sso

import android.app.Activity
import android.os.Bundle
import com.clerk.log.ClerkLog

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
    ClerkLog.d("OAuthReceiverActivity started with intent: ${intent?.data}")
    super.onCreate(savedInstanceState)
    startActivity(SSOManagerActivity.createResponseHandlingIntent(this, intent?.data))
    finish()
  }
}
