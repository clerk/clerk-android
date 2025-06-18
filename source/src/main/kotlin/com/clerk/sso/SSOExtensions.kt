package com.clerk.sso

import com.clerk.network.model.error.ClerkErrorResponse
import com.clerk.network.serialization.ClerkResult
import com.clerk.network.serialization.shortErrorMessageOrNull
import com.clerk.signin.SignIn
import com.clerk.signup.SignUp

/**
 * Converts a [ClerkResult] of [SignIn] to a [ClerkResult] of [OAuthResult].
 *
 * Since Clerk handles the transfer flow internally (i.e. moving a SignIn to a SignUp) this handles
 * the case where calling [SignIn.create] returns a SignUp instead.
 */
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
        ClerkResult.Failure.ErrorType.UNKNOWN ->
          ClerkResult.unknownFailure(error("${this.shortErrorMessageOrNull()}"))
      }
    }
  }
}

/**
 * Converts a [ClerkResult] of [SignUp] to a [ClerkResult] of [OAuthResult].
 *
 * Since Clerk handles the transfer flow internally (i.e. moving a SignIn to a SignUp) this handles
 * the case where calling [SignIn.create] returns a SignUp instead.
 */
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

        ClerkResult.Failure.ErrorType.UNKNOWN ->
          ClerkResult.unknownFailure(error("${this.shortErrorMessageOrNull()}"))
      }
    }
  }
}
