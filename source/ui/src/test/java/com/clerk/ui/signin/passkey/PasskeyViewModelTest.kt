package com.clerk.ui.signin.passkey

import app.cash.turbine.test
import com.clerk.api.credentials.CredentialFlowException
import com.clerk.api.network.serialization.ClerkResult
import com.clerk.api.signin.SignIn
import com.clerk.ui.auth.AuthenticationViewState
import com.clerk.ui.userprofile.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest

@OptIn(ExperimentalCoroutinesApi::class)
class PasskeyViewModelTest {

  @get:org.junit.Rule val dispatcherRule = MainDispatcherRule()

  @BeforeTest
  fun setUp() {
    mockkObject(SignIn.Companion)
  }

  @AfterTest
  fun tearDown() {
    unmockkAll()
  }

  @Test
  fun authenticate_cancellation_resets_to_idle() = runTest {
    coEvery { SignIn.authenticateWithGoogleCredential(any()) } returns
      ClerkResult.unknownFailure(CredentialFlowException.UserCancelled())

    val viewModel = PasskeyViewModel()
    viewModel.state.test {
      assertEquals(AuthenticationViewState.Idle, awaitItem())
      viewModel.authenticate()
      assertEquals(AuthenticationViewState.Loading, awaitItem())
      assertEquals(AuthenticationViewState.Idle, awaitItem())
    }
  }

  @Test
  fun authenticate_missing_activity_surfaces_retry_message() = runTest {
    coEvery { SignIn.authenticateWithGoogleCredential(any()) } returns
      ClerkResult.unknownFailure(CredentialFlowException.MissingActivity())

    val viewModel = PasskeyViewModel()
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
    val signIn = mockk<SignIn>(relaxed = true)
    coEvery { SignIn.authenticateWithGoogleCredential(any()) } returns ClerkResult.success(signIn)

    val viewModel = PasskeyViewModel()
    viewModel.state.test {
      assertEquals(AuthenticationViewState.Idle, awaitItem())
      viewModel.authenticate()
      assertEquals(AuthenticationViewState.Loading, awaitItem())
      assertEquals(AuthenticationViewState.Success.SignIn(signIn), awaitItem())
    }
  }
}
