package com.clerk.ui.auth

import androidx.compose.material3.SnackbarHostState
import androidx.compose.ui.test.junit4.v2.createComposeRule
import io.mockk.mockk
import io.mockk.verify
import kotlin.test.Test
import kotlin.test.assertEquals
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AuthStateEffectsTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun errorStateShowsMessageAndIsResetImmediately() {
    var resetCalls = 0
    val authState = mockk<AuthState>(relaxed = true)
    val snackbarHostState = SnackbarHostState()
    // Don't let the clock run the snackbar's display timeout out before we inspect it.
    composeTestRule.mainClock.autoAdvance = false

    composeTestRule.setContent {
      AuthStateEffects(
        authState = authState,
        state = AuthenticationViewState.Error("Invalid password"),
        snackbarHostState = snackbarHostState,
        onAuthComplete = {},
        onReset = { resetCalls++ },
      )
    }
    composeTestRule.mainClock.advanceTimeByFrame()

    assertEquals(1, resetCalls)
    assertEquals("Invalid password", snackbarHostState.currentSnackbarData?.visuals?.message)
    verify(exactly = 0) { authState.clearBackStack() }
  }
}
