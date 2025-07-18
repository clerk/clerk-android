package com.clerk.exampleapp

import android.app.Application
import com.clerk.Clerk
import com.clerk.ClerkConfigurationOptions
import dagger.hilt.android.HiltAndroidApp

/**
 * Main Application class for the Clerk Android SDK Example App.
 *
 * This class is responsible for initializing Clerk when the app starts. The Hilt annotation enables
 * dependency injection throughout the app.
 *
 * Requirements:
 * - CLERK_PUBLISHABLE_KEY must be set in gradle.properties
 * - This class must be registered in AndroidManifest.xml
 */
@HiltAndroidApp
class MainApplication : Application() {
  override fun onCreate() {
    super.onCreate()

    /**
     * Initialize Clerk with your publishable key.
     *
     * The publishable key is injected from BuildConfig, which reads from gradle.properties. Enable
     * debug mode for development to see detailed logs.
     *
     * For production:
     * - Set enableDebugMode = false
     * - Use your live publishable key (pk_live_...)
     */
    Clerk.initialize(
      this,
      BuildConfig.SAMPLE_APP_CLERK_PUBLISHABLE_KEY,
      options = ClerkConfigurationOptions(enableDebugMode = true),
    )
  }
}
