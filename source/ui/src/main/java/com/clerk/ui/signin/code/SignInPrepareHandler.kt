package com.clerk.ui.signin.code

import com.clerk.api.*
import com.clerk.ui.auth.FactorSelection
import com.clerk.ui.core.common.displayMessage
import com.clerk.ui.core.common.runUiOperation

internal class SignInPrepareHandler {
  internal suspend fun prepareForResetWithEmailCode(
    inProgressSignIn: SignIn,
    factor: FactorSelection,
    onError: (String) -> Unit,
  ) {
    runUiOperation { inProgressSignIn.resetPasswordEmailCode.sendCode() }
      .onFailure { onError(it.displayMessage) }
  }

  internal suspend fun prepareForResetPasswordWithPhone(
    inProgressSignIn: SignIn,
    factor: FactorSelection,
    onError: (String) -> Unit,
  ) {
    runUiOperation { inProgressSignIn.resetPasswordPhoneCode.sendCode() }
      .onFailure { onError(it.displayMessage) }
  }

  internal suspend fun prepareForPhoneCode(
    inProgressSignIn: SignIn,
    factor: FactorSelection,
    isSecondFactor: Boolean,
    onError: (String) -> Unit,
  ) {
    runUiOperation {
        if (isSecondFactor) inProgressSignIn.mfa.sendPhoneCode()
        else
          inProgressSignIn.phoneCode.sendCode(
            SignInPhoneCodeSendParams.Case2(
              SignInPhoneCodeSendCodeParamsCase2(
                phoneNumberId = requireNotNull(factor.phoneNumberId)
              )
            )
          )
      }
      .onFailure { onError(it.displayMessage) }
  }

  internal suspend fun prepareForEmailCode(
    inProgressSignIn: SignIn,
    factor: FactorSelection,
    isSecondFactor: Boolean,
    onError: (String) -> Unit,
  ) {
    runUiOperation {
        if (isSecondFactor) inProgressSignIn.mfa.sendEmailCode()
        else
          inProgressSignIn.emailCode.sendCode(
            SignInEmailCodeSendParams.Case2(
              SignInEmailCodeSendCodeParamsCase2(
                emailAddressId = requireNotNull(factor.emailAddressId)
              )
            )
          )
      }
      .onFailure { onError(it.displayMessage) }
  }
}
