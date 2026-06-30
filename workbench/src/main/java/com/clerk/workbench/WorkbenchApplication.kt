package com.clerk.workbench

import android.app.Application
import androidx.compose.ui.graphics.Color
import com.clerk.api.Clerk
import com.clerk.api.ClerkConfigurationOptions
import com.clerk.api.ui.ClerkColors
import com.clerk.api.ui.ClerkTheme

class WorkbenchApplication : Application() {

  override fun onCreate() {
    super.onCreate()
    StorageHelper.initialize(this)

    val publicKey = StorageHelper.loadValue(StorageKey.PUBLIC_KEY)
    val proxyUrl = StorageHelper.loadValue(StorageKey.PROXY_URL)
    publicKey?.let { key ->
      Clerk.initialize(
        this,
        key,
        options = ClerkConfigurationOptions(enableDebugMode = true, proxyUrl = proxyUrl),
        theme = ClerkTheme(colors = ClerkColors(background = Color.Green))
      )
    }
  }
}
