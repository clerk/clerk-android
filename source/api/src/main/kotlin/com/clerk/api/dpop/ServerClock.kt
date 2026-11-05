package com.clerk.api.dpop

import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Instant

/**
 * The local clock moved by the offset of the last server time it recorded.
 *
 * The network stack owns the one instance. The code that records server time and the code that
 * builds proofs must share it, or proofs keep the local clock's skew.
 *
 * @param localClock [Clock.System] in production.
 */
internal class ServerClock(private val localClock: Clock) : Clock {
  @Volatile private var offset: Duration = Duration.ZERO

  fun record(serverTime: Instant) {
    offset = serverTime - localClock.now()
  }

  override fun now(): Instant = localClock.now() + offset
}
