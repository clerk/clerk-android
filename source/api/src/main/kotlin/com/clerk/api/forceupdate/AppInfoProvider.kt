package com.clerk.api.forceupdate

import android.content.pm.PackageManager
import android.os.Build
import com.clerk.api.Clerk

internal object AppInfoProvider {
  fun packageName(): String? = Clerk.applicationContext?.get()?.packageName?.takeIf { it.isNotBlank() }

  fun appVersion(): String? {
    val context = Clerk.applicationContext?.get() ?: return null

    val packageInfo =
      try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
          context.packageManager.getPackageInfo(
            context.packageName,
            PackageManager.PackageInfoFlags.of(0),
          )
        } else {
          @Suppress("DEPRECATION")
          context.packageManager.getPackageInfo(context.packageName, 0)
        }
      } catch (_: Exception) {
        return null
      }

    return packageInfo.versionName?.trim()?.takeIf { it.isNotEmpty() }
  }
}
