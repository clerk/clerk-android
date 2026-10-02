package com.clerk.ui.core.log

import android.util.Log
import com.clerk.api.Clerk

/**
 * Logger for the Clerk UI module.
 *
 * Mirrors the API module's logger (same tag and debug gating) so that the UI module does not depend
 * on `com.clerk.api.log.ClerkLog`, which is being removed from the public API.
 */
internal object ClerkLog {
  private const val TAG = "ClerkLog"

  private inline fun safeLog(action: () -> Int): Int =
    try {
      action()
    } catch (_: Throwable) {
      0
    }

  /** Logs an error message. */
  fun e(message: String): Int = safeLog { Log.e(TAG, "Clerk error: $message") }

  /** Logs a debug message when [Clerk.debugMode] is enabled. */
  fun d(message: String): Int = if (Clerk.debugMode) safeLog { Log.d(TAG, message) } else 0

  /** Logs a verbose message when [Clerk.debugMode] is enabled. */
  fun v(message: String): Int = if (Clerk.debugMode) safeLog { Log.v(TAG, message) } else 0
}
