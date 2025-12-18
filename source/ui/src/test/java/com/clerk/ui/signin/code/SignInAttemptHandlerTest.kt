package com.clerk.ui.signin.code

import com.clerk.api.log.ClerkLog
import com.clerk.api.network.model.error.ClerkErrorResponse
import com.clerk.api.network.serialization.ClerkResult
import com.clerk.api.signin.SignIn
import com.clerk.api.signin.attemptFirstFactor
import com.clerk.api.signin.attemptSecondFactor
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test

/**
 * Comprehensive test suite for SignInAttemptHandler covering:
 * - Email code attempts for both regular and password reset scenarios
 * - Phone code attempts for first and second factor scenarios
 * - TOTP attempts for two-factor authentication
 * - Password reset attempts for phone and email
 * - Success and failure callback handling for all attempt methods
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SignInAttemptHandlerTest {

  private val mockSignIn = mockk<SignIn>(relaxed = true)
  private val handler = SignInAttemptHandler()

  @Before
  fun setUp() {
    mockkStatic("com.clerk.api.signin.SignInKt")
    mockkObject(ClerkLog)
    every { ClerkLog.e(any()) } returns 0
  }

  @After
  fun tearDown() {
    unmockkAll()
  }

  @Test
  fun attemptFirstFactorEmailCodeShouldCallAttemptFirstFactorAndTriggerSuccessCallback() = runTest {
    val code = "123456"
    val successResult = ClerkResult.success(mockSignIn)
    var successCallbackCalled = false
    var errorCallbackCalled = false

    coEvery {
      mockSignIn.attemptFirstFactor(SignIn.AttemptFirstFactorParams.EmailCode(code = code))
    } returns successResult

    handler.attemptFirstFactorEmailCode(
      inProgressSignIn = mockSignIn,
      code = code,
      onSuccessCallback = { successCallbackCalled = true },
      onErrorCallback = { errorCallbackCalled = true },
    )

    coVerify { mockSignIn.attemptFirstFactor(any()) }
    assert(successCallbackCalled)
    assert(!errorCallbackCalled)
  }

  @Test
  fun attemptEmailCodeAsSecondFactorShouldCallAttemptSecondFactorAndTriggerSuccessCallback() =
    runTest {
      val code = "123456"
      val successResult = ClerkResult.success(mockSignIn)
      var successCallbackCalled = false
      var errorCallbackCalled = false

      coEvery {
        mockSignIn.attemptSecondFactor(SignIn.AttemptSecondFactorParams.EmailCode(code = code))
      } returns successResult

      handler.attemptEmailCode(
        inProgressSignIn = mockSignIn,
        code = code,
        useSecondFactorApi = true,
        onSuccessCallback = { successCallbackCalled = true },
        onErrorCallback = { errorCallbackCalled = true },
      )

      coVerify { mockSignIn.attemptSecondFactor(any()) }
      assert(successCallbackCalled)
      assert(!errorCallbackCalled)
    }

  @Test
  fun attemptFirstFactorEmailCodeShouldTriggerErrorCallbackOnFailure() = runTest {
    val code = "123456"
    val errorResponse = ClerkErrorResponse(errors = emptyList())
    val failureResult = ClerkResult.apiFailure(errorResponse)
    var successCallbackCalled = false
    var errorCallbackCalled = false
    var receivedError: ClerkErrorResponse? = null

    coEvery {
      mockSignIn.attemptFirstFactor(SignIn.AttemptFirstFactorParams.EmailCode(code = code))
    } returns failureResult

    handler.attemptFirstFactorEmailCode(
      inProgressSignIn = mockSignIn,
      code = code,
      onSuccessCallback = { successCallbackCalled = true },
      onErrorCallback = {
        receivedError = errorResponse
        errorCallbackCalled = true
      },
    )

    coVerify { mockSignIn.attemptFirstFactor(any()) }
    assert(!successCallbackCalled)
    assert(errorCallbackCalled)
    assert(receivedError == errorResponse)
  }

  @Test
  fun attemptFirstFactorPhoneCodeAsFirstFactorShouldCallAttemptFirstFactorAndTriggerSuccessCallback() =
    runTest {
      val code = "654321"
      val successResult = ClerkResult.success(mockSignIn)
      var successCallbackCalled = false
      var errorCallbackCalled = false

      coEvery {
        mockSignIn.attemptFirstFactor(SignIn.AttemptFirstFactorParams.PhoneCode(code = code))
      } returns successResult

      handler.attemptFirstFactorPhoneCode(
        inProgressSignIn = mockSignIn,
        code = code,
        useSecondFactorApi = false,
        onSuccessCallback = { successCallbackCalled = true },
        onErrorCallback = { errorCallbackCalled = true },
      )

      coVerify { mockSignIn.attemptFirstFactor(any()) }
      assert(successCallbackCalled)
      assert(!errorCallbackCalled)
    }

  @Test
  fun attemptFirstFactorPhoneCodeAsFirstFactorShouldTriggerErrorCallbackOnFailure() = runTest {
    val code = "654321"
    val errorResponse = ClerkErrorResponse(errors = emptyList())
    val failureResult = ClerkResult.apiFailure(errorResponse)
    var successCallbackCalled = false
    var errorCallbackCalled = false

    coEvery {
      mockSignIn.attemptFirstFactor(SignIn.AttemptFirstFactorParams.PhoneCode(code = code))
    } returns failureResult

    handler.attemptFirstFactorPhoneCode(
      inProgressSignIn = mockSignIn,
      code = code,
      useSecondFactorApi = false,
      onSuccessCallback = { successCallbackCalled = true },
      onErrorCallback = { errorCallbackCalled = true },
    )

    coVerify { mockSignIn.attemptFirstFactor(any()) }
    assert(!successCallbackCalled)
    assert(errorCallbackCalled)
  }

  @Test
  fun attemptFirstFactorPhoneCodeAsSecondFactorShouldCallAttemptSecondFactorAndTriggerSuccessCallback() =
    runTest {
      val code = "789012"
      val successResult = ClerkResult.success(mockSignIn)
      var successCallbackCalled = false
      var errorCallbackCalled = false

      coEvery {
        mockSignIn.attemptSecondFactor(SignIn.AttemptSecondFactorParams.PhoneCode(code = code))
      } returns successResult

      handler.attemptFirstFactorPhoneCode(
        inProgressSignIn = mockSignIn,
        code = code,
        useSecondFactorApi = true,
        onSuccessCallback = { successCallbackCalled = true },
        onErrorCallback = { errorCallbackCalled = true },
      )

      coVerify { mockSignIn.attemptSecondFactor(any()) }
      assert(successCallbackCalled)
      assert(!errorCallbackCalled)
    }

  @Test
  fun attemptFirstFactorPhoneCodeAsSecondFactorShouldTriggerErrorCallbackOnFailure() = runTest {
    val code = "789012"
    val errorResponse = ClerkErrorResponse(errors = emptyList())
    val failureResult = ClerkResult.apiFailure(errorResponse)
    var successCallbackCalled = false
    var errorCallbackCalled = false

    coEvery {
      mockSignIn.attemptSecondFactor(SignIn.AttemptSecondFactorParams.PhoneCode(code = code))
    } returns failureResult

    handler.attemptFirstFactorPhoneCode(
      inProgressSignIn = mockSignIn,
      code = code,
      useSecondFactorApi = true,
      onSuccessCallback = { successCallbackCalled = true },
      onErrorCallback = { errorCallbackCalled = true },
    )

    coVerify { mockSignIn.attemptSecondFactor(any()) }
    assert(!successCallbackCalled)
    assert(errorCallbackCalled)
  }

  @Test
  fun attemptForTotpShouldCallAttemptSecondFactorAndTriggerSuccessCallback() = runTest {
    val code = "345678"
    val successResult = ClerkResult.success(mockSignIn)
    var successCallbackCalled = false
    var errorCallbackCalled = false

    coEvery {
      mockSignIn.attemptSecondFactor(SignIn.AttemptSecondFactorParams.TOTP(code = code))
    } returns successResult

    handler.attemptForTotp(
      inProgressSignIn = mockSignIn,
      code = code,
      onSuccessCallback = { successCallbackCalled = true },
      onErrorCallback = { errorCallbackCalled = true },
    )

    coVerify { mockSignIn.attemptSecondFactor(any()) }
    assert(successCallbackCalled)
    assert(!errorCallbackCalled)
  }

  @Test
  fun attemptForTotpShouldTriggerErrorCallbackOnFailure() = runTest {
    val code = "345678"
    val errorResponse = ClerkErrorResponse(errors = emptyList())
    val failureResult = ClerkResult.apiFailure(errorResponse)
    var successCallbackCalled = false
    var errorCallbackCalled = false

    coEvery {
      mockSignIn.attemptSecondFactor(SignIn.AttemptSecondFactorParams.TOTP(code = code))
    } returns failureResult

    handler.attemptForTotp(
      inProgressSignIn = mockSignIn,
      code = code,
      onSuccessCallback = { successCallbackCalled = true },
      onErrorCallback = { errorCallbackCalled = true },
    )

    coVerify { mockSignIn.attemptSecondFactor(any()) }
    assert(!successCallbackCalled)
    assert(errorCallbackCalled)
  }

  @Test
  fun attemptResetForEmailCodeShouldCallAttemptFirstFactorAndTriggerSuccessCallback() = runTest {
    val code = "901234"
    val successResult = ClerkResult.success(mockSignIn)
    var successCallbackCalled = false
    var errorCallbackCalled = false

    coEvery {
      mockSignIn.attemptFirstFactor(
        SignIn.AttemptFirstFactorParams.ResetPasswordEmailCode(code = code)
      )
    } returns successResult

    handler.attemptResetForEmailCode(
      inProgressSignIn = mockSignIn,
      code = code,
      onSuccessCallback = { successCallbackCalled = true },
      onErrorCallback = { errorCallbackCalled = true },
    )

    coVerify { mockSignIn.attemptFirstFactor(any()) }
    assert(successCallbackCalled)
    assert(!errorCallbackCalled)
  }

  @Test
  fun attemptResetForEmailCodeShouldTriggerErrorCallbackOnFailure() = runTest {
    val code = "901234"
    val errorResponse = ClerkErrorResponse(errors = emptyList())
    val failureResult = ClerkResult.apiFailure(errorResponse)
    var successCallbackCalled = false
    var errorCallbackCalled = false

    coEvery {
      mockSignIn.attemptFirstFactor(
        SignIn.AttemptFirstFactorParams.ResetPasswordEmailCode(code = code)
      )
    } returns failureResult

    handler.attemptResetForEmailCode(
      inProgressSignIn = mockSignIn,
      code = code,
      onSuccessCallback = { successCallbackCalled = true },
      onErrorCallback = { errorCallbackCalled = true },
    )

    coVerify { mockSignIn.attemptFirstFactor(any()) }
    assert(!successCallbackCalled)
    assert(errorCallbackCalled)
  }

  @Test
  fun attemptResetForPhoneCodeShouldCallAttemptFirstFactorAndTriggerSuccessCallback() = runTest {
    val code = "567890"
    val successResult = ClerkResult.success(mockSignIn)
    var successCallbackCalled = false
    var errorCallbackCalled = false

    coEvery {
      mockSignIn.attemptFirstFactor(
        SignIn.AttemptFirstFactorParams.ResetPasswordPhoneCode(code = code)
      )
    } returns successResult

    handler.attemptResetForPhoneCode(
      inProgressSignIn = mockSignIn,
      code = code,
      onSuccessCallback = { successCallbackCalled = true },
      onErrorCallback = { errorCallbackCalled = true },
    )

    coVerify { mockSignIn.attemptFirstFactor(any()) }
    assert(successCallbackCalled)
    assert(!errorCallbackCalled)
  }

  @Test
  fun attemptResetForPhoneCodeShouldTriggerErrorCallbackOnFailure() = runTest {
    val code = "567890"
    val errorResponse = ClerkErrorResponse(errors = emptyList())
    val failureResult = ClerkResult.apiFailure(errorResponse)
    var successCallbackCalled = false
    var errorCallbackCalled = false

    coEvery {
      mockSignIn.attemptFirstFactor(
        SignIn.AttemptFirstFactorParams.ResetPasswordPhoneCode(code = code)
      )
    } returns failureResult

    handler.attemptResetForPhoneCode(
      inProgressSignIn = mockSignIn,
      code = code,
      onSuccessCallback = { successCallbackCalled = true },
      onErrorCallback = { errorCallbackCalled = true },
    )

    coVerify { mockSignIn.attemptFirstFactor(any()) }
    assert(!successCallbackCalled)
    assert(errorCallbackCalled)
  }
}
