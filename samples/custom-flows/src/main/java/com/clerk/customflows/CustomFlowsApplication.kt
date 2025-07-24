package com.clerk.customflows

import android.app.Application
import com.clerk.Clerk
import com.clerk.ClerkConfigurationOptions

class CustomFlowsApplication : Application() {
  override fun onCreate() {
    super.onCreate()
    Clerk.initialize(
      this,
      publishableKey = BuildConfig.CUSTOM_FLOWS_CLERK_PUBLISHABLE_KEY,
      options = ClerkConfigurationOptions(enableDebugMode = true),
    )
  }
}
