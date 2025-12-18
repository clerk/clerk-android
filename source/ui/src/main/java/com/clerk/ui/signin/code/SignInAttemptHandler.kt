package com.clerk.ui.signin.code

import com.clerk.api.log.ClerkLog
import com.clerk.api.network.serialization.errorMessage
import com.clerk.api.network.serialization.onFailure
import com.clerk.api.network.serialization.onSuccess
import com.clerk.api.signin.SignIn
import com.clerk.api.signin.attemptFirstFactor
import com.clerk.api.signin.attemptSecondFactor

internal class SignInAttemptHandler {

  internal suspend fun attemptEmailCode(
    inProgressSignIn: SignIn,
    code: String,
    useSecondFactorApi: Boolean,
    onSuccessCallback: suspend (SignIn) -> Unit,
    onErrorCallback: suspend (String?) -> Unit,
  ) {
    if (useSecondFactorApi) {
      inProgressSignIn
        .attemptSecondFactor(SignIn.AttemptSecondFactorParams.EmailCode(code = code))
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

  internal suspend fun attemptForTotp(
    inProgressSignIn: SignIn,
    code: String,
    onSuccessCallback: suspend (SignIn) -> Unit,
    onErrorCallback: suspend (String?) -> Unit,
  ) {
    inProgressSignIn
      .attemptSecondFactor(SignIn.AttemptSecondFactorParams.TOTP(code = code))
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
    useSecondFactorApi: Boolean,
    onSuccessCallback: suspend (SignIn) -> Unit,
    onErrorCallback: suspend (String?) -> Unit,
  ) {
    if (useSecondFactorApi) {
      inProgressSignIn
        .attemptSecondFactor(SignIn.AttemptSecondFactorParams.PhoneCode(code = code))
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

  internal suspend fun attemptFirstFactorEmailCode(
    inProgressSignIn: SignIn,
    code: String,
    onSuccessCallback: suspend (SignIn) -> Unit,
    onErrorCallback: suspend (String?) -> Unit,
  ) {
    attemptEmailCode(
      inProgressSignIn = inProgressSignIn,
      code = code,
      useSecondFactorApi = false,
      onSuccessCallback = onSuccessCallback,
      onErrorCallback = onErrorCallback,
    )
  }
}
