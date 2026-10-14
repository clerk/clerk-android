package com.clerk.api.log

/**
 * Logging utility used by the Clerk SDK.
 *
 * This is an SDK implementation detail that was published by accident; it forwards to the SDK's
 * internal logger.
 */
@Deprecated(
  "ClerkLog is the SDK's internal logger and will become internal in the next major version. " +
    "Use android.util.Log or your own logger instead."
)
public object ClerkLog {
  /** Logs an error message. Returns the result of the underlying `Log.e()` call. */
  public fun e(message: String): Int = ClerkLogger.e(message)

  /** Logs a warning message. Returns the result of the underlying `Log.w()` call. */
  public fun w(message: String): Int = ClerkLogger.w(message)

  /** Logs an informational message. Returns the result of the underlying `Log.i()` call. */
  public fun i(message: String): Int = ClerkLogger.i(message)

  /** Logs a debug message when [com.clerk.api.Clerk.debugMode] is enabled. */
  public fun d(message: String): Int = ClerkLogger.d(message)

  /** Logs a verbose message when [com.clerk.api.Clerk.debugMode] is enabled. */
  public fun v(message: String): Int = ClerkLogger.v(message)
}
