package com.clerk.prebuiltui

import android.app.Application
import com.clerk.api.Clerk
import com.clerk.api.ClerkConfigurationOptions

class PrebuiltUiApplication : Application() {

  override fun onCreate() {
    super.onCreate()
    Clerk.initialize(
      this,
      BuildConfig.PREBUILT_UI_CLERK_PUBLISHABLE_KEY,
      options = ClerkConfigurationOptions(enableDebugMode = true),
    )
  }
}
