package com.clerk.api.appversion

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.ComponentName
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
  // The api library can't depend on the UI library, so it names the UI library's activity.
  internal const val ACTIVITY_CLASS_NAME = "com.clerk.ui.updaterequired.UpdateRequiredActivity"

  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
  private var job: Job? = null

  @Volatile private var enabled = false

  fun start(enabled: Boolean) {
    stop()
    this.enabled = enabled
    if (!enabled) return
    job = scope.launch {
      AppVersionSupport.status
        .distinctUntilChangedBy { it.isSupported }
        .filter { !it.isSupported }
        .collect { Clerk.currentActivity?.get()?.let(::launch) }
    }
  }

  fun stop() {
    enabled = false
    job?.cancel()
    job = null
  }

  fun onActivityResumed(activity: Activity) {
    if (enabled && !AppVersionSupport.status.value.isSupported) launch(activity)
  }

  private fun launch(activity: Activity) {
    if (activity.javaClass.name == ACTIVITY_CLASS_NAME || activity.isFinishing) return
    val component = ComponentName(activity, ACTIVITY_CLASS_NAME)
    try {
      activity.packageManager.getActivityInfo(component, 0)
    } catch (_: PackageManager.NameNotFoundException) {
      return
    }
    try {
      activity.startActivity(
        Intent().setComponent(component).addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
      )
    } catch (e: ActivityNotFoundException) {
      ClerkLogger.w("Couldn't show the update-required screen: ${e.message}")
    }
  }
}
