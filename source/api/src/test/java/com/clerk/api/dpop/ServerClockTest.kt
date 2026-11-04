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
  fun `without a Date header the server clock reads local time`() {
    assertEquals(localClock.now, serverClock.now())
  }

  @Test
  fun `a Date header ten minutes ahead moves now by ten minutes`() {
    serverClock.record("Wed, 04 Nov 2026 12:10:00 GMT")
    localClock.now += 5.seconds

    assertEquals(localClock.now + 10.minutes, serverClock.now())
  }

  @Test
  fun `a missing or unparsable Date header leaves the offset`() {
    serverClock.record("Wed, 04 Nov 2026 12:10:00 GMT")

    serverClock.record(null)
    serverClock.record("")
    serverClock.record("not a date")
    serverClock.record("Wed, 04 Nov 2026 12:10:00 GMT and more")

    assertEquals(localClock.now + 10.minutes, serverClock.now())
  }

  @Test
  fun `a second Date header replaces the first`() {
    serverClock.record("Wed, 04 Nov 2026 12:10:00 GMT")
    serverClock.record("Wed, 04 Nov 2026 11:40:00 GMT")

    assertEquals(localClock.now - 20.minutes, serverClock.now())
  }
}
