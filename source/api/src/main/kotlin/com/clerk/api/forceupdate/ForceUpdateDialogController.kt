package com.clerk.api.forceupdate

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.compose.ui.graphics.toArgb
import com.clerk.api.Clerk
import com.clerk.api.log.ClerkLog
import java.lang.ref.WeakReference
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

internal object ForceUpdateDialogController {
  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
  private var hasStarted = false
  private var currentActivity: WeakReference<Activity>? = null
  private var activeDialog: AlertDialog? = null
  private var latestStatus: ForceUpdateStatus = ForceUpdateStatus.SupportedDefault

  fun start(context: Context) {
    if (hasStarted) {
      return
    }

    hasStarted = true
    registerActivityCallbacks(context.applicationContext)

    scope.launch {
      Clerk.forceUpdateStatus.collectLatest { status ->
        latestStatus = status
        render()
      }
    }
  }

  private fun registerActivityCallbacks(context: Context) {
    val application = context as? Application ?: return

    application.registerActivityLifecycleCallbacks(
      object : Application.ActivityLifecycleCallbacks {
        override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit

        override fun onActivityStarted(activity: Activity) = Unit

        override fun onActivityResumed(activity: Activity) {
          currentActivity = WeakReference(activity)
          render()
        }

        override fun onActivityPaused(activity: Activity) {
          if (currentActivity?.get() == activity) {
            currentActivity = null
          }
        }

        override fun onActivityStopped(activity: Activity) = Unit

        override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit

        override fun onActivityDestroyed(activity: Activity) {
          if (currentActivity?.get() == activity) {
            currentActivity = null
            dismissDialog()
          }
        }
      }
    )
  }

  private fun render() {
    val status = latestStatus

    if (status.isSupported) {
      dismissDialog()
      return
    }

    val activity = currentActivity?.get() ?: return
    if (activity.isFinishing || activity.isDestroyed) {
      dismissDialog()
      return
    }

    if (activeDialog?.isShowing == true) {
      return
    }

    showDialog(activity, status)
  }

  private fun showDialog(activity: Activity, status: ForceUpdateStatus) {
    val dialog =
      AlertDialog
        .Builder(activity)
        .setTitle("Update required")
        .setMessage(messageFor(status))
        .create()

    dialog.setCancelable(false)
    dialog.setCanceledOnTouchOutside(false)

    status.updateUrl?.let {
      dialog.setButton(AlertDialog.BUTTON_POSITIVE, "Update") { _, _ -> }
    }

    dialog.show()
    applyTheme(dialog)

    status.updateUrl?.let { updateUrl ->
      dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.setOnClickListener {
        openUpdateUrl(activity = activity, updateUrl = updateUrl)
      }
    }

    activeDialog = dialog
  }

  private fun applyTheme(dialog: AlertDialog) {
    val background = Clerk.customTheme?.colors?.background?.toArgb() ?: DEFAULT_BACKGROUND
    val foreground = Clerk.customTheme?.colors?.foreground?.toArgb() ?: DEFAULT_FOREGROUND
    val mutedForeground = Clerk.customTheme?.colors?.mutedForeground?.toArgb() ?: DEFAULT_MUTED_FOREGROUND
    val primary = Clerk.customTheme?.colors?.primary?.toArgb() ?: DEFAULT_PRIMARY

    dialog.window?.setBackgroundDrawable(ColorDrawable(background))
    dialog.findViewById<TextView>(android.R.id.title)?.setTextColor(foreground)
    dialog.findViewById<TextView>(android.R.id.message)?.setTextColor(mutedForeground)
    dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(primary)
  }

  private fun messageFor(status: ForceUpdateStatus): String {
    val minimumVersion = status.minimumVersion?.takeIf { it.isNotBlank() }
    return if (minimumVersion != null) {
      "This version of the app is no longer supported. Please update to version $minimumVersion or newer."
    } else {
      "This version of the app is no longer supported. Please update to continue."
    }
  }

  private fun openUpdateUrl(activity: Activity, updateUrl: String) {
    val uri = runCatching { Uri.parse(updateUrl) }.getOrNull() ?: return
    val intent =
      Intent(Intent.ACTION_VIEW, uri).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      }

    runCatching { activity.startActivity(intent) }
      .onFailure { error ->
        ClerkLog.w("Failed to open update URL: ${error.message}")
      }
  }

  private fun dismissDialog() {
    activeDialog?.dismiss()
    activeDialog = null
  }

  private const val DEFAULT_PRIMARY = 0xFF2F3037.toInt()
  private const val DEFAULT_BACKGROUND = 0xFFFFFFFF.toInt()
  private const val DEFAULT_FOREGROUND = 0xFF212126.toInt()
  private const val DEFAULT_MUTED_FOREGROUND = 0xFF747686.toInt()
}
