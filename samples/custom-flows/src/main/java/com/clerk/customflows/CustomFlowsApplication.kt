package com.clerk.customflows

import android.app.Application
import com.clerk.api.Clerk
import com.clerk.api.ClerkConfigurationOptions

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
