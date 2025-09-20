package com.clerk.ui.signin.code

import com.clerk.api.log.ClerkLog
import com.clerk.api.network.model.error.ClerkErrorResponse
import com.clerk.api.network.model.factor.Factor
import com.clerk.api.network.serialization.ClerkResult
import com.clerk.api.signin.SignIn
import com.clerk.api.signin.prepareFirstFactor
import com.clerk.api.signin.prepareSecondFactor
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
 * Comprehensive test suite for SignInPrepareHandler covering:
 * - Email code preparation for both regular and password reset scenarios
 * - Phone code preparation for first and second factor scenarios
 * - Phone code preparation for password reset scenarios
 * - Success and failure handling for all preparation methods
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SignInPrepareHandlerTest {

  private val mockSignIn = mockk<SignIn>(relaxed = true)
  private val handler = SignInPrepareHandler()

  @Before
  fun setUp() {
    mockkStatic("com.clerk.api.signin.SignInKt")
    mockkObject(ClerkLog)
    every { ClerkLog.e(any()) } returns 0
    every { ClerkLog.v(any()) } returns 0
  }

  @After
  fun tearDown() {
    unmockkAll()
  }

  @Test
  fun prepareForEmailCodeShouldCallPrepareFirstFactorWithEmailCodeParams() = runTest {
    val factor = Factor(strategy = "email_code", emailAddressId = "email_123")
    val successResult = ClerkResult.success(mockSignIn)

    coEvery {
      mockSignIn.prepareFirstFactor(
        SignIn.PrepareFirstFactorParams.EmailCode(emailAddressId = "email_123")
      )
    } returns successResult

    handler.prepareForEmailCode(mockSignIn, factor)

    coVerify {
      mockSignIn.prepareFirstFactor(
        SignIn.PrepareFirstFactorParams.EmailCode(emailAddressId = "email_123")
      )
    }
  }

  @Test
  fun prepareForEmailCodeShouldHandleFailureGracefully() = runTest {
    val factor = Factor(strategy = "email_code", emailAddressId = "email_123")
    val errorResponse = mockk<ClerkErrorResponse>()
    val failureResult = ClerkResult.apiFailure(errorResponse)

    coEvery {
      mockSignIn.prepareFirstFactor(
        SignIn.PrepareFirstFactorParams.EmailCode(emailAddressId = "email_123")
      )
    } returns failureResult

    // This should not throw an exception - the handler logs but doesn't propagate errors
    handler.prepareForEmailCode(mockSignIn, factor)

    coVerify {
      mockSignIn.prepareFirstFactor(
        SignIn.PrepareFirstFactorParams.EmailCode(emailAddressId = "email_123")
      )
    }
  }

  @Test
  fun prepareForPhoneCodeAsFirstFactorShouldCallPrepareFirstFactorWithPhoneCodeParams() = runTest {
    val factor = Factor(strategy = "phone_code", phoneNumberId = "phone_456")
    val successResult = ClerkResult.success(mockSignIn)

    coEvery {
      mockSignIn.prepareFirstFactor(
        SignIn.PrepareFirstFactorParams.PhoneCode(phoneNumberId = "phone_456")
      )
    } returns successResult

    handler.prepareForPhoneCode(mockSignIn, factor, isSecondFactor = false)

    coVerify {
      mockSignIn.prepareFirstFactor(
        SignIn.PrepareFirstFactorParams.PhoneCode(phoneNumberId = "phone_456")
      )
    }
  }

  @Test
  fun prepareForPhoneCodeAsFirstFactorShouldHandleFailureGracefully() = runTest {
    val factor = Factor(strategy = "phone_code", phoneNumberId = "phone_456")
    val errorResponse = mockk<ClerkErrorResponse>()
    val failureResult = ClerkResult.apiFailure(errorResponse)

    coEvery {
      mockSignIn.prepareFirstFactor(
        SignIn.PrepareFirstFactorParams.PhoneCode(phoneNumberId = "phone_456")
      )
    } returns failureResult

    // This should not throw an exception - the handler logs but doesn't propagate errors
    handler.prepareForPhoneCode(mockSignIn, factor, isSecondFactor = false)

    coVerify {
      mockSignIn.prepareFirstFactor(
        SignIn.PrepareFirstFactorParams.PhoneCode(phoneNumberId = "phone_456")
      )
    }
  }

  @Test
  fun prepareForPhoneCodeAsSecondFactorShouldCallPrepareSecondFactor() = runTest {
    val factor = Factor(strategy = "phone_code", phoneNumberId = "phone_789")
    val successResult = ClerkResult.success(mockSignIn)

    coEvery { mockSignIn.prepareSecondFactor("phone_789") } returns successResult

    handler.prepareForPhoneCode(mockSignIn, factor, isSecondFactor = true)

    coVerify { mockSignIn.prepareSecondFactor("phone_789") }
  }

  @Test
  fun prepareForPhoneCodeAsSecondFactorShouldHandleFailureGracefully() = runTest {
    val factor = Factor(strategy = "phone_code", phoneNumberId = "phone_789")
    val errorResponse = mockk<ClerkErrorResponse>()
    val failureResult = ClerkResult.apiFailure(errorResponse)

    coEvery { mockSignIn.prepareSecondFactor("phone_789") } returns failureResult

    // This should not throw an exception - the handler logs but doesn't propagate errors
    handler.prepareForPhoneCode(mockSignIn, factor, isSecondFactor = true)

    coVerify { mockSignIn.prepareSecondFactor("phone_789") }
  }

  @Test
  fun prepareForResetPasswordWithPhoneShouldCallPrepareFirstFactorWithResetPasswordPhoneCodeParams() =
    runTest {
      val factor = Factor(strategy = "reset_password_phone_code", phoneNumberId = "phone_reset_123")
      val successResult = ClerkResult.success(mockSignIn)

      coEvery {
        mockSignIn.prepareFirstFactor(
          SignIn.PrepareFirstFactorParams.ResetPasswordPhoneCode(phoneNumberId = "phone_reset_123")
        )
      } returns successResult

      handler.prepareForResetPasswordWithPhone(mockSignIn, factor)

      coVerify {
        mockSignIn.prepareFirstFactor(
          SignIn.PrepareFirstFactorParams.ResetPasswordPhoneCode(phoneNumberId = "phone_reset_123")
        )
      }
    }

  @Test
  fun prepareForResetPasswordWithPhoneShouldHandleFailureGracefully() = runTest {
    val factor = Factor(strategy = "reset_password_phone_code", phoneNumberId = "phone_reset_123")
    val errorResponse = mockk<ClerkErrorResponse>()
    val failureResult = ClerkResult.apiFailure(errorResponse)

    coEvery {
      mockSignIn.prepareFirstFactor(
        SignIn.PrepareFirstFactorParams.ResetPasswordPhoneCode(phoneNumberId = "phone_reset_123")
      )
    } returns failureResult

    // This should not throw an exception - the handler logs but doesn't propagate errors
    handler.prepareForResetPasswordWithPhone(mockSignIn, factor)

    coVerify {
      mockSignIn.prepareFirstFactor(
        SignIn.PrepareFirstFactorParams.ResetPasswordPhoneCode(phoneNumberId = "phone_reset_123")
      )
    }
  }

  @Test
  fun prepareForResetWithEmailCodeShouldCallPrepareFirstFactorWithResetPasswordEmailCodeParams() =
    runTest {
      val factor =
        Factor(strategy = "reset_password_email_code", emailAddressId = "email_reset_456")
      val successResult = ClerkResult.success(mockSignIn)

      coEvery {
        mockSignIn.prepareFirstFactor(
          SignIn.PrepareFirstFactorParams.ResetPasswordEmailCode(emailAddressId = "email_reset_456")
        )
      } returns successResult

      handler.prepareForResetWithEmailCode(mockSignIn, factor)

      coVerify {
        mockSignIn.prepareFirstFactor(
          SignIn.PrepareFirstFactorParams.ResetPasswordEmailCode(emailAddressId = "email_reset_456")
        )
      }
    }

  @Test
  fun prepareForResetWithEmailCodeShouldHandleFailureGracefully() = runTest {
    val factor = Factor(strategy = "reset_password_email_code", emailAddressId = "email_reset_456")
    val errorResponse = mockk<ClerkErrorResponse>()
    val failureResult = ClerkResult.apiFailure(errorResponse)

    coEvery {
      mockSignIn.prepareFirstFactor(
        SignIn.PrepareFirstFactorParams.ResetPasswordEmailCode(emailAddressId = "email_reset_456")
      )
    } returns failureResult

    // This should not throw an exception - the handler logs but doesn't propagate errors
    handler.prepareForResetWithEmailCode(mockSignIn, factor)

    coVerify {
      mockSignIn.prepareFirstFactor(
        SignIn.PrepareFirstFactorParams.ResetPasswordEmailCode(emailAddressId = "email_reset_456")
      )
    }
  }

  @Test
  fun prepareForPhoneCodeShouldHandleNullPhoneNumberId() = runTest {
    val factor = Factor(strategy = "phone_code", phoneNumberId = null)

    // This should throw a NullPointerException due to the !! operator in the handler
    var thrownException: Exception? = null
    try {
      handler.prepareForPhoneCode(mockSignIn, factor, isSecondFactor = false)
    } catch (e: Exception) {
      thrownException = e
    }

    // Verify that an exception was thrown (due to null phoneNumberId!!)
    assert(thrownException != null)
  }

  @Test
  fun prepareForEmailCodeShouldHandleNullEmailAddressId() = runTest {
    val factor = Factor(strategy = "email_code", emailAddressId = null)

    // This should throw a NullPointerException due to the !! operator in the handler
    var thrownException: Exception? = null
    try {
      handler.prepareForEmailCode(mockSignIn, factor)
    } catch (e: Exception) {
      thrownException = e
    }

    // Verify that an exception was thrown (due to null emailAddressId!!)
    assert(thrownException != null)
  }
}
