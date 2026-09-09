package com.clerk.ui.signin.code

import com.clerk.api.*
import com.clerk.testing.testCoreError
import com.clerk.ui.auth.FactorSelection
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Test

class SignInPrepareHandlerTest {
  private val signIn = mockk<SignIn>(relaxed = true)
  private val handler = SignInPrepareHandler()

  @After fun tearDown() = unmockkAll()

  @Test
  fun emailFirstPreservesSelectedFactor() = runTest {
    val factor = FactorSelection("email_code", emailAddressId = "selected_email")
    coEvery {
      signIn.emailCode.sendCode(
        SignInEmailCodeSendParams.Case2(
          SignInEmailCodeSendCodeParamsCase2(emailAddressId = "selected_email")
        )
      )
    } returns mockk(relaxed = true)
    var error: String? = null
    handler.prepareForEmailCode(signIn, factor, isSecondFactor = false, onError = { error = it })
    coVerify(exactly = 1) {
      signIn.emailCode.sendCode(
        SignInEmailCodeSendParams.Case2(
          SignInEmailCodeSendCodeParamsCase2(emailAddressId = "selected_email")
        )
      )
    }
    assertNull(error)
  }

  @Test
  fun emailFirstFailure() = runTest {
    val factor = FactorSelection("email_code", emailAddressId = "selected_email")
    coEvery {
      signIn.emailCode.sendCode(
        SignInEmailCodeSendParams.Case2(
          SignInEmailCodeSendCodeParamsCase2(emailAddressId = "selected_email")
        )
      )
    } throws testCoreError("Code could not be sent")
    var error: String? = null
    handler.prepareForEmailCode(signIn, factor, isSecondFactor = false, onError = { error = it })
    coVerify(exactly = 1) {
      signIn.emailCode.sendCode(
        SignInEmailCodeSendParams.Case2(
          SignInEmailCodeSendCodeParamsCase2(emailAddressId = "selected_email")
        )
      )
    }
    assertEquals("Code could not be sent", error)
  }

  @Test
  fun emailSecondPreservesSelectedFactor() = runTest {
    val factor = FactorSelection("email_code", emailAddressId = "selected_email")
    coEvery {
      signIn.mfa.sendEmailCode(SignInMFAEmailCodeSendParams(emailAddressId = "selected_email"))
    } returns mockk(relaxed = true)
    var error: String? = null
    handler.prepareForEmailCode(signIn, factor, isSecondFactor = true, onError = { error = it })
    coVerify(exactly = 1) {
      signIn.mfa.sendEmailCode(SignInMFAEmailCodeSendParams(emailAddressId = "selected_email"))
    }
    assertNull(error)
  }

  @Test
  fun emailSecondFailure() = runTest {
    val factor = FactorSelection("email_code", emailAddressId = "selected_email")
    coEvery {
      signIn.mfa.sendEmailCode(SignInMFAEmailCodeSendParams(emailAddressId = "selected_email"))
    } throws testCoreError("Code could not be sent")
    var error: String? = null
    handler.prepareForEmailCode(signIn, factor, isSecondFactor = true, onError = { error = it })
    coVerify(exactly = 1) {
      signIn.mfa.sendEmailCode(SignInMFAEmailCodeSendParams(emailAddressId = "selected_email"))
    }
    assertEquals("Code could not be sent", error)
  }

  @Test
  fun phoneFirstPreservesSelectedFactor() = runTest {
    val factor = FactorSelection("phone_code", phoneNumberId = "selected_phone")
    coEvery {
      signIn.phoneCode.sendCode(
        SignInPhoneCodeSendParams.Case2(
          SignInPhoneCodeSendCodeParamsCase2(phoneNumberId = "selected_phone")
        )
      )
    } returns mockk(relaxed = true)
    var error: String? = null
    handler.prepareForPhoneCode(signIn, factor, isSecondFactor = false, onError = { error = it })
    coVerify(exactly = 1) {
      signIn.phoneCode.sendCode(
        SignInPhoneCodeSendParams.Case2(
          SignInPhoneCodeSendCodeParamsCase2(phoneNumberId = "selected_phone")
        )
      )
    }
    assertNull(error)
  }

  @Test
  fun phoneFirstFailure() = runTest {
    val factor = FactorSelection("phone_code", phoneNumberId = "selected_phone")
    coEvery {
      signIn.phoneCode.sendCode(
        SignInPhoneCodeSendParams.Case2(
          SignInPhoneCodeSendCodeParamsCase2(phoneNumberId = "selected_phone")
        )
      )
    } throws testCoreError("Code could not be sent")
    var error: String? = null
    handler.prepareForPhoneCode(signIn, factor, isSecondFactor = false, onError = { error = it })
    coVerify(exactly = 1) {
      signIn.phoneCode.sendCode(
        SignInPhoneCodeSendParams.Case2(
          SignInPhoneCodeSendCodeParamsCase2(phoneNumberId = "selected_phone")
        )
      )
    }
    assertEquals("Code could not be sent", error)
  }

  @Test
  fun phoneSecondPreservesSelectedFactor() = runTest {
    val factor = FactorSelection("phone_code", phoneNumberId = "selected_phone")
    coEvery {
      signIn.mfa.sendPhoneCode(SignInMFAPhoneCodeSendParams(phoneNumberId = "selected_phone"))
    } returns mockk(relaxed = true)
    var error: String? = null
    handler.prepareForPhoneCode(signIn, factor, isSecondFactor = true, onError = { error = it })
    coVerify(exactly = 1) {
      signIn.mfa.sendPhoneCode(SignInMFAPhoneCodeSendParams(phoneNumberId = "selected_phone"))
    }
    assertNull(error)
  }

