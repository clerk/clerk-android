package com.clerk.api.log

import android.util.Log
import com.clerk.api.Clerk

internal object ClerkLogger {
  private inline fun safeLog(action: () -> Int): Int =
    try {
      action()
    } catch (_: Throwable) {
      0
    }

  private fun fallback(prefix: String, message: String): Int {
    safeLog {
      println("$prefix$message")
      0
    }
    return 0
  }

  fun e(message: String): Int =
    safeLog { Log.e("ClerkLog", "Clerk error: $message") }.takeIf { it != 0 }
      ?: fallback("Clerk error: ", message)

  fun w(message: String): Int =
    safeLog { Log.w("ClerkLog", "Clerk warning: $message") }.takeIf { it != 0 }
      ?: fallback("Clerk warning: ", message)

  fun i(message: String): Int =
    safeLog { Log.i("ClerkLog", message) }.takeIf { it != 0 } ?: fallback("Clerk info: ", message)

  fun d(message: String): Int =
    if (Clerk.debugMode) {
      safeLog { Log.d("ClerkLog", message) }.takeIf { it != 0 }
        ?: fallback("Clerk debug: ", message)
    } else {
      0
    }

  fun v(message: String): Int =
    if (Clerk.debugMode) {
      safeLog { Log.v("ClerkLog", message) }.takeIf { it != 0 }
        ?: fallback("Clerk verbose: ", message)
    } else {
      0
    }
}
