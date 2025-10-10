@file:Suppress("UNCHECKED_CAST")

package com.clerk.ui.signin.code

import app.cash.turbine.test
import com.clerk.api.Clerk
import com.clerk.api.network.model.factor.Factor
import com.clerk.api.signin.SignIn
import com.clerk.ui.auth.AuthenticationViewState
import com.clerk.ui.core.common.StrategyKeys
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/**
 * Comprehensive test suite for SignInFactorCodeViewModel covering:
 * - State management (Idle, Verifying, Success, Error transitions)
 * - Prepare operations for different factor strategies (EMAIL_CODE, PHONE_CODE, etc.)
 * - Attempt operations for different factor strategies and success/error scenarios
 * - Error handling for missing sign-in context
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SignInFactorCodeViewModelTest {

  private val mockAttemptHandler = mockk<SignInAttemptHandler>(relaxed = true)
  private val mockPrepareHandler = mockk<SignInPrepareHandler>(relaxed = true)
  private val mockSignIn = mockk<SignIn>(relaxed = true)
  private val testDispatcher = StandardTestDispatcher()

  private lateinit var viewModel: SignInFactorCodeViewModel

  @Before
  fun setUp() {
    Dispatchers.setMain(testDispatcher)
    mockkObject(Clerk)
    every { Clerk.signIn } returns mockSignIn

    viewModel =
      SignInFactorCodeViewModel(
        attemptHandler = mockAttemptHandler,
        prepareHandler = mockPrepareHandler,
      )
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
    unmockkAll()
  }

  @Test
  fun initialStateShouldBeIdle() = runTest {
    viewModel.state.test { assertEquals(AuthenticationViewState.Idle, awaitItem()) }
  }

  @Test
  fun prepareShouldThrowErrorWhenNoSignInIsInProgress() = runTest {
    every { Clerk.signIn } returns null
    val factor = Factor(strategy = StrategyKeys.EMAIL_CODE)

    viewModel.prepare(factor, isSecondFactor = false)
    testDispatcher.scheduler.advanceUntilIdle()

    viewModel.state.test { assertEquals(AuthenticationViewState.NotStarted, awaitItem()) }
  }

  @Test
  fun attemptWithEmailCodeStrategyShouldCallAttemptFirstFactorEmailCodeAndSetSuccessState() =
    runTest {
      val factor = Factor(strategy = StrategyKeys.EMAIL_CODE)
      val code = "123456"

      coEvery {
        mockAttemptHandler.attemptFirstFactorEmailCode(
          inProgressSignIn = mockSignIn,
          code = code,
          onSuccessCallback = any(),
          onErrorCallback = any(),
        )
      } coAnswers
        {
          val onSuccess = args[2] as suspend (SignIn) -> Unit
          onSuccess(mockSignIn)
        }

      viewModel.attempt(factor, isSecondFactor = false, code)
      testDispatcher.scheduler.advanceUntilIdle()

      coVerify {
        mockAttemptHandler.attemptFirstFactorEmailCode(
          inProgressSignIn = mockSignIn,
          code = code,
          onSuccessCallback = any(),
          onErrorCallback = any(),
        )
      }
      viewModel.state.test {
        assertEquals(AuthenticationViewState.Success.SignIn(mockSignIn), awaitItem())
      }
    }

  @Test
  fun attemptWithPhoneCodeStrategyShouldCallAttemptFirstFactorPhoneCodeAndSetSuccessState() =
    runTest {
      val factor = Factor(strategy = StrategyKeys.PHONE_CODE)
      val code = "654321"
      val isSecondFactor = true

      coEvery {
        mockAttemptHandler.attemptFirstFactorPhoneCode(
          inProgressSignIn = mockSignIn,
          code = code,
          isSecondFactor = isSecondFactor,
          onSuccessCallback = any(),
          onErrorCallback = any(),
        )
      } coAnswers
        {
          val onSuccess = args[3] as suspend (SignIn) -> Unit
          onSuccess(mockSignIn)
        }

      viewModel.attempt(factor, isSecondFactor, code)
      testDispatcher.scheduler.advanceUntilIdle()

      coVerify {
        mockAttemptHandler.attemptFirstFactorPhoneCode(
          inProgressSignIn = mockSignIn,
          code = code,
          isSecondFactor = isSecondFactor,
          onSuccessCallback = any(),
          onErrorCallback = any(),
        )
      }
      viewModel.state.test {
        assertEquals(AuthenticationViewState.Success.SignIn(mockSignIn), awaitItem())
      }
    }

  @Test
  fun attemptWithResetPasswordEmailCodeStrategyShouldCallAttemptResetForEmailCode() = runTest {
    val factor = Factor(strategy = StrategyKeys.RESET_PASSWORD_EMAIL_CODE)
    val code = "789012"

    coEvery {
      mockAttemptHandler.attemptResetForEmailCode(
        inProgressSignIn = mockSignIn,
        code = code,
        onSuccessCallback = any(),
        onErrorCallback = any(),
      )
    } coAnswers
      {
        val onSuccess = args[2] as suspend (SignIn) -> Unit
        onSuccess(mockSignIn)
      }

    viewModel.attempt(factor, isSecondFactor = false, code)
    testDispatcher.scheduler.advanceUntilIdle()

    coVerify {
      mockAttemptHandler.attemptResetForEmailCode(
        inProgressSignIn = mockSignIn,
        code = code,
        onSuccessCallback = any(),
        onErrorCallback = any(),
      )
    }
    viewModel.state.test {
      assertEquals(AuthenticationViewState.Success.SignIn(mockSignIn), awaitItem())
    }
  }

  @Test
  fun attemptWithResetPasswordPhoneCodeStrategyShouldCallAttemptResetForPhoneCode() = runTest {
    val factor = Factor(strategy = StrategyKeys.RESET_PASSWORD_PHONE_CODE)
    val code = "345678"

    coEvery {
      mockAttemptHandler.attemptResetForPhoneCode(
        inProgressSignIn = mockSignIn,
        code = code,
        onSuccessCallback = any(),
        onErrorCallback = any(),
      )
    } coAnswers
      {
        val onSuccess = args[2] as suspend (SignIn) -> Unit
        onSuccess(mockSignIn)
      }

    viewModel.attempt(factor, isSecondFactor = false, code)
    testDispatcher.scheduler.advanceUntilIdle()

    coVerify {
      mockAttemptHandler.attemptResetForPhoneCode(
        inProgressSignIn = mockSignIn,
        code = code,
        onSuccessCallback = any(),
        onErrorCallback = any(),
      )
    }
    viewModel.state.test {
      assertEquals(AuthenticationViewState.Success.SignIn(mockSignIn), awaitItem())
    }
  }

  @Test
  fun attemptWithTotpStrategyShouldCallAttemptForTotp() = runTest {
    val factor = Factor(strategy = StrategyKeys.TOTP)
    val code = "901234"

    coEvery {
      mockAttemptHandler.attemptForTotp(
        inProgressSignIn = mockSignIn,
        code = code,
        onSuccessCallback = any(),
        onErrorCallback = any(),
      )
    } coAnswers
      {
        val onSuccess = args[2] as suspend (SignIn) -> Unit
        onSuccess(mockSignIn)
      }

    viewModel.attempt(factor, isSecondFactor = true, code)
    testDispatcher.scheduler.advanceUntilIdle()

    coVerify {
      mockAttemptHandler.attemptForTotp(
        inProgressSignIn = mockSignIn,
        code = code,
        onSuccessCallback = any(),
        onErrorCallback = any(),
      )
    }
    viewModel.state.test {
      assertEquals(AuthenticationViewState.Success.SignIn(mockSignIn), awaitItem())
    }
  }

  @Test
  fun attemptShouldSetErrorStateWhenHandlerCallsOnErrorCallback() = runTest {
    val factor = Factor(strategy = StrategyKeys.EMAIL_CODE)
    val code = "123456"

    coEvery {
      mockAttemptHandler.attemptFirstFactorEmailCode(
        inProgressSignIn = mockSignIn,
        code = code,
        onSuccessCallback = any(),
        onErrorCallback = any(),
      )
    } coAnswers
      {
        val onError = args[3] as suspend (String?) -> Unit
        onError("error")
      }

    viewModel.state.test {
      assertEquals(AuthenticationViewState.Idle, awaitItem())

      viewModel.attempt(factor, isSecondFactor = false, code)

      // First, the view model emits Loading; then, error from the handler
      assertEquals(AuthenticationViewState.Loading, awaitItem())
      assertEquals(AuthenticationViewState.Error("error"), awaitItem())
    }
  }

  @Test
  fun attemptShouldThrowErrorWhenNoSignInIsInProgress() = runTest {
    every { Clerk.signIn } returns null
    val factor = Factor(strategy = StrategyKeys.EMAIL_CODE)
    val code = "123456"

    viewModel.attempt(factor, isSecondFactor = false, code)
    testDispatcher.scheduler.advanceUntilIdle()

    viewModel.state.test { assertEquals(AuthenticationViewState.NotStarted, awaitItem()) }
  }

  @Test
  fun stateShouldTransitionFromIdleToVerifyingDuringPrepare() = runTest {
    val factor = Factor(strategy = StrategyKeys.EMAIL_CODE, emailAddressId = "email_id")

    viewModel.state.test {
      assertEquals(AuthenticationViewState.Idle, awaitItem())

      viewModel.prepare(factor, isSecondFactor = false)

      assertEquals(AuthenticationViewState.Loading, awaitItem())
    }
  }

  @Test
  fun stateShouldTransitionFromIdleToVerifyingDuringAttempt() = runTest {
    val factor = Factor(strategy = StrategyKeys.EMAIL_CODE)
    val code = "123456"

    viewModel.state.test {
      assertEquals(AuthenticationViewState.Idle, awaitItem())

      viewModel.attempt(factor, isSecondFactor = false, code)

      assertEquals(AuthenticationViewState.Loading, awaitItem())
    }
  }
}
