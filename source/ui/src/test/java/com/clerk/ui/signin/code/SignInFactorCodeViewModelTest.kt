@file:Suppress("UNCHECKED_CAST")

package com.clerk.ui.signin.code

import app.cash.turbine.test
import com.clerk.api.Clerk
import com.clerk.api.SignIn
import com.clerk.api.VerificationStatus
import com.clerk.testing.*
import com.clerk.ui.auth.AuthenticationViewState
import com.clerk.ui.auth.FactorSelection
import com.clerk.ui.core.common.StrategyKeys
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import java.time.Instant
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
  private val clerk = mockk<Clerk>(relaxed = true)

  private val mockAttemptHandler = mockk<SignInAttemptHandler>(relaxed = true)
  private val mockPrepareHandler = mockk<SignInPrepareHandler>(relaxed = true)
  private val mockSignIn = mockk<SignIn>(relaxed = true)
  private val testDispatcher = StandardTestDispatcher()

  private lateinit var viewModel: SignInFactorCodeViewModel

  @Before
  fun setUp() {
    Dispatchers.setMain(testDispatcher)
    every { clerk.signIn } returns mockSignIn
    every { mockSignIn.id } returns "sign_in_123"
    every { mockSignIn.firstFactorVerification } returns mockVerification()
    every { mockSignIn.secondFactorVerification } returns mockVerification()
    every { mockSignIn.identifier } returns "user@example.com"
    every { mockSignIn.supportedFirstFactors } returns
      listOf(
          FactorSelection(strategy = StrategyKeys.EMAIL_CODE, emailAddressId = "email_123"),
          FactorSelection(strategy = StrategyKeys.PHONE_CODE, phoneNumberId = "phone_456"),
        )
        .map(::firstFactor)
    every { mockSignIn.supportedSecondFactors } returns
      listOf(
          FactorSelection(strategy = StrategyKeys.EMAIL_CODE, emailAddressId = "email_123"),
          FactorSelection(strategy = StrategyKeys.PHONE_CODE, phoneNumberId = "phone_456"),
          FactorSelection(strategy = StrategyKeys.TOTP),
        )
        .map(::secondFactor)

    viewModel =
      SignInFactorCodeViewModel(
        clerk = clerk,
        attemptHandler = mockAttemptHandler,
        prepareHandler = mockPrepareHandler,
        workDispatcher = testDispatcher,
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
    every { mockSignIn.id } returns null
    val factor = FactorSelection(strategy = StrategyKeys.EMAIL_CODE)

    viewModel.prepare(factor, isSecondFactor = false)
    testDispatcher.scheduler.advanceUntilIdle()

    viewModel.state.test { assertEquals(AuthenticationViewState.NotStarted, awaitItem()) }
  }

  @Test
  fun attemptWithEmailCodeStrategyShouldCallAttemptEmailCodeAndSetSuccessState() = runTest {
    val factor = FactorSelection(strategy = StrategyKeys.EMAIL_CODE)
    val code = "123456"

    coEvery {
      mockAttemptHandler.attemptEmailCode(
        inProgressSignIn = mockSignIn,
        code = code,
        isSecondFactor = false,
        onSuccessCallback = any(),
        onErrorCallback = any(),
      )
    } coAnswers
      {
        val onSuccess = args[3] as suspend (SignIn) -> Unit
        onSuccess(mockSignIn)
      }

    viewModel.attempt(factor, isSecondFactor = false, code)
    testDispatcher.scheduler.advanceUntilIdle()

    coVerify {
      mockAttemptHandler.attemptEmailCode(
        inProgressSignIn = mockSignIn,
        code = code,
        isSecondFactor = false,
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
      val factor = FactorSelection(strategy = StrategyKeys.PHONE_CODE)
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
    val factor = FactorSelection(strategy = StrategyKeys.RESET_PASSWORD_EMAIL_CODE)
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

    viewModel.state.test {
      assertEquals(AuthenticationViewState.Idle, awaitItem())

      viewModel.attempt(factor, isSecondFactor = false, code)
      testDispatcher.scheduler.advanceUntilIdle()

      assertEquals(AuthenticationViewState.Loading, awaitItem())
      assertEquals(AuthenticationViewState.Success.SignIn(mockSignIn), awaitItem())
    }

    coVerify {
      mockAttemptHandler.attemptResetForEmailCode(
        inProgressSignIn = mockSignIn,
        code = code,
        onSuccessCallback = any(),
        onErrorCallback = any(),
      )
    }
  }

  @Test
  fun attemptWithResetPasswordPhoneCodeStrategyShouldCallAttemptResetForPhoneCode() = runTest {
    val factor = FactorSelection(strategy = StrategyKeys.RESET_PASSWORD_PHONE_CODE)
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

    viewModel.state.test {
      assertEquals(AuthenticationViewState.Idle, awaitItem())

      viewModel.attempt(factor, isSecondFactor = false, code)
      testDispatcher.scheduler.advanceUntilIdle()

      assertEquals(AuthenticationViewState.Loading, awaitItem())
      assertEquals(AuthenticationViewState.Success.SignIn(mockSignIn), awaitItem())
    }

    coVerify {
      mockAttemptHandler.attemptResetForPhoneCode(
        inProgressSignIn = mockSignIn,
        code = code,
        onSuccessCallback = any(),
        onErrorCallback = any(),
      )
    }
  }

  @Test
  fun attemptWithTotpStrategyShouldCallAttemptForTotp() = runTest {
    val factor = FactorSelection(strategy = StrategyKeys.TOTP)
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

    viewModel.state.test {
      assertEquals(AuthenticationViewState.Idle, awaitItem())

      viewModel.attempt(factor, isSecondFactor = true, code)
      testDispatcher.scheduler.advanceUntilIdle()

      assertEquals(AuthenticationViewState.Loading, awaitItem())
      assertEquals(AuthenticationViewState.Success.SignIn(mockSignIn), awaitItem())
    }

    coVerify {
      mockAttemptHandler.attemptForTotp(
        inProgressSignIn = mockSignIn,
        code = code,
        onSuccessCallback = any(),
        onErrorCallback = any(),
      )
    }
  }

  @Test
  fun attemptShouldSetErrorStateWhenHandlerCallsOnErrorCallback() = runTest {
    val factor = FactorSelection(strategy = StrategyKeys.EMAIL_CODE)
    val code = "123456"

    coEvery {
      mockAttemptHandler.attemptEmailCode(
        inProgressSignIn = mockSignIn,
        code = code,
        isSecondFactor = false,
        onSuccessCallback = any(),
        onErrorCallback = any(),
      )
    } coAnswers
      {
        val onError = args[4] as suspend (String?) -> Unit
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
    every { mockSignIn.id } returns null
    val factor = FactorSelection(strategy = StrategyKeys.EMAIL_CODE)
    val code = "123456"

    viewModel.attempt(factor, isSecondFactor = false, code)
    testDispatcher.scheduler.advanceUntilIdle()

    viewModel.state.test { assertEquals(AuthenticationViewState.NotStarted, awaitItem()) }
  }

  @Test
  fun stateShouldTransitionFromIdleToVerifyingDuringPrepare() = runTest {
    val factor = FactorSelection(strategy = StrategyKeys.EMAIL_CODE, emailAddressId = "email_id")

    viewModel.state.test {
      assertEquals(AuthenticationViewState.Idle, awaitItem())

      viewModel.prepare(factor, isSecondFactor = false)

      assertEquals(AuthenticationViewState.Loading, awaitItem())
    }
  }

  @Test
  fun stateShouldTransitionFromIdleToVerifyingDuringAttempt() = runTest {
    val factor = FactorSelection(strategy = StrategyKeys.EMAIL_CODE)
    val code = "123456"

    viewModel.state.test {
      assertEquals(AuthenticationViewState.Idle, awaitItem())

      viewModel.attempt(factor, isSecondFactor = false, code)

      assertEquals(AuthenticationViewState.Loading, awaitItem())
    }
  }

  @Test
  fun prepareShouldRerouteToFirstFactorWhenEmailLinkIsAvailableForEmailIdentifier() = runTest {
    every { mockSignIn.identifier } returns "sam@clerk.dev"
    every { mockSignIn.supportedFirstFactors } returns
      listOf(
          FactorSelection(strategy = StrategyKeys.EMAIL_CODE, emailAddressId = "email_123"),
          FactorSelection(strategy = StrategyKeys.EMAIL_LINK, emailAddressId = "email_123"),
        )
        .map(::firstFactor)
    val factor = FactorSelection(strategy = StrategyKeys.EMAIL_CODE, emailAddressId = "email_123")

    viewModel.state.test {
      assertEquals(AuthenticationViewState.Idle, awaitItem())

      viewModel.prepare(factor, isSecondFactor = false)
      testDispatcher.scheduler.advanceUntilIdle()

      assertEquals(AuthenticationViewState.Loading, awaitItem())
      assertEquals(AuthenticationViewState.Success.SignIn(mockSignIn), awaitItem())
    }

    coVerify(exactly = 0) { mockPrepareHandler.prepareForEmailCode(any(), any(), any(), any()) }
  }

  @Test
  fun prepareShouldRerouteToFirstFactorWhenIdentifierIsMissingButEmailFactorExists() = runTest {
    every { mockSignIn.identifier } returns null
    every { mockSignIn.supportedFirstFactors } returns
      listOf(
          FactorSelection(strategy = StrategyKeys.EMAIL_CODE, emailAddressId = "email_123"),
          FactorSelection(strategy = StrategyKeys.EMAIL_LINK, emailAddressId = "email_123"),
        )
        .map(::firstFactor)
    val factor = FactorSelection(strategy = StrategyKeys.EMAIL_CODE, emailAddressId = "email_123")

    viewModel.state.test {
      assertEquals(AuthenticationViewState.Idle, awaitItem())

      viewModel.prepare(factor, isSecondFactor = false)
      testDispatcher.scheduler.advanceUntilIdle()

      assertEquals(AuthenticationViewState.Loading, awaitItem())
      assertEquals(AuthenticationViewState.Success.SignIn(mockSignIn), awaitItem())
    }

    coVerify(exactly = 0) { mockPrepareHandler.prepareForEmailCode(any(), any(), any(), any()) }
  }

  @Test
  fun prepareShouldNotRerouteWhenEmailCodeIsAlreadyPrepared() = runTest {
    every { mockSignIn.identifier } returns "sam@clerk.dev"
    every { mockSignIn.firstFactorVerification } returns
      mockVerification(status = VerificationStatus.Unverified, strategy = StrategyKeys.EMAIL_CODE)
    every { mockSignIn.supportedFirstFactors } returns
      listOf(
          FactorSelection(strategy = StrategyKeys.EMAIL_CODE, emailAddressId = "email_123"),
          FactorSelection(strategy = StrategyKeys.EMAIL_LINK, emailAddressId = "email_123"),
        )
        .map(::firstFactor)
    val factor = FactorSelection(strategy = StrategyKeys.EMAIL_CODE, emailAddressId = "email_123")

    viewModel.prepare(factor, isSecondFactor = false)
    testDispatcher.scheduler.advanceUntilIdle()

    coVerify(exactly = 1) {
      mockPrepareHandler.prepareForEmailCode(mockSignIn, factor, false, any())
    }
  }

  @Test
  fun prepareShouldReuseActiveFirstFactormockVerification() = runTest {
    every { mockSignIn.firstFactorVerification } returns
      mockVerification(
        status = VerificationStatus.Unverified,
        strategy = StrategyKeys.EMAIL_CODE,
        expireAt = Instant.parse("2030-01-01T00:00:00Z"),
      )
    val factor = FactorSelection(strategy = StrategyKeys.EMAIL_CODE, emailAddressId = "email_123")

    viewModel.prepare(factor, isSecondFactor = false)
    testDispatcher.scheduler.advanceUntilIdle()

    coVerify(exactly = 0) { mockPrepareHandler.prepareForEmailCode(any(), any(), any(), any()) }
    viewModel.state.test { assertEquals(AuthenticationViewState.Idle, awaitItem()) }
  }

  @Test
  fun prepareShouldReuseActiveSecondFactormockVerification() = runTest {
    every { mockSignIn.secondFactorVerification } returns
      mockVerification(
        status = VerificationStatus.Unverified,
        strategy = StrategyKeys.PHONE_CODE,
        expireAt = Instant.parse("2030-01-01T00:00:00Z"),
      )
    val factor = FactorSelection(strategy = StrategyKeys.PHONE_CODE, phoneNumberId = "phone_456")

    viewModel.prepare(factor, isSecondFactor = true)
    testDispatcher.scheduler.advanceUntilIdle()

    coVerify(exactly = 0) { mockPrepareHandler.prepareForPhoneCode(any(), any(), any(), any()) }
  }

  @Test
  fun prepareShouldSendNewCodeWhenVerificationIsExpired() = runTest {
    every { mockSignIn.firstFactorVerification } returns
      mockVerification(
        status = VerificationStatus.Unverified,
        strategy = StrategyKeys.EMAIL_CODE,
        expireAt = Instant.EPOCH,
      )
    val factor = FactorSelection(strategy = StrategyKeys.EMAIL_CODE, emailAddressId = "email_123")

    viewModel.prepare(factor, isSecondFactor = false)
    testDispatcher.scheduler.advanceUntilIdle()

    coVerify(exactly = 1) {
      mockPrepareHandler.prepareForEmailCode(mockSignIn, factor, false, any())
    }
  }

  @Test
  fun prepareShouldSendNewCodeWhenResendIsExplicit() = runTest {
    every { mockSignIn.firstFactorVerification } returns
      mockVerification(
        status = VerificationStatus.Unverified,
        strategy = StrategyKeys.EMAIL_CODE,
        expireAt = Instant.parse("2030-01-01T00:00:00Z"),
      )
    val factor = FactorSelection(strategy = StrategyKeys.EMAIL_CODE, emailAddressId = "email_123")

    viewModel.prepare(factor, isSecondFactor = false, forcePrepare = true)
    testDispatcher.scheduler.advanceUntilIdle()

    coVerify(exactly = 1) {
      mockPrepareHandler.prepareForEmailCode(mockSignIn, factor, false, any())
    }
  }

  @Test
  fun prepareShouldRerouteWhenRequestedFactorIsNoLongerSupported() = runTest {
    every { mockSignIn.identifier } returns null
    every { mockSignIn.supportedFirstFactors } returns
      listOf(FactorSelection(strategy = "password")).map(::firstFactor)
    val factor = FactorSelection(strategy = StrategyKeys.EMAIL_CODE, emailAddressId = "email_123")

    viewModel.state.test {
      assertEquals(AuthenticationViewState.Idle, awaitItem())

      viewModel.prepare(factor, isSecondFactor = false)
      testDispatcher.scheduler.advanceUntilIdle()

      assertEquals(AuthenticationViewState.Loading, awaitItem())
      assertEquals(AuthenticationViewState.Success.SignIn(mockSignIn), awaitItem())
    }

    coVerify(exactly = 0) { mockPrepareHandler.prepareForEmailCode(any(), any(), any(), any()) }
  }

  @Test
  fun prepareShouldRerouteActiveVerificationWhenFactorIsNoLongerSupported() = runTest {
    every { mockSignIn.identifier } returns null
    every { mockSignIn.supportedFirstFactors } returns
      listOf(FactorSelection(strategy = "password")).map(::firstFactor)
    every { mockSignIn.firstFactorVerification } returns
      mockVerification(
        status = VerificationStatus.Unverified,
        strategy = StrategyKeys.EMAIL_CODE,
        expireAt = Instant.parse("2030-01-01T00:00:00Z"),
      )
    val factor = FactorSelection(strategy = StrategyKeys.EMAIL_CODE, emailAddressId = "email_123")

    viewModel.state.test {
      assertEquals(AuthenticationViewState.Idle, awaitItem())

      viewModel.prepare(factor, isSecondFactor = false)
      testDispatcher.scheduler.advanceUntilIdle()

      assertEquals(AuthenticationViewState.Loading, awaitItem())
      assertEquals(AuthenticationViewState.Success.SignIn(mockSignIn), awaitItem())
    }

    coVerify(exactly = 0) { mockPrepareHandler.prepareForEmailCode(any(), any(), any(), any()) }
  }

  @Test
  fun prepareShouldRerouteWhenMatchingStrategyHasDifferentIdentifier() = runTest {
    every { mockSignIn.identifier } returns null
    every { mockSignIn.supportedFirstFactors } returns
      listOf(FactorSelection(strategy = StrategyKeys.EMAIL_CODE, emailAddressId = "email_456"))
        .map(::firstFactor)
    val factor = FactorSelection(strategy = StrategyKeys.EMAIL_CODE, emailAddressId = "email_123")

    viewModel.state.test {
      assertEquals(AuthenticationViewState.Idle, awaitItem())

      viewModel.prepare(factor, isSecondFactor = false)
      testDispatcher.scheduler.advanceUntilIdle()

      assertEquals(AuthenticationViewState.Loading, awaitItem())
      assertEquals(AuthenticationViewState.Success.SignIn(mockSignIn), awaitItem())
    }

    coVerify(exactly = 0) { mockPrepareHandler.prepareForEmailCode(any(), any(), any(), any()) }
  }
}
