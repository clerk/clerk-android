package com.clerk.api.log

import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test

@Suppress("DEPRECATION")
class ClerkLogTest {
  @After
  fun tearDown() {
    unmockkAll()
  }

  @Test
  fun `deprecated ClerkLog forwards to the internal logger`() {
    mockkObject(ClerkLogger)
    every { ClerkLogger.e(any()) } returns 1
    every { ClerkLogger.w(any()) } returns 2
    every { ClerkLogger.i(any()) } returns 3
    every { ClerkLogger.d(any()) } returns 4
    every { ClerkLogger.v(any()) } returns 5

    assertEquals(1, ClerkLog.e("error"))
    assertEquals(2, ClerkLog.w("warning"))
    assertEquals(3, ClerkLog.i("info"))
    assertEquals(4, ClerkLog.d("debug"))
    assertEquals(5, ClerkLog.v("verbose"))
    verify(exactly = 1) {
      ClerkLogger.e("error")
      ClerkLogger.w("warning")
      ClerkLogger.i("info")
      ClerkLogger.d("debug")
      ClerkLogger.v("verbose")
    }
  }
}
