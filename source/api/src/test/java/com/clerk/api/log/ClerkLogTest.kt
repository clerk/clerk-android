package com.clerk.api.log

import android.util.Log
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ClerkLogTest {
  @After
  fun tearDown() {
    ClerkLog.setDebugLoggingEnabled(false)
    unmockkAll()
  }

  @Test
  fun `debug logs are suppressed when debug mode is disabled`() {
    ClerkLog.setDebugLoggingEnabled(false)
    mockkStatic(Log::class)
    every { Log.d(any(), any()) } returns 1

    val result = ClerkLog.d("debug message")

    assertEquals(0, result)
    verify(exactly = 0) { Log.d(any(), any()) }
  }

  @Test
  fun `debug logs are emitted when debug mode is enabled`() {
    ClerkLog.setDebugLoggingEnabled(true)
    mockkStatic(Log::class)
    every { Log.d(any(), any()) } returns 1

    val result = ClerkLog.d("debug message")

    assertEquals(1, result)
    verify(exactly = 1) { Log.d("ClerkLog", "debug message") }
  }

  @Test
  fun `verbose logs are suppressed when debug mode is disabled`() {
    ClerkLog.setDebugLoggingEnabled(false)
    mockkStatic(Log::class)
    every { Log.v(any(), any()) } returns 1

    val result = ClerkLog.v("verbose message")

    assertEquals(0, result)
    verify(exactly = 0) { Log.v(any(), any()) }
  }

  @Test
  fun `verbose logs are emitted when debug mode is enabled`() {
    ClerkLog.setDebugLoggingEnabled(true)
    mockkStatic(Log::class)
    every { Log.v(any(), any()) } returns 1

    val result = ClerkLog.v("verbose message")

    assertEquals(1, result)
    verify(exactly = 1) { Log.v("ClerkLog", "verbose message") }
  }
}
