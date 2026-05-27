package com.clerk.ui.util

import android.content.Context
import android.content.Intent

internal object EmailAppLauncher {
  fun open(context: Context): Boolean {
    val packageManager = context.packageManager
    val appIntent = Intent(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_APP_EMAIL) }

    val explicitLaunchIntent =
      packageManager.queryIntentActivities(appIntent, 0).firstNotNullOfOrNull { resolveInfo ->
        packageManager.getLaunchIntentForPackage(resolveInfo.activityInfo.packageName)
      }

    if (explicitLaunchIntent != null && context.tryStartActivity(explicitLaunchIntent)) {
      return true
    }

    return context.tryStartActivity(appIntent)
  }

  private fun Context.tryStartActivity(intent: Intent): Boolean {
    val launchIntent = Intent(intent).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
    return runCatching {
        startActivity(launchIntent)
        true
      }
      .getOrElse { false }
  }
}
