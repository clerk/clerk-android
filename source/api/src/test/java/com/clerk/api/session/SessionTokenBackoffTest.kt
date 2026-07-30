package com.clerk.api.session

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionTokenBackoffTest {

  @Test
  fun `a key with no failures is never backing off`() {
    val backoff = SessionTokenBackoff { 0L }

    assertFalse(backoff.isBackingOff("sess_1"))
  }

  @Test
  fun `the first failure blocks requests for five seconds`() {
    var now = 0L
    val backoff = SessionTokenBackoff { now }

    backoff.recordFailure("sess_1")

    now = 4_999L
    assertTrue(backoff.isBackingOff("sess_1"))
    now = 5_000L
    assertFalse(backoff.isBackingOff("sess_1"))
  }

  @Test
  fun `consecutive failures double the window even after each one expires`() {
    var now = 0L
    val backoff = SessionTokenBackoff { now }

    backoff.recordFailure("sess_1")
    now = 5_000L
    // Reading past the deadline is what the wake loop does; it must not reset the schedule.
    assertFalse(backoff.isBackingOff("sess_1"))
    backoff.recordFailure("sess_1")

    now = 14_999L
    assertTrue(backoff.isBackingOff("sess_1"))
    now = 15_000L
    assertFalse(backoff.isBackingOff("sess_1"))
    backoff.recordFailure("sess_1")

    now = 34_999L
    assertTrue(backoff.isBackingOff("sess_1"))
    now = 35_000L
    assertFalse(backoff.isBackingOff("sess_1"))
  }

  @Test
  fun `the window is capped at sixty seconds`() {
    var now = 0L
    val backoff = SessionTokenBackoff { now }

    repeat(20) { backoff.recordFailure("sess_1") }

    now = 59_999L
    assertTrue(backoff.isBackingOff("sess_1"))
    now = 60_000L
    assertFalse(backoff.isBackingOff("sess_1"))
  }

  @Test
  fun `a success closes the window and resets the schedule`() {
    var now = 0L
    val backoff = SessionTokenBackoff { now }

    backoff.recordFailure("sess_1")
    backoff.recordFailure("sess_1")
    backoff.recordSuccess("sess_1")
    assertFalse(backoff.isBackingOff("sess_1"))

    backoff.recordFailure("sess_1")
    now = 5_000L
    assertFalse(backoff.isBackingOff("sess_1"))
  }

  @Test
  fun `windows are tracked per cache key`() {
    val backoff = SessionTokenBackoff { 0L }

    backoff.recordFailure("sess_1")

    assertTrue(backoff.isBackingOff("sess_1"))
    assertFalse(backoff.isBackingOff("sess_1-custom_template"))
    assertFalse(backoff.isBackingOff("sess_2"))
  }

  @Test
  fun `clearForSession resets the schedule for the default and templated keys`() {
    var now = 0L
    val backoff = SessionTokenBackoff { now }

    backoff.recordFailure("sess_1")
    backoff.recordFailure("sess_1")
    backoff.recordFailure("sess_1-custom_template")
    backoff.recordFailure("sess_2")

    backoff.clearForSession("sess_1")

    assertFalse(backoff.isBackingOff("sess_1"))
    assertFalse(backoff.isBackingOff("sess_1-custom_template"))
    assertTrue(backoff.isBackingOff("sess_2"))

    // The schedule restarted, so the next failure opens the first window again.
    backoff.recordFailure("sess_1")
    now = 5_000L
    assertFalse(backoff.isBackingOff("sess_1"))
  }

  @Test
  fun `clear forgets every window`() {
    val backoff = SessionTokenBackoff { 0L }

    backoff.recordFailure("sess_1")
    backoff.clear()

    assertFalse(backoff.isBackingOff("sess_1"))
  }
}
