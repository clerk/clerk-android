package com.clerk.ui.signin.code

import com.clerk.api.*
import com.clerk.ui.core.common.displayMessage
import com.clerk.ui.core.common.runUiOperation

internal class SignInAttemptHandler {
  internal suspend fun attemptForTotp(
    inProgressSignIn: SignIn,
    code: String,
    onSuccessCallback: suspend (SignIn) -> Unit,
    onErrorCallback: suspend (String?) -> Unit,
  ) {
    runUiOperation { inProgressSignIn.mfa.verifyTOTP(SignInTOTPVerifyParams(code)) }
      .onSuccess { onSuccessCallback(inProgressSignIn) }
      .onFailure { onErrorCallback(it.displayMessage) }
  }

  internal suspend fun attemptResetForPhoneCode(
    inProgressSignIn: SignIn,
    code: String,
    onSuccessCallback: suspend (SignIn) -> Unit,
    onErrorCallback: suspend (String?) -> Unit,
  ) {
    runUiOperation {
        inProgressSignIn.resetPasswordPhoneCode.verifyCode(
          SignInResetPasswordPhoneCodeVerifyParams(code)
        )
      }
      .onSuccess { onSuccessCallback(inProgressSignIn) }
      .onFailure { onErrorCallback(it.displayMessage) }
  }

  internal suspend fun attemptResetForEmailCode(
    inProgressSignIn: SignIn,
    code: String,
    onSuccessCallback: suspend (SignIn) -> Unit,
    onErrorCallback: suspend (String?) -> Unit,
  ) {
    runUiOperation {
        inProgressSignIn.resetPasswordEmailCode.verifyCode(SignInEmailCodeVerifyParams(code))
      }
      .onSuccess { onSuccessCallback(inProgressSignIn) }
      .onFailure { onErrorCallback(it.displayMessage) }
  }

  internal suspend fun attemptFirstFactorPhoneCode(
    inProgressSignIn: SignIn,
    code: String,
    isSecondFactor: Boolean,
    onSuccessCallback: suspend (SignIn) -> Unit,
    onErrorCallback: suspend (String?) -> Unit,
  ) {
    runUiOperation {
        if (isSecondFactor)
          inProgressSignIn.mfa.verifyPhoneCode(SignInMFAPhoneCodeVerifyParams(code))
        else inProgressSignIn.phoneCode.verifyCode(SignInPhoneCodeVerifyParams(code))
      }
      .onSuccess { onSuccessCallback(inProgressSignIn) }
      .onFailure { onErrorCallback(it.displayMessage) }
  }

  internal suspend fun attemptEmailCode(
    inProgressSignIn: SignIn,
    code: String,
    isSecondFactor: Boolean,
    onSuccessCallback: suspend (SignIn) -> Unit,
    onErrorCallback: suspend (String?) -> Unit,
  ) {
    runUiOperation {
        if (isSecondFactor)
          inProgressSignIn.mfa.verifyEmailCode(SignInMFAEmailCodeVerifyParams(code))
        else inProgressSignIn.emailCode.verifyCode(SignInEmailCodeVerifyParams(code))
      }
      .onSuccess { onSuccessCallback(inProgressSignIn) }
      .onFailure { onErrorCallback(it.displayMessage) }
  }
}
