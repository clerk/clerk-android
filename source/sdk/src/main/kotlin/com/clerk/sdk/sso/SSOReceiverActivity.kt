package com.clerk.sdk.sso

import android.app.Activity
import android.os.Bundle
import com.clerk.sdk.log.ClerkLog

internal class SSOReceiverActivity : Activity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    ClerkLog.e("SSOReceiverActivity started with intent: ${intent?.data}")
    super.onCreate(savedInstanceState)
    startActivity(SSOManagerActivity.createResponseHandlingIntent(this, intent?.data))
    finish()
  }
}
