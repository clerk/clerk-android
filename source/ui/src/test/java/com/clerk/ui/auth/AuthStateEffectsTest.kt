package com.clerk.ui.auth

import androidx.compose.material3.SnackbarHostState
import com.clerk.base.BaseSnapshotTest
import io.mockk.mockk
import kotlin.test.Test
import kotlin.test.assertEquals

class AuthStateEffectsTest : BaseSnapshotTest() {

  @Test
  fun errorStateIsResetImmediately() {
    var resetCalls = 0
    val authState = mockk<AuthState>(relaxed = true)
    val snackbarHostState = SnackbarHostState()

    paparazzi.snapshot {
      AuthStateEffects(
        authState = authState,
        state = AuthenticationViewState.Error("Invalid password"),
        snackbarHostState = snackbarHostState,
        onAuthComplete = {},
        onReset = { resetCalls++ },
      )
    }

    assertEquals(1, resetCalls)
  }
}
