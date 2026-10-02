package com.clerk.ui.signin.code

import com.clerk.api.auth.types.MfaType
import com.clerk.api.log.ClerkLog
import com.clerk.api.network.serialization.errorMessage
import com.clerk.api.network.serialization.onFailure
import com.clerk.api.network.serialization.onSuccess
import com.clerk.api.signin.SignIn
import com.clerk.api.signin.attemptFirstFactor
import com.clerk.api.signin.verifyMfaCode

internal class SignInAttemptHandler {

  internal suspend fun attemptForTotp(
    inProgressSignIn: SignIn,
    code: String,
    onSuccessCallback: suspend (SignIn) -> Unit,
    onErrorCallback: suspend (String?) -> Unit,
  ) {
    inProgressSignIn
      .verifyMfaCode(code = code, type = MfaType.TOTP)
      .onSuccess { onSuccessCallback(it) }
      .onFailure {
        ClerkLog.e("Error attempting TOTP code: $it")
        onErrorCallback(it.errorMessage)
      }
  }

  internal suspend fun attemptResetForPhoneCode(
    inProgressSignIn: SignIn,
    code: String,
    onSuccessCallback: suspend (SignIn) -> Unit,
    onErrorCallback: suspend (String?) -> Unit,
  ) {
    inProgressSignIn
      .attemptFirstFactor(SignIn.AttemptFirstFactorParams.ResetPasswordPhoneCode(code = code))
      .onSuccess { onSuccessCallback(it) }
      .onFailure {
        ClerkLog.e("Error attempting reset password phone code: $it")
        onErrorCallback(it.errorMessage)
      }
  }

  internal suspend fun attemptResetForEmailCode(
    inProgressSignIn: SignIn,
    code: String,
    onSuccessCallback: suspend (SignIn) -> Unit,
    onErrorCallback: suspend (String?) -> Unit,
  ) {
    inProgressSignIn
      .attemptFirstFactor(SignIn.AttemptFirstFactorParams.ResetPasswordEmailCode(code = code))
      .onSuccess { onSuccessCallback(it) }
      .onFailure {
        ClerkLog.e("Error attempting reset password email code: $it")
        onErrorCallback(it.errorMessage)
      }
  }

  internal suspend fun attemptFirstFactorPhoneCode(
    inProgressSignIn: SignIn,
    code: String,
    isSecondFactor: Boolean,
    onSuccessCallback: suspend (SignIn) -> Unit,
    onErrorCallback: suspend (String?) -> Unit,
  ) {
    if (isSecondFactor) {
      inProgressSignIn
        .verifyMfaCode(code = code, type = MfaType.PHONE_CODE)
        .onSuccess { onSuccessCallback(it) }
        .onFailure {
          ClerkLog.e("Error attempting phone code as second factor: $it")
          onErrorCallback(it.errorMessage)
        }
    } else {
      inProgressSignIn
        .attemptFirstFactor(SignIn.AttemptFirstFactorParams.PhoneCode(code = code))
        .onSuccess { onSuccessCallback(it) }
        .onFailure {
          ClerkLog.e("Error attempting phone code: $it")
          onErrorCallback(it.errorMessage)
        }
    }
  }

  internal suspend fun attemptEmailCode(
    inProgressSignIn: SignIn,
    code: String,
    isSecondFactor: Boolean,
    onSuccessCallback: suspend (SignIn) -> Unit,
    onErrorCallback: suspend (String?) -> Unit,
  ) {
    if (isSecondFactor) {
      inProgressSignIn
        .verifyMfaCode(code = code, type = MfaType.EMAIL_CODE)
        .onSuccess { onSuccessCallback(it) }
        .onFailure {
          ClerkLog.e("Error attempting email code as second factor: $it")
          onErrorCallback(it.errorMessage)
        }
    } else {
      inProgressSignIn
        .attemptFirstFactor(SignIn.AttemptFirstFactorParams.EmailCode(code = code))
        .onSuccess { onSuccessCallback(it) }
        .onFailure {
          ClerkLog.e("Error attempting email code: $it")
          onErrorCallback(it.errorMessage)
        }
    }
  }
}
