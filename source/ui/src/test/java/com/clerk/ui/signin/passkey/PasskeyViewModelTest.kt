package com.clerk.ui.signin.passkey

import app.cash.turbine.test
import com.clerk.api.*
import com.clerk.testing.*
import com.clerk.ui.auth.AuthenticationViewState
import com.clerk.ui.userprofile.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest

@OptIn(ExperimentalCoroutinesApi::class)
class PasskeyViewModelTest {
  private val clerk = mockClerk()
  private val signIn = mockSignIn(SignInStatus.NeedsFirstFactor)

  @get:org.junit.Rule val dispatcherRule = MainDispatcherRule()

  @BeforeTest
  fun setUp() {
    every { clerk.signIn } returns signIn
  }

  @AfterTest
  fun tearDown() {
    unmockkAll()
  }

  @Test
  fun authenticate_cancellation_resets_to_idle() = runTest {
    coEvery { signIn.passkey(any()) } throws CoreException("user_cancelled")

    val viewModel = PasskeyViewModel(clerk)
    viewModel.state.test {
      assertEquals(AuthenticationViewState.Idle, awaitItem())
      viewModel.authenticate()
      assertEquals(AuthenticationViewState.Loading, awaitItem())
      assertEquals(AuthenticationViewState.Idle, awaitItem())
    }
  }

  @Test
  fun authenticate_missing_activity_surfaces_retry_message() = runTest {
    coEvery { signIn.passkey(any()) } throws
      CoreException(
        "missing_activity",
        "Authentication requires an active screen. Try again from the app.",
      )

    val viewModel = PasskeyViewModel(clerk)
    viewModel.state.test {
      assertEquals(AuthenticationViewState.Idle, awaitItem())
      viewModel.authenticate()
      assertEquals(AuthenticationViewState.Loading, awaitItem())
      assertEquals(
        AuthenticationViewState.Error(
          "Authentication requires an active screen. Try again from the app."
        ),
        awaitItem(),
      )
    }
  }

  @Test
  fun authenticate_success_emits_sign_in_success() = runTest {
    coEvery { signIn.passkey(any()) } returns mockk(relaxed = true)

    val viewModel = PasskeyViewModel(clerk)
    viewModel.state.test {
      assertEquals(AuthenticationViewState.Idle, awaitItem())
      viewModel.authenticate()
      assertEquals(AuthenticationViewState.Loading, awaitItem())
      assertEquals(AuthenticationViewState.Success.SignIn(signIn), awaitItem())
    }
  }
}
