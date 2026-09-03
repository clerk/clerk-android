package com.clerk.ui.signin.password.forgot

import com.clerk.api.network.serialization.ClerkResult
import com.clerk.api.signin.SignIn
import com.clerk.api.sso.OAuthProvider
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
      val signIn = SignIn(id = "sign_in_existing", status = SignIn.Status.NEEDS_FIRST_FACTOR)
      coEvery { authenticateWithRedirect(signIn, OAuthProvider.GITHUB, true) } returns
        ClerkResult.unknownFailure(ssoCancellation())
      val viewModel = ForgotPasswordViewModel(ioDispatcher = testDispatcher)

      viewModel.signInWithProvider(OAuthProvider.GITHUB, signIn = signIn)
      advanceUntilIdle()

      assertEquals(ResetPasswordViewState.Idle, viewModel.state.value)
    }

  @Test
  fun `missing sign in returns to auth start`() =
    runTest(testDispatcher) {
      val viewModel = ForgotPasswordViewModel(ioDispatcher = testDispatcher)

      viewModel.signInWithProvider(OAuthProvider.GITHUB, signIn = null)
      advanceUntilIdle()

      assertEquals(ResetPasswordViewState.NotStarted, viewModel.state.value)
      coVerify(exactly = 0) { authenticateWithRedirect(any(), any(), any()) }
    }

  private fun ssoCancellation(): Throwable =
    Class.forName("com.clerk.api.sso.SSOCancellationException")
      .getDeclaredConstructor(String::class.java)
      .apply { isAccessible = true }
      .newInstance("Authentication cancelled") as Throwable
}
