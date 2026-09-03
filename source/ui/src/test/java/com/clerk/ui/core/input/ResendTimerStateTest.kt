package com.clerk.ui.core.input

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class ResendTimerStateTest {

  @Test
  fun countdownRestartsAfterEachResend() = runTest {
    val timer = ResendTimerState(durationSeconds = 2)

    timer.countDown()
    assertEquals(0, timer.secondsRemaining)

    repeat(2) { resendCount ->
      timer.restart()
      assertEquals(resendCount + 1, timer.restartCount)
      assertEquals(2, timer.secondsRemaining)

      timer.countDown()
      assertEquals(0, timer.secondsRemaining)
    }
  }
}
