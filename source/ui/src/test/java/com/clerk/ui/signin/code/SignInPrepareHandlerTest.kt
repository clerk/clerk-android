package com.clerk.ui.signin.code

import com.clerk.api.network.model.error.ClerkErrorResponse
import com.clerk.api.network.model.error.Error as ClerkApiError
import com.clerk.api.network.model.factor.Factor
import com.clerk.api.network.serialization.ClerkResult
import com.clerk.api.signin.SignIn
import com.clerk.api.signin.sendEmailCode
import com.clerk.api.signin.sendMfaEmailCode
import com.clerk.api.signin.sendMfaPhoneCode
import com.clerk.api.signin.sendPhoneCode
import com.clerk.api.signin.sendResetPasswordEmailCode
import com.clerk.api.signin.sendResetPasswordPhoneCode
import com.clerk.ui.core.log.ClerkLog
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
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SignInPrepareHandlerTest {

  private val mockSignIn = mockk<SignIn>(relaxed = true)
  private val handler = SignInPrepareHandler()

  @Before
  fun setUp() {
    mockkStatic("com.clerk.api.signin.SignInKt")
    mockkStatic("com.clerk.api.signin.SignInExtensionsKt")
    mockkObject(ClerkLog)
    every { ClerkLog.e(any()) } returns 0
    every { ClerkLog.v(any()) } returns 0
  }

  @After
  fun tearDown() {
    unmockkAll()
  }

  @Test
  fun prepareForEmailCodeAsFirstFactorShouldSendEmailCode() = runTest {
    val factor = Factor(strategy = "email_code", emailAddressId = "email_123")
    val successResult = ClerkResult.success(mockSignIn)

    coEvery {
      mockSignIn.sendEmailCode(emailAddressId = "email_123")
    } returns successResult

    handler.prepareForEmailCode(mockSignIn, factor, isSecondFactor = false, onError = {})

    coVerify {
      mockSignIn.sendEmailCode(emailAddressId = "email_123")
    }
  }

  @Test
  fun prepareForEmailCodeAsSecondFactorShouldSendMfaEmailCode() = runTest {
    val factor = Factor(strategy = "email_code", emailAddressId = "email_123")
    val successResult = ClerkResult.success(mockSignIn)

    coEvery { mockSignIn.sendMfaEmailCode(emailAddressId = "email_123") } returns successResult

    handler.prepareForEmailCode(mockSignIn, factor, isSecondFactor = true, onError = {})

    coVerify { mockSignIn.sendMfaEmailCode(emailAddressId = "email_123") }
  }

  @Test
  fun prepareForEmailCodeAsSecondFactorShouldHandleFailureGracefully() = runTest {
    val factor = Factor(strategy = "email_code", emailAddressId = "email_123")
    val errorResponse =
      ClerkErrorResponse(
        errors =
          listOf(
            ClerkApiError(message = "Short", longMessage = "Email second factor failed", code = "x")
          ),
        clerkTraceId = null,
      )
    val failureResult = ClerkResult.apiFailure(errorResponse)
    var capturedMessage: String? = null

    coEvery { mockSignIn.sendMfaEmailCode(emailAddressId = "email_123") } returns failureResult

    handler.prepareForEmailCode(
      mockSignIn,
      factor,
      isSecondFactor = true,
      onError = { capturedMessage = it },
    )

    coVerify { mockSignIn.sendMfaEmailCode(emailAddressId = "email_123") }
    assertEquals("Email second factor failed", capturedMessage)
  }

  @Test
  fun prepareForPhoneCodeAsFirstFactorShouldSendPhoneCode() = runTest {
    val factor = Factor(strategy = "phone_code", phoneNumberId = "phone_456")
    val successResult = ClerkResult.success(mockSignIn)

    coEvery {
      mockSignIn.sendPhoneCode(phoneNumberId = "phone_456")
    } returns successResult

    handler.prepareForPhoneCode(mockSignIn, factor, isSecondFactor = false, onError = {})

    coVerify {
      mockSignIn.sendPhoneCode(phoneNumberId = "phone_456")
    }
  }

  @Test
  fun prepareForPhoneCodeAsSecondFactorShouldSendMfaPhoneCode() = runTest {
    val factor = Factor(strategy = "phone_code", phoneNumberId = "phone_789")
    val successResult = ClerkResult.success(mockSignIn)

    coEvery { mockSignIn.sendMfaPhoneCode(phoneNumberId = "phone_789") } returns successResult

    handler.prepareForPhoneCode(mockSignIn, factor, isSecondFactor = true, onError = {})

    coVerify { mockSignIn.sendMfaPhoneCode(phoneNumberId = "phone_789") }
  }

  @Test
  fun prepareForPhoneCodeAsSecondFactorShouldHandleFailureGracefully() = runTest {
    val factor = Factor(strategy = "phone_code", phoneNumberId = "phone_789")
    val errorResponse =
      ClerkErrorResponse(
        errors =
          listOf(
            ClerkApiError(message = "Short", longMessage = "Phone second factor failed", code = "x")
          ),
        clerkTraceId = null,
      )
    val failureResult = ClerkResult.apiFailure(errorResponse)
    var capturedMessage: String? = null

    coEvery { mockSignIn.sendMfaPhoneCode(phoneNumberId = "phone_789") } returns failureResult

    handler.prepareForPhoneCode(
      mockSignIn,
      factor,
      isSecondFactor = true,
      onError = { capturedMessage = it },
    )

    coVerify { mockSignIn.sendMfaPhoneCode(phoneNumberId = "phone_789") }
    assertEquals("Phone second factor failed", capturedMessage)
  }

  @Test
  fun prepareForResetPasswordWithPhoneShouldSendResetPasswordPhoneCode() = runTest {
    val factor = Factor(strategy = "reset_password_phone_code", phoneNumberId = "phone_reset_123")
    val successResult = ClerkResult.success(mockSignIn)

    coEvery {
      mockSignIn.sendResetPasswordPhoneCode(phoneNumberId = "phone_reset_123")
    } returns successResult

    handler.prepareForResetPasswordWithPhone(mockSignIn, factor, onError = {})

    coVerify {
      mockSignIn.sendResetPasswordPhoneCode(phoneNumberId = "phone_reset_123")
    }
  }

  @Test
  fun prepareForResetWithEmailCodeShouldSendResetPasswordEmailCode() = runTest {
    val factor = Factor(strategy = "reset_password_email_code", emailAddressId = "email_reset_456")
    val successResult = ClerkResult.success(mockSignIn)

    coEvery {
      mockSignIn.sendResetPasswordEmailCode(emailAddressId = "email_reset_456")
    } returns successResult

    handler.prepareForResetWithEmailCode(mockSignIn, factor, onError = {})

    coVerify {
      mockSignIn.sendResetPasswordEmailCode(emailAddressId = "email_reset_456")
    }
  }

  @Test
  fun prepareForPhoneCodeShouldHandleNullPhoneNumberId() = runTest {
    val factor = Factor(strategy = "phone_code", phoneNumberId = null)

    handler.prepareForPhoneCode(mockSignIn, factor, isSecondFactor = false, onError = {})

    coVerify(exactly = 0) { mockSignIn.sendPhoneCode(any()) }
  }

  @Test
  fun prepareForEmailCodeShouldHandleNullEmailAddressId() = runTest {
    val factor = Factor(strategy = "email_code", emailAddressId = null)

    handler.prepareForEmailCode(mockSignIn, factor, isSecondFactor = false, onError = {})

    coVerify(exactly = 0) { mockSignIn.sendEmailCode(any()) }
  }

  @Test
  fun prepareForEmailCodeShouldInvokeOnErrorOnFailure() = runTest {
    val factor = Factor(strategy = "email_code", emailAddressId = "email_123")
    val error =
      ClerkErrorResponse(
        errors =
          listOf(ClerkApiError(message = "Short", longMessage = "Long message", code = "code")),
        clerkTraceId = null,
      )
    val failureResult = ClerkResult.apiFailure(error)

    coEvery {
      mockSignIn.sendEmailCode(emailAddressId = "email_123")
    } returns failureResult

    var capturedMessage: String? = null
    handler.prepareForEmailCode(
      mockSignIn,
      factor,
      isSecondFactor = false,
      onError = { capturedMessage = it },
    )

    assertEquals("Long message", capturedMessage)
  }

  @Test
  fun prepareForPhoneCodeFirstFactorShouldInvokeOnErrorOnFailure() = runTest {
    val factor = Factor(strategy = "phone_code", phoneNumberId = "phone_456")
    val error =
      ClerkErrorResponse(
        errors = listOf(ClerkApiError(message = null, longMessage = null, code = "code")),
        clerkTraceId = null,
      )
    val failureResult = ClerkResult.apiFailure(error)

    coEvery {
      mockSignIn.sendPhoneCode(phoneNumberId = "phone_456")
    } returns failureResult

    var capturedMessage: String? = null
    handler.prepareForPhoneCode(
      mockSignIn,
      factor,
      isSecondFactor = false,
      onError = { capturedMessage = it },
    )

    assertEquals("Error occurred with unknown message.", capturedMessage)
  }

  @Test
  fun prepareForResetPasswordWithPhoneShouldInvokeOnErrorOnFailure() = runTest {
    val factor = Factor(strategy = "reset_password_phone_code", phoneNumberId = "phone_reset_123")
    val error =
      ClerkErrorResponse(
        errors = listOf(ClerkApiError(message = "Short", longMessage = null, code = "x")),
        clerkTraceId = null,
      )
    val failureResult = ClerkResult.apiFailure(error)

    coEvery {
      mockSignIn.sendResetPasswordPhoneCode(phoneNumberId = "phone_reset_123")
    } returns failureResult

    var capturedMessage: String? = null
    handler.prepareForResetPasswordWithPhone(mockSignIn, factor, onError = { capturedMessage = it })

    assertEquals("Short", capturedMessage)
  }

  @Test
  fun prepareForResetWithEmailCodeShouldInvokeOnErrorOnFailure() = runTest {
    val factor = Factor(strategy = "reset_password_email_code", emailAddressId = "email_reset_456")
    val error =
      ClerkErrorResponse(
        errors = listOf(ClerkApiError(message = null, longMessage = "Long fail", code = "x")),
        clerkTraceId = null,
      )
    val failureResult = ClerkResult.apiFailure(error)

    coEvery {
      mockSignIn.sendResetPasswordEmailCode(emailAddressId = "email_reset_456")
    } returns failureResult

    var capturedMessage: String? = null
    handler.prepareForResetWithEmailCode(mockSignIn, factor, onError = { capturedMessage = it })

    assertEquals("Long fail", capturedMessage)
  }

  @Test
  fun prepareForEmailCodeWithNullIdShouldNotInvokeOnError() = runTest {
    val factor = Factor(strategy = "email_code", emailAddressId = null)
    var called = false

    handler.prepareForEmailCode(
      mockSignIn,
      factor,
      isSecondFactor = false,
      onError = { called = true },
    )

    coVerify(exactly = 0) { mockSignIn.sendEmailCode(any()) }
    assertFalse(called)
  }

  @Test
  fun prepareForPhoneCodeWithNullIdShouldNotInvokeOnError() = runTest {
    val factor = Factor(strategy = "phone_code", phoneNumberId = null)
    var called = false

    handler.prepareForPhoneCode(
      mockSignIn,
      factor,
      isSecondFactor = false,
      onError = { called = true },
    )

    coVerify(exactly = 0) { mockSignIn.sendPhoneCode(any()) }
    assertFalse(called)
  }

  @Test
  fun prepareForEmailCodeShouldSkipApiCallWhenFirstFactorIsNotSupported() = runTest {
    every { mockSignIn.supportedFirstFactors } returns listOf(Factor(strategy = "ticket"))
    val factor = Factor(strategy = "email_code", emailAddressId = "email_123")
    var capturedMessage: String? = null

    handler.prepareForEmailCode(
      inProgressSignIn = mockSignIn,
      factor = factor,
      isSecondFactor = false,
      onError = { capturedMessage = it },
    )

    coVerify(exactly = 0) { mockSignIn.sendEmailCode(any()) }
    assertEquals("Selected sign-in method is no longer available.", capturedMessage)
  }

  @Test
  fun prepareForEmailCodeShouldSkipApiCallWhenSecondFactorIsNotSupported() = runTest {
    every { mockSignIn.supportedSecondFactors } returns listOf(Factor(strategy = "totp"))
    val factor = Factor(strategy = "email_code", emailAddressId = "email_123")
    var capturedMessage: String? = null

    handler.prepareForEmailCode(
      inProgressSignIn = mockSignIn,
      factor = factor,
      isSecondFactor = true,
      onError = { capturedMessage = it },
    )

    coVerify(exactly = 0) { mockSignIn.sendMfaEmailCode(emailAddressId = any()) }
    assertEquals("Selected sign-in method is no longer available.", capturedMessage)
  }
}
