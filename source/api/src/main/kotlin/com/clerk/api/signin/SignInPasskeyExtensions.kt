package com.clerk.api.signin

import com.clerk.api.network.ClerkApi
import com.clerk.api.network.model.error.ClerkErrorResponse
import com.clerk.api.network.serialization.ClerkResult
import com.clerk.api.passkeys.PasskeyService

/** Prepares an explicit second-factor strategy, including passkeys. */
suspend fun SignIn.prepareSecondFactor(
  strategy: SignIn.PrepareSecondFactorStrategy
): ClerkResult<SignIn, ClerkErrorResponse> {
  if (status != SignIn.Status.NEEDS_SECOND_FACTOR && status != SignIn.Status.NEEDS_CLIENT_TRUST) {
    return invalidPrepareState(
      code = "sign_in_status_invalid",
      longMessage = "Cannot prepare second factor while sign-in status is ${status.name}",
    )
  }

  return ClerkApi.signIn.prepareSecondFactor(id = id, params = strategy.toParams().toMap())
}

/**
 * Authenticates this sign-in with a passkey.
 *
 * If this sign-in requires a second factor and advertises passkey support, the existing sign-in is
 * prepared and attempted as a second factor. Otherwise, this starts the existing first-factor
 * passkey flow.
 */
suspend fun SignIn.authenticateWithPasskey(
  allowedCredentialIds: List<String> = emptyList()
): ClerkResult<SignIn, ClerkErrorResponse> {
  return PasskeyService.authenticateWithPasskey(this, allowedCredentialIds)
}