  @Test
  fun phoneSecondFailure() = runTest {
    val factor = FactorSelection("phone_code", phoneNumberId = "selected_phone")
    coEvery {
      signIn.mfa.sendPhoneCode(SignInMFAPhoneCodeSendParams(phoneNumberId = "selected_phone"))
    } throws testCoreError("Code could not be sent")
    var error: String? = null
    handler.prepareForPhoneCode(signIn, factor, isSecondFactor = true, onError = { error = it })
    coVerify(exactly = 1) {
      signIn.mfa.sendPhoneCode(SignInMFAPhoneCodeSendParams(phoneNumberId = "selected_phone"))
    }
    assertEquals("Code could not be sent", error)
  }

  @Test
  fun resetEmailPreservesSelectedFactor() = runTest {
    val factor = FactorSelection("reset_password_email_code", emailAddressId = "selected_email")
    coEvery {
      signIn.resetPasswordEmailCode.sendCode(
        SignInResetPasswordEmailCodeSendParams(emailAddressId = "selected_email")
      )
    } returns mockk(relaxed = true)
    var error: String? = null
    handler.prepareForResetWithEmailCode(signIn, factor, onError = { error = it })
    coVerify(exactly = 1) {
      signIn.resetPasswordEmailCode.sendCode(
        SignInResetPasswordEmailCodeSendParams(emailAddressId = "selected_email")
      )
    }
    assertNull(error)
  }

  @Test
  fun resetEmailFailure() = runTest {
    val factor = FactorSelection("reset_password_email_code", emailAddressId = "selected_email")
    coEvery {
      signIn.resetPasswordEmailCode.sendCode(
        SignInResetPasswordEmailCodeSendParams(emailAddressId = "selected_email")
      )
    } throws testCoreError("Code could not be sent")
    var error: String? = null
    handler.prepareForResetWithEmailCode(signIn, factor, onError = { error = it })
    coVerify(exactly = 1) {
      signIn.resetPasswordEmailCode.sendCode(
        SignInResetPasswordEmailCodeSendParams(emailAddressId = "selected_email")
      )
    }
    assertEquals("Code could not be sent", error)
  }

  @Test
  fun resetPhonePreservesSelectedFactor() = runTest {
    val factor = FactorSelection("reset_password_phone_code", phoneNumberId = "selected_phone")
    coEvery {
      signIn.resetPasswordPhoneCode.sendCode(
        SignInResetPasswordPhoneCodeSendParams(phoneNumberId = "selected_phone")
      )
    } returns mockk(relaxed = true)
    var error: String? = null
    handler.prepareForResetPasswordWithPhone(signIn, factor, onError = { error = it })
    coVerify(exactly = 1) {
      signIn.resetPasswordPhoneCode.sendCode(
        SignInResetPasswordPhoneCodeSendParams(phoneNumberId = "selected_phone")
      )
    }
    assertNull(error)
  }

  @Test
  fun resetPhoneFailure() = runTest {
    val factor = FactorSelection("reset_password_phone_code", phoneNumberId = "selected_phone")
    coEvery {
      signIn.resetPasswordPhoneCode.sendCode(
        SignInResetPasswordPhoneCodeSendParams(phoneNumberId = "selected_phone")
      )
    } throws testCoreError("Code could not be sent")
    var error: String? = null
    handler.prepareForResetPasswordWithPhone(signIn, factor, onError = { error = it })
    coVerify(exactly = 1) {
      signIn.resetPasswordPhoneCode.sendCode(
        SignInResetPasswordPhoneCodeSendParams(phoneNumberId = "selected_phone")
      )
    }
    assertEquals("Code could not be sent", error)
  }

  @Test
  fun missingEmailIdDoesNotSendCodeOrShowAnError() = runTest {
    var error: String? = null
    handler.prepareForEmailCode(signIn, FactorSelection("email_code"), false) { error = it }
    coVerify(exactly = 0) { signIn.emailCode.sendCode(any()) }
    assertNull(error)
  }

  @Test
  fun missingPhoneIdDoesNotSendCodeOrShowAnError() = runTest {
    var error: String? = null
    handler.prepareForPhoneCode(signIn, FactorSelection("phone_code"), false) { error = it }
    coVerify(exactly = 0) { signIn.phoneCode.sendCode(any()) }
    assertNull(error)
  }

  @Test
  fun unavailableFirstFactorDisplaysCoreError() = runTest {
    coEvery { signIn.emailCode.sendCode(any()) } throws
      testCoreError("Selected sign-in method is no longer available.", "factor_not_found")
    var error: String? = null
    handler.prepareForEmailCode(
      signIn,
      FactorSelection("email_code", emailAddressId = "removed_email"),
      false,
    ) {
      error = it
    }
    assertEquals("Selected sign-in method is no longer available.", error)
  }

  @Test
  fun unavailableSecondFactorDisplaysCoreError() = runTest {
    coEvery { signIn.mfa.sendEmailCode(any()) } throws
      testCoreError("Selected sign-in method is no longer available.", "factor_not_found")
    var error: String? = null
    handler.prepareForEmailCode(
      signIn,
      FactorSelection("email_code", emailAddressId = "removed_email"),
      true,
    ) {
      error = it
    }
    assertEquals("Selected sign-in method is no longer available.", error)
  }
}
