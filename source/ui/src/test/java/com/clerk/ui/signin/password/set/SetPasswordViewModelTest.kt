package com.clerk.ui.signin.password.set

import com.clerk.api.Clerk
import com.clerk.api.auth.Auth
import com.clerk.api.auth.types.Strategy
import com.clerk.api.network.model.error.ClerkErrorResponse
import com.clerk.api.network.model.error.Error
import com.clerk.api.network.model.factor.Factor
import com.clerk.api.network.serialization.ClerkResult
import com.clerk.api.signin.SignIn
import com.clerk.api.signin.verifyWithPassword
import com.clerk.ui.auth.AuthenticationViewState
import com.clerk.ui.userprofile.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
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
class SetPasswordViewModelTest {

  private val testDispatcher = StandardTestDispatcher()
  @get:Rule val dispatcherRule = MainDispatcherRule(testDispatcher)
  private val auth = mockk<Auth>(relaxed = true)

  @Before
  fun setUp() {
    mockkObject(Clerk)
    every { Clerk.auth } returns auth
    mockkStatic("com.clerk.api.signin.SignInExtensionsKt")
  }

  @After
  fun tearDown() {
    unmockkAll()
  }

  @Test
  fun `password reset required opens the password reset required screen`() =
    runTest(testDispatcher) {
      val signIn = signIn(canResetPassword = true)
      every { auth.currentSignIn } returns signIn
      coEvery { signIn.verifyWithPassword(PASSWORD) } returns failure("password_reset_required")

      val viewModel = SetPasswordViewModel(ioDispatcher = testDispatcher)
      viewModel.submitPassword(PASSWORD)
      advanceUntilIdle()

      assertEquals(AuthenticationViewState.PasswordResetRequired, viewModel.state.value)
    }

  @Test
  fun `password reset required shows the error when the password cannot be reset`() =
    runTest(testDispatcher) {
      val signIn = signIn(canResetPassword = false)
      every { auth.currentSignIn } returns signIn
      coEvery { signIn.verifyWithPassword(PASSWORD) } returns failure("password_reset_required")

      val viewModel = SetPasswordViewModel(ioDispatcher = testDispatcher)
      viewModel.submitPassword(PASSWORD)
      advanceUntilIdle()

      assertEquals(AuthenticationViewState.Error(LONG_MESSAGE), viewModel.state.value)
    }

  @Test
  fun `other password errors show the error`() =
    runTest(testDispatcher) {
      val signIn = signIn(canResetPassword = true)
      every { auth.currentSignIn } returns signIn
      coEvery { signIn.verifyWithPassword(PASSWORD) } returns failure("form_password_incorrect")

      val viewModel = SetPasswordViewModel(ioDispatcher = testDispatcher)
      viewModel.submitPassword(PASSWORD)
      advanceUntilIdle()

      assertEquals(AuthenticationViewState.Error(LONG_MESSAGE), viewModel.state.value)
    }

  private fun signIn(canResetPassword: Boolean): SignIn {
    val password = Factor(strategy = Strategy.Password.value)
    val resetPassword = Factor(strategy = Strategy.ResetPasswordEmailCode.value)
    return SignIn(
      id = "sign_in_password",
      status = SignIn.Status.NEEDS_FIRST_FACTOR,
      supportedFirstFactors =
        if (canResetPassword) listOf(password, resetPassword) else listOf(password),
    )
  }

  private fun failure(code: String): ClerkResult.Failure<ClerkErrorResponse> =
    ClerkResult.apiFailure(
      ClerkErrorResponse(errors = listOf(Error(longMessage = LONG_MESSAGE, code = code)))
    )

  private companion object {
    const val PASSWORD = "correct-horse"
    const val LONG_MESSAGE = "The API's message"
  }
}
