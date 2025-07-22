package com.clerk.customflows

import android.app.Application
import com.clerk.Clerk
import com.clerk.ClerkConfigurationOptions

class CustomFlowsApplication : Application() {
  override fun onCreate() {
    super.onCreate()
    Clerk.initialize(
      this,
      publishableKey = "pk_test_ZWxlZ2FudC1tdXNrcmF0LTg3LmNsZXJrLmFjY291bnRzLmRldiQ",
      options = ClerkConfigurationOptions(enableDebugMode = true),
    )
  }
}
