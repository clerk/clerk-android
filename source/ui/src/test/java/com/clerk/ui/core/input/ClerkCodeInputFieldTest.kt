package com.clerk.ui.core.input

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ClerkCodeInputFieldTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun changingTimerDurationStartsNewCountdown() {
    composeTestRule.mainClock.autoAdvance = false
    var timerDuration by mutableIntStateOf(2)

    composeTestRule.setContent {
      ClerkCodeInputField(onTextChange = {}, onClickResend = {}, timerDuration = timerDuration)
    }

    composeTestRule.mainClock.advanceTimeByFrame()
    composeTestRule.onNodeWithText("Didn't receive a code? Resend (2)").assertExists()

    composeTestRule.runOnUiThread { timerDuration = 3 }
    composeTestRule.mainClock.advanceTimeByFrame()
    composeTestRule.onNodeWithText("Didn't receive a code? Resend (3)").assertExists()

    composeTestRule.mainClock.advanceTimeBy(1_000)
    composeTestRule.onNodeWithText("Didn't receive a code? Resend (2)").assertExists()
  }
}
