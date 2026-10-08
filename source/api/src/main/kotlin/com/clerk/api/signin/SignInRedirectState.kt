package com.clerk.api.signin

import com.clerk.api.network.ClerkApi
import com.clerk.api.network.model.error.ClerkErrorResponse
import com.clerk.api.network.serialization.ClerkResult
import com.clerk.api.redirect.RedirectState

internal suspend fun SignIn.prepareRedirectFirstFactor(
  fields: Map<String, String>
): ClerkResult<SignIn, ClerkErrorResponse> {
  val redirectUrl =
    fields[REDIRECT_URL_FIELD] ?: return ClerkApi.signIn.prepareSignInFirstFactor(id, fields)
  val state = RedirectState.generate()
  val result =
    ClerkApi.signIn.prepareSignInFirstFactor(
      id,
      fields + (REDIRECT_URL_FIELD to RedirectState.withState(redirectUrl, state)),
    )
  if (result is ClerkResult.Success) {
    result.value.firstFactorVerification?.externalVerificationRedirectUrl?.let {
      RedirectState.remember(it, state)
    }
  }
  return result
}

private const val REDIRECT_URL_FIELD = "redirect_url"
