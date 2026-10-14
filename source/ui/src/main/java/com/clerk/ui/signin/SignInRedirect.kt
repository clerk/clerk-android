package com.clerk.ui.signin

import com.clerk.api.network.model.error.ClerkErrorResponse
import com.clerk.api.network.serialization.ClerkResult
import com.clerk.api.signin.SignIn
import com.clerk.api.signin.authenticateWithOAuth
import com.clerk.api.sso.OAuthProvider
import com.clerk.api.sso.OAuthResult

internal suspend fun authenticateWithRedirect(
  signIn: SignIn,
  provider: OAuthProvider,
  transferable: Boolean,
): ClerkResult<OAuthResult, ClerkErrorResponse> =
  signIn.authenticateWithOAuth(provider = provider, transferable = transferable)
