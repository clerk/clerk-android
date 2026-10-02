package com.clerk.api.sso

import com.clerk.api.network.model.error.ClerkErrorResponse
import com.clerk.api.network.model.verification.Verification
import com.clerk.api.network.serialization.ClerkResult
import com.clerk.api.signin.SignIn
import com.clerk.api.signup.SignUp

internal fun ClerkResult<SignIn, ClerkErrorResponse>.signInToOAuthResult():
  ClerkResult<OAuthResult, ClerkErrorResponse> {
  return when (this) {
    is ClerkResult.Success -> {
      ClerkResult.success(OAuthResult(signIn = this.value))
    }
    is ClerkResult.Failure -> {
      when (this.errorType) {
        ClerkResult.Failure.ErrorType.API -> ClerkResult.apiFailure(this.error)
        ClerkResult.Failure.ErrorType.HTTP ->
          ClerkResult.httpFailure(error = this.error, code = this.code!!)
        ClerkResult.Failure.ErrorType.UNKNOWN -> this
      }
    }
  }
}

internal fun ClerkResult<SignUp, ClerkErrorResponse>.signUpToOAuthResult():
  ClerkResult<OAuthResult, ClerkErrorResponse> {
  return when (this) {
    is ClerkResult.Success -> {
      ClerkResult.success(OAuthResult(signUp = this.value))
    }

    is ClerkResult.Failure -> {
      when (this.errorType) {
        ClerkResult.Failure.ErrorType.API -> ClerkResult.apiFailure(this.error)
        ClerkResult.Failure.ErrorType.HTTP ->
          ClerkResult.httpFailure(error = this.error, code = this.code!!)

        ClerkResult.Failure.ErrorType.UNKNOWN -> this
      }
    }
  }
}

internal suspend fun ClerkResult<SignUp, ClerkErrorResponse>.signUpToOAuthResultWithTransfer():
  ClerkResult<OAuthResult, ClerkErrorResponse> {
  return when (this) {
    is ClerkResult.Success -> this.value.toOAuthResultWithTransfer()
    is ClerkResult.Failure -> this.signUpToOAuthResult()
  }
}

private suspend fun SignUp.toOAuthResultWithTransfer():
  ClerkResult<OAuthResult, ClerkErrorResponse> {
  return if (needsTransferToSignIn) {
    SignIn.create(SignIn.CreateParams.Strategy.Transfer()).signInToOAuthResult()
  } else {
    ClerkResult.success(OAuthResult(signUp = this))
  }
}

private val SignUp.needsTransferToSignIn: Boolean
  get() = verifications["external_account"]?.status == Verification.Status.TRANSFERABLE
