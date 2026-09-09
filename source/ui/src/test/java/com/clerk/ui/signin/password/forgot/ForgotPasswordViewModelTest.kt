package com.clerk.ui.signin.password.forgot

import com.clerk.api.*
import com.clerk.testing.*
import com.clerk.ui.signin.authenticateWithRedirect
import com.clerk.ui.userprofile.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ForgotPasswordViewModelTest {
  private val clerk = mockClerk()

  private val testDispatcher = StandardTestDispatcher()
  @get:Rule val dispatcherRule = MainDispatcherRule(testDispatcher)

  @Before
  fun setUp() {
    mockkStatic("com.clerk.ui.signin.SignInRedirectKt")
  }

  @After
  fun tearDown() {
    unmockkAll()
  }

  @Test
  fun `OAuth cancellation leaves forgot password idle`() =
    runTest(testDispatcher) {
      val signIn = mockSignIn(SignInStatus.NeedsFirstFactor)
      coEvery { authenticateWithRedirect(clerk, OAuthProvider.Github, true) } throws
        CoreException("user_cancelled")
      val viewModel = ForgotPasswordViewModel(clerk)

      viewModel.signInWithProvider(OAuthProvider.Github, signIn = signIn)
      advanceUntilIdle()

      assertEquals(ResetPasswordViewState.Idle, viewModel.state.value)
    }

  @Test
  fun `missing sign in returns to auth start`() =
    runTest(testDispatcher) {
      val viewModel = ForgotPasswordViewModel(clerk)

      viewModel.signInWithProvider(OAuthProvider.Github, signIn = null)
      advanceUntilIdle()

      assertEquals(ResetPasswordViewState.NotStarted, viewModel.state.value)
      coVerify(exactly = 0) { authenticateWithRedirect(any(), any(), any()) }
    }
}
