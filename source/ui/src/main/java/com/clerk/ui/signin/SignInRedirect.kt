package com.clerk.ui.signin

import com.clerk.api.network.model.error.ClerkErrorResponse
import com.clerk.api.network.serialization.ClerkResult
import com.clerk.api.signin.SignIn
import com.clerk.api.signin.authenticateWithPreparedRedirect
import com.clerk.api.signin.prepareFirstFactor
import com.clerk.api.sso.OAuthProvider
import com.clerk.api.sso.OAuthResult

internal suspend fun authenticateWithRedirect(
  signIn: SignIn?,
  provider: OAuthProvider,
  transferable: Boolean,
): ClerkResult<OAuthResult, ClerkErrorResponse> {
  val params = SignIn.AuthenticateWithRedirectParams.OAuth(provider)
  if (signIn == null) {
    return SignIn.authenticateWithRedirect(params, transferable)
  }

  return when (
    val prepareResult =
      signIn.prepareFirstFactor(
        SignIn.PrepareFirstFactorParams.OAuth(
          strategy = provider.strategy,
          redirectUrl = params.redirectUrl,
        )
      )
  ) {
    is ClerkResult.Failure -> prepareResult
    is ClerkResult.Success ->
      prepareResult.value.authenticateWithPreparedRedirect(transferable = transferable)
  }
}
