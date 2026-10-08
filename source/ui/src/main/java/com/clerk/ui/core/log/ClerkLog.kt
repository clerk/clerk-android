package com.clerk.ui.core.log

import android.util.Log
import com.clerk.api.Clerk

internal object ClerkLog {
  private const val TAG = "ClerkLog"

  private inline fun safeLog(action: () -> Int): Int =
    try {
      action()
    } catch (_: Throwable) {
      0
    }

  fun e(message: String): Int = safeLog { Log.e(TAG, "Clerk error: $message") }

  fun d(message: String): Int = if (Clerk.debugMode) safeLog { Log.d(TAG, message) } else 0

  fun v(message: String): Int = if (Clerk.debugMode) safeLog { Log.v(TAG, message) } else 0
}
