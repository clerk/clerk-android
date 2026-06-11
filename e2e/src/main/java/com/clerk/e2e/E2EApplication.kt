package com.clerk.e2e

import android.app.Application
import com.clerk.api.Clerk
import com.clerk.api.ClerkConfigurationOptions

class E2EApplication : Application() {
  override fun onCreate() {
    super.onCreate()
    Clerk.initialize(
      this,
      publishableKey = BuildConfig.E2E_CLERK_PUBLISHABLE_KEY,
      options = ClerkConfigurationOptions(enableDebugMode = true),
    )
  }
}
