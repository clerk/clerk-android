package com.clerk.ui.signin.code

import com.clerk.api.log.ClerkLog
import com.clerk.api.network.model.error.ClerkErrorResponse
import com.clerk.api.network.serialization.onFailure
import com.clerk.api.network.serialization.onSuccess
import com.clerk.api.signin.SignIn
import com.clerk.api.signin.attemptFirstFactor
import com.clerk.api.signin.attemptSecondFactor

internal class SignInAttemptHandler {

  internal suspend fun attemptForTotp(
    inProgressSignIn: SignIn,
    code: String,
    onSuccessCallback: suspend () -> Unit,
    onErrorCallback: suspend (ClerkErrorResponse?) -> Unit,
  ) {
    inProgressSignIn
      .attemptSecondFactor(SignIn.AttemptSecondFactorParams.TOTP(code = code))
      .onSuccess { onSuccessCallback() }
      .onFailure {
        ClerkLog.e("Error attempting TOTP code: $it")
        onErrorCallback(it.error)
      }
  }

  internal suspend fun attemptResetForPhoneCode(
    inProgressSignIn: SignIn,
    code: String,
    onSuccessCallback: suspend () -> Unit,
    onErrorCallback: suspend (ClerkErrorResponse?) -> Unit,
  ) {
    inProgressSignIn
      .attemptFirstFactor(SignIn.AttemptFirstFactorParams.ResetPasswordPhoneCode(code = code))
      .onSuccess { onSuccessCallback() }
      .onFailure {
        ClerkLog.e("Error attempting reset password phone code: $it")
        onErrorCallback(it.error)
      }
  }

  internal suspend fun attemptResetForEmailCode(
    inProgressSignIn: SignIn,
    code: String,
    onSuccessCallback: suspend () -> Unit,
    onErrorCallback: suspend (ClerkErrorResponse?) -> Unit,
  ) {
    inProgressSignIn
      .attemptFirstFactor(SignIn.AttemptFirstFactorParams.ResetPasswordEmailCode(code = code))
      .onSuccess { onSuccessCallback() }
      .onFailure {
        ClerkLog.e("Error attempting reset password email code: $it")
        onErrorCallback(it.error)
      }
  }

  internal suspend fun attemptFirstFactorPhoneCode(
    inProgressSignIn: SignIn,
    code: String,
    isSecondFactor: Boolean,
    onSuccessCallback: suspend () -> Unit,
    onErrorCallback: suspend (ClerkErrorResponse?) -> Unit,
  ) {
    if (isSecondFactor) {
      inProgressSignIn
        .attemptSecondFactor(SignIn.AttemptSecondFactorParams.PhoneCode(code = code))
        .onSuccess { onSuccessCallback() }
        .onFailure {
          ClerkLog.e("Error attempting phone code as second factor: $it")
          onErrorCallback(it.error)
        }
    } else {
      inProgressSignIn
        .attemptFirstFactor(SignIn.AttemptFirstFactorParams.PhoneCode(code = code))
        .onSuccess { onSuccessCallback() }
        .onFailure {
          ClerkLog.e("Error attempting phone code: $it")
          onErrorCallback(it.error)
        }
    }
  }

  internal suspend fun attemptFirstFactorEmailCode(
    inProgressSignIn: SignIn,
    code: String,
    onSuccessCallback: suspend () -> Unit,
    onErrorCallback: suspend (ClerkErrorResponse?) -> Unit,
  ) {
    inProgressSignIn
      .attemptFirstFactor(SignIn.AttemptFirstFactorParams.EmailCode(code = code))
      .onSuccess { onSuccessCallback() }
      .onFailure {
        ClerkLog.e("Error attempting email code: $it")
        onErrorCallback(it.error)
      }
  }
}
