package com.clerk.api.log

import android.util.Log

/**
 * Internal logging utility for the Clerk SDK.
 *
 * This object provides a centralized logging interface that wraps Android's Log class with
 * consistent formatting and tagging for all Clerk SDK log messages. All log messages are prefixed
 * with "Clerk" to make them easily identifiable in log output.
 *
 * This is an internal utility and should not be used outside the Clerk SDK.
 */
internal object ClerkLog {
  /**
   * Logs an error message.
   *
   * Use this method to log error conditions that indicate a problem in the SDK or application flow
   * that should be investigated.
   *
   * @param message The error message to log
   * @return The result of the underlying Log.e() call
   */
  fun e(message: String) = Log.e("ClerkLog", "Clerk error: $message")

  /**
   * Logs a warning message.
   *
   * Use this method to log warning conditions that indicate potential issues but don't prevent
   * normal operation.
   *
   * @param message The warning message to log
   * @return The result of the underlying Log.w() call
   */
  fun w(message: String) = Log.w("ClerkLog", "Clerk warning: $message")

  /**
   * Logs an informational message.
   *
   * Use this method to log general information about SDK operations and state changes.
   *
   * @param message The informational message to log
   * @return The result of the underlying Log.i() call
   */
  fun i(message: String) = Log.i("ClerkLog", message)

  /**
   * Logs a debug message.
   *
   * Use this method to log detailed information useful for debugging and development. Debug
   * messages are typically filtered out in release builds.
   *
   * @param message The debug message to log
   * @return The result of the underlying Log.d() call
   */
  fun d(message: String) = Log.d("ClerkLog", message)

  /**
   * Logs a verbose message.
   *
   * Use this method to log very detailed tracing information. Verbose messages provide the most
   * detailed logging level and are typically used for deep debugging.
   *
   * @param message The verbose message to log
   * @return The result of the underlying Log.v() call
   */
  fun v(message: String) = Log.v("ClerkLog", message)
}
