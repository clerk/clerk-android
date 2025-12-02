package com.clerk.workbench

import android.app.Application
import com.clerk.api.Clerk
import com.clerk.api.ClerkConfigurationOptions

class WorkbenchApplication : Application() {

  override fun onCreate() {
    super.onCreate()
    StorageHelper.initialize(this)

    val publicKey = StorageHelper.loadValue(StorageKey.PUBLIC_KEY)
    publicKey?.let { key ->
      Clerk.initialize(this, key, options = ClerkConfigurationOptions(enableDebugMode = true))
    }
  }
}
