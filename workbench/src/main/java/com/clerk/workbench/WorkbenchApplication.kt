package com.clerk.workbench

import android.app.Application
import com.clerk.api.Clerk
import com.clerk.api.ClerkConfigurationOptions

class WorkbenchApplication : Application() {

  override fun onCreate() {
    super.onCreate()
    StorageHelper.initialize(this)

    val publicKey = StorageHelper.loadValue(StorageKey.PUBLIC_KEY)
    val proxyUrl = StorageHelper.loadValue(StorageKey.PROXY_URL)
    publicKey?.let { key ->
      Clerk.initialize(
        this,
        "pk_test_bG92aW5nLW1vdGgtMjIuY2xlcmsuYWNjb3VudHMuZGV2JA",
        options = ClerkConfigurationOptions(enableDebugMode = true, proxyUrl = proxyUrl),
      )
    }
  }
}
