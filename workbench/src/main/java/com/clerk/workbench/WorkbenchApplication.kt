package com.clerk.workbench

import android.app.Application
import androidx.compose.ui.text.font.FontWeight
import com.clerk.api.Clerk
import com.clerk.api.ClerkConfigurationOptions
import com.clerk.api.ui.ClerkTheme
import com.clerk.api.ui.ClerkTypographyDefaults

class WorkbenchApplication : Application() {

  override fun onCreate() {
    super.onCreate()
    StorageHelper.initialize(this)

    val publicKey = StorageHelper.loadValue(StorageKey.PUBLIC_KEY)
    publicKey?.let { key ->
      val typographyOverrides =
        ClerkTypographyDefaults.typography {
          displaySmall = displaySmall.copy(fontWeight = FontWeight.SemiBold)
        }

      Clerk.initialize(
        this,
        key,
        options = ClerkConfigurationOptions(enableDebugMode = true),
        theme = ClerkTheme(typography = typographyOverrides),
      )
    }
  }
}
