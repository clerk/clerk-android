package com.clerk.api.session

import com.clerk.api.Constants.Config.TOKEN_FETCH_BACKOFF_BASE_MS
import com.clerk.api.Constants.Config.TOKEN_FETCH_BACKOFF_MAX_MS
import java.util.concurrent.ConcurrentHashMap

/**
 * Per-cache-key failure backoff for session token requests.
 *
 * The SDK wakes every few seconds to refresh the active session token. Without a backoff a failing
 * endpoint is re-POSTed on every wake for as long as the app stays in the foreground, so each
 * consecutive failure doubles the window during which the fetch layer refuses to send another
 * request.
 *
 * The failure count outlives the window it opened: an expired window means another request may go
 * out, not that the endpoint recovered. Only a success or a deliberate state change (sign-out, org
 * switch, SDK reset) resets the schedule. Dropping the count at the deadline instead would restart
 * every key at five seconds and leave the wake loop hammering forever.
 *
 * @param clock Source of the current time in milliseconds. Injectable for tests.
 */
internal class SessionTokenBackoff(private val clock: () -> Long = System::currentTimeMillis) {

  private val windows = ConcurrentHashMap<String, Window>()

  /** Whether a request for [cacheKey] is still inside the window opened by its last failure. */
  internal fun isBackingOff(cacheKey: String): Boolean {
    val window = windows[cacheKey] ?: return false
    return clock() < window.retryAtMillis
  }

  /** Opens the next, wider window for [cacheKey]. */
  internal fun recordFailure(cacheKey: String) {
    windows.compute(cacheKey) { _, existing ->
      val failures = (existing?.failures ?: 0) + 1
      Window(failures = failures, retryAtMillis = clock() + delayMillis(failures))
    }
  }

  /** Closes the window for [cacheKey] and resets its schedule. */
  internal fun recordSuccess(cacheKey: String) {
    windows.remove(cacheKey)
  }

  /** Resets the schedule for a session's default and templated keys. */
  internal fun clearForSession(sessionId: String) {
    windows.keys.removeAll { it == sessionId || it.startsWith("$sessionId-") }
  }

  /** Resets every schedule. */
  internal fun clear() {
    windows.clear()
  }

  private fun delayMillis(failures: Int): Long {
    val exponent = (failures - 1).coerceAtMost(EXPONENT_CEILING)
    return (TOKEN_FETCH_BACKOFF_BASE_MS shl exponent).coerceAtMost(TOKEN_FETCH_BACKOFF_MAX_MS)
  }

  private data class Window(val failures: Int, val retryAtMillis: Long)

  internal companion object {
    /** Keeps the shift below Long overflow; the delay is capped well before this bites. */
    private const val EXPONENT_CEILING = 16

    /** The schedule the SDK uses. Tests build their own with a fake clock. */
    internal val shared = SessionTokenBackoff()
  }
}
