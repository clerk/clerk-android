package com.clerk.workbench

import android.app.Application
import com.clerk.api.Clerk
import com.clerk.api.ClerkConfigurationOptions

class WorkbenchApplication : Application() {

  override fun onCreate() {
    super.onCreate()
    StorageHelper.initialize(this)
    Clerk.initialize(
      this,
      BuildConfig.WORKBENCH_CLERK_PUBLISHABLE_KEY,
      options = ClerkConfigurationOptions(enableDebugMode = true),
    )
  }
}
