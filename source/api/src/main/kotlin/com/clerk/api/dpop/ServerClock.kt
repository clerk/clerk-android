package com.clerk.api.dpop

import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Instant

internal class ServerClock(private val localClock: Clock) : Clock {
  @Volatile private var offset: Duration = Duration.ZERO

  fun record(serverTime: Instant) {
    offset = serverTime - localClock.now()
  }

  override fun now(): Instant = localClock.now() + offset
}
