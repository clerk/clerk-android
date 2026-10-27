package com.clerk.api.appversion

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import com.clerk.api.Clerk
import com.clerk.api.log.ClerkLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch

internal object UpdateRequiredViewLauncher {
  internal const val UI_LIBRARY_ACTIVITY_CLASS_NAME =
    "com.clerk.ui.updaterequired.UpdateRequiredActivity"

  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
  private var job: Job? = null

  @Volatile private var updateScreen: ComponentName? = null

  @Synchronized
  fun start(context: Context, enabled: Boolean) {
    stop()
    if (!enabled) return
    updateScreen = uiLibraryActivity(context) ?: return
    job = scope.launch {
      AppVersionSupport.status
        .distinctUntilChangedBy { it.isSupported }
        .filter { !it.isSupported }
        .collect { Clerk.currentActivity?.get()?.let(::launch) }
    }
  }

  @Synchronized
  fun stop() {
    updateScreen = null
    job?.cancel()
    job = null
  }

  fun onActivityResumed(activity: Activity) {
    if (!AppVersionSupport.status.value.isSupported) launch(activity)
  }

  private fun uiLibraryActivity(context: Context): ComponentName? {
    val component = ComponentName(context, UI_LIBRARY_ACTIVITY_CLASS_NAME)
    return try {
      context.packageManager.getActivityInfo(component, 0)
      component
    } catch (_: PackageManager.NameNotFoundException) {
      null
    }
  }

  private fun launch(activity: Activity) {
    val component = updateScreen ?: return
    if (activity.javaClass.name == UI_LIBRARY_ACTIVITY_CLASS_NAME || activity.isFinishing) return
    try {
      activity.startActivity(
        Intent().setComponent(component).addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
      )
    } catch (e: ActivityNotFoundException) {
      ClerkLogger.w("Couldn't show the update-required screen: ${e.message}")
    }
  }
}
