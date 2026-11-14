package com.clerk.api.dpop

import kotlin.test.assertEquals
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant
import org.junit.Test

class ServerClockTest {
  private val localClock =
    object : Clock {
      var now = Instant.parse("2026-11-04T12:00:00Z")

      override fun now(): Instant = now
    }
  private val serverClock = ServerClock(localClock)

  @Test
  fun `before any server time the server clock reads local time`() {
    assertEquals(localClock.now, serverClock.now())
  }

  @Test
  fun `a server time ten minutes ahead moves now by ten minutes`() {
    serverClock.record(Instant.parse("2026-11-04T12:10:00Z"))
    localClock.now += 5.seconds

    assertEquals(localClock.now + 10.minutes, serverClock.now())
  }

  @Test
  fun `a second server time replaces the first`() {
    serverClock.record(Instant.parse("2026-11-04T12:10:00Z"))
    serverClock.record(Instant.parse("2026-11-04T11:40:00Z"))

    assertEquals(localClock.now - 20.minutes, serverClock.now())
  }
}
