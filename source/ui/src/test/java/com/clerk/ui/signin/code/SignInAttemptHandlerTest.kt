package com.clerk.ui.signin.code

import com.clerk.api.*
import com.clerk.testing.testCoreError
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Test

class SignInAttemptHandlerTest {
  private val signIn = mockk<SignIn>(relaxed = true)
  private val handler = SignInAttemptHandler()

  @After fun tearDown() = unmockkAll()

  @Test
  fun emailFirstSuccess() = runTest {
    coEvery { signIn.emailCode.verifyCode(SignInEmailCodeVerifyParams("123456")) } returns
      mockk(relaxed = true)
    var success: SignIn? = null
    var error: String? = null
    handler.attemptEmailCode(
      signIn,
      "123456",
      isSecondFactor = false,
      onSuccessCallback = { success = it },
      onErrorCallback = { error = it },
    )
    coVerify(exactly = 1) { signIn.emailCode.verifyCode(SignInEmailCodeVerifyParams("123456")) }
    assertSame(signIn, success)
    assertNull(error)
  }

  @Test
  fun emailFirstFailure() = runTest {
    coEvery { signIn.emailCode.verifyCode(SignInEmailCodeVerifyParams("123456")) } throws
      testCoreError("Code rejected")
    var success: SignIn? = null
    var error: String? = null
    handler.attemptEmailCode(
      signIn,
      "123456",
      isSecondFactor = false,
      onSuccessCallback = { success = it },
      onErrorCallback = { error = it },
    )
    coVerify(exactly = 1) { signIn.emailCode.verifyCode(SignInEmailCodeVerifyParams("123456")) }
    assertNull(success)
    assertEquals("Code rejected", error)
  }

  @Test
  fun emailSecondSuccess() = runTest {
    coEvery { signIn.mfa.verifyEmailCode(SignInMFAEmailCodeVerifyParams("123456")) } returns
      mockk(relaxed = true)
    var success: SignIn? = null
    var error: String? = null
    handler.attemptEmailCode(
      signIn,
      "123456",
      isSecondFactor = true,
      onSuccessCallback = { success = it },
      onErrorCallback = { error = it },
    )
    coVerify(exactly = 1) { signIn.mfa.verifyEmailCode(SignInMFAEmailCodeVerifyParams("123456")) }
    assertSame(signIn, success)
    assertNull(error)
  }

  @Test
  fun emailSecondFailure() = runTest {
    coEvery { signIn.mfa.verifyEmailCode(SignInMFAEmailCodeVerifyParams("123456")) } throws
      testCoreError("Code rejected")
    var success: SignIn? = null
    var error: String? = null
    handler.attemptEmailCode(
      signIn,
      "123456",
      isSecondFactor = true,
      onSuccessCallback = { success = it },
      onErrorCallback = { error = it },
    )
    coVerify(exactly = 1) { signIn.mfa.verifyEmailCode(SignInMFAEmailCodeVerifyParams("123456")) }
    assertNull(success)
    assertEquals("Code rejected", error)
  }

  @Test
  fun phoneFirstSuccess() = runTest {
    coEvery { signIn.phoneCode.verifyCode(SignInPhoneCodeVerifyParams("123456")) } returns
      mockk(relaxed = true)
    var success: SignIn? = null
    var error: String? = null
    handler.attemptFirstFactorPhoneCode(
      signIn,
      "123456",
      isSecondFactor = false,
      onSuccessCallback = { success = it },
      onErrorCallback = { error = it },
    )
    coVerify(exactly = 1) { signIn.phoneCode.verifyCode(SignInPhoneCodeVerifyParams("123456")) }
    assertSame(signIn, success)
    assertNull(error)
  }

  @Test
  fun phoneFirstFailure() = runTest {
    coEvery { signIn.phoneCode.verifyCode(SignInPhoneCodeVerifyParams("123456")) } throws
      testCoreError("Code rejected")
    var success: SignIn? = null
    var error: String? = null
    handler.attemptFirstFactorPhoneCode(
      signIn,
      "123456",
      isSecondFactor = false,
      onSuccessCallback = { success = it },
      onErrorCallback = { error = it },
    )
    coVerify(exactly = 1) { signIn.phoneCode.verifyCode(SignInPhoneCodeVerifyParams("123456")) }
    assertNull(success)
    assertEquals("Code rejected", error)
  }

  @Test
  fun phoneSecondSuccess() = runTest {
    coEvery { signIn.mfa.verifyPhoneCode(SignInMFAPhoneCodeVerifyParams("123456")) } returns
      mockk(relaxed = true)
    var success: SignIn? = null
    var error: String? = null
    handler.attemptFirstFactorPhoneCode(
      signIn,
      "123456",
      isSecondFactor = true,
      onSuccessCallback = { success = it },
      onErrorCallback = { error = it },
    )
    coVerify(exactly = 1) { signIn.mfa.verifyPhoneCode(SignInMFAPhoneCodeVerifyParams("123456")) }
    assertSame(signIn, success)
    assertNull(error)
  }

