package com.clerk.exampleapp

import android.app.Application
import com.clerk.Clerk
import com.clerk.ClerkConfigurationOptions
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class MainApplication : Application() {
  override fun onCreate() {
    super.onCreate()

    Clerk.initialize(
      this,
      BuildConfig.CLERK_PUBLISHABLE_KEY,
      options = ClerkConfigurationOptions(enableDebugMode = true),
    )
  }
}
