package com.clerk.api.sso

import com.clerk.api.network.model.error.ClerkErrorResponse
import com.clerk.api.network.model.verification.Verification
import com.clerk.api.network.serialization.ClerkResult
import com.clerk.api.network.serialization.shortErrorMessageOrNull
import com.clerk.api.signin.SignIn
import com.clerk.api.signup.SignUp

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

/**
 * Converts a [SignUp] to OAuth output, following the reverse transfer flow used by native ID-token
 * providers when the selected external account already belongs to an existing Clerk user.
 */
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