  @Test
  fun phoneSecondFailure() = runTest {
    coEvery { signIn.mfa.verifyPhoneCode(SignInMFAPhoneCodeVerifyParams("123456")) } throws
      testCoreError("Code rejected")
    var success: SignIn? = null
    var error: String? = null
    handler.attemptFirstFactorPhoneCode(
      signIn,
      "123456",
      isSecondFactor = true,
      onSuccessCallback = { success = it },
      onErrorCallback = { error = it },
    )
    coVerify(exactly = 1) { signIn.mfa.verifyPhoneCode(SignInMFAPhoneCodeVerifyParams("123456")) }
    assertNull(success)
    assertEquals("Code rejected", error)
  }

  @Test
  fun totpSuccess() = runTest {
    coEvery { signIn.mfa.verifyTOTP(SignInTOTPVerifyParams("123456")) } returns
      mockk(relaxed = true)
    var success: SignIn? = null
    var error: String? = null
    handler.attemptForTotp(
      signIn,
      "123456",
      onSuccessCallback = { success = it },
      onErrorCallback = { error = it },
    )
    coVerify(exactly = 1) { signIn.mfa.verifyTOTP(SignInTOTPVerifyParams("123456")) }
    assertSame(signIn, success)
    assertNull(error)
  }

  @Test
  fun totpFailure() = runTest {
    coEvery { signIn.mfa.verifyTOTP(SignInTOTPVerifyParams("123456")) } throws
      testCoreError("Code rejected")
    var success: SignIn? = null
    var error: String? = null
    handler.attemptForTotp(
      signIn,
      "123456",
      onSuccessCallback = { success = it },
      onErrorCallback = { error = it },
    )
    coVerify(exactly = 1) { signIn.mfa.verifyTOTP(SignInTOTPVerifyParams("123456")) }
    assertNull(success)
    assertEquals("Code rejected", error)
  }

  @Test
  fun resetEmailSuccess() = runTest {
    coEvery {
      signIn.resetPasswordEmailCode.verifyCode(SignInEmailCodeVerifyParams("123456"))
    } returns mockk(relaxed = true)
    var success: SignIn? = null
    var error: String? = null
    handler.attemptResetForEmailCode(
      signIn,
      "123456",
      onSuccessCallback = { success = it },
      onErrorCallback = { error = it },
    )
    coVerify(exactly = 1) {
      signIn.resetPasswordEmailCode.verifyCode(SignInEmailCodeVerifyParams("123456"))
    }
    assertSame(signIn, success)
    assertNull(error)
  }

  @Test
  fun resetEmailFailure() = runTest {
    coEvery {
      signIn.resetPasswordEmailCode.verifyCode(SignInEmailCodeVerifyParams("123456"))
    } throws testCoreError("Code rejected")
    var success: SignIn? = null
    var error: String? = null
    handler.attemptResetForEmailCode(
      signIn,
      "123456",
      onSuccessCallback = { success = it },
      onErrorCallback = { error = it },
    )
    coVerify(exactly = 1) {
      signIn.resetPasswordEmailCode.verifyCode(SignInEmailCodeVerifyParams("123456"))
    }
    assertNull(success)
    assertEquals("Code rejected", error)
  }

  @Test
  fun resetPhoneSuccess() = runTest {
    coEvery {
      signIn.resetPasswordPhoneCode.verifyCode(SignInResetPasswordPhoneCodeVerifyParams("123456"))
    } returns mockk(relaxed = true)
    var success: SignIn? = null
    var error: String? = null
    handler.attemptResetForPhoneCode(
      signIn,
      "123456",
      onSuccessCallback = { success = it },
      onErrorCallback = { error = it },
    )
    coVerify(exactly = 1) {
      signIn.resetPasswordPhoneCode.verifyCode(SignInResetPasswordPhoneCodeVerifyParams("123456"))
    }
    assertSame(signIn, success)
    assertNull(error)
  }

  @Test
  fun resetPhoneFailure() = runTest {
    coEvery {
      signIn.resetPasswordPhoneCode.verifyCode(SignInResetPasswordPhoneCodeVerifyParams("123456"))
    } throws testCoreError("Code rejected")
    var success: SignIn? = null
    var error: String? = null
    handler.attemptResetForPhoneCode(
      signIn,
      "123456",
      onSuccessCallback = { success = it },
      onErrorCallback = { error = it },
    )
    coVerify(exactly = 1) {
      signIn.resetPasswordPhoneCode.verifyCode(SignInResetPasswordPhoneCodeVerifyParams("123456"))
    }
    assertNull(success)
    assertEquals("Code rejected", error)
  }
}
