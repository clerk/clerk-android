package com.clerk.linearclone

import android.app.Application
import com.clerk.Clerk
import com.clerk.ClerkConfigurationOptions

class LinearCloneApp : Application() {

  override fun onCreate() {
    super.onCreate()
    Clerk.initialize(
      this,
      BuildConfig.LINEAR_CLONE_CLERK_PUBLISHABLE_KEY,
      ClerkConfigurationOptions(enableDebugMode = true),
    )
  }
}
