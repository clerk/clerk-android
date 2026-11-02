package com.clerk.api.passkeys

import com.clerk.api.network.model.error.ClerkErrorResponse
import com.clerk.api.network.serialization.ClerkResult
import com.clerk.api.session.Session
import com.clerk.api.session.SessionVerification
import com.clerk.api.signin.SignIn

internal object PasskeyService {

  suspend fun signInWithPasskey(
    allowedCredentialIds: List<String> = emptyList(),
    preferImmediatelyAvailableCredentials: Boolean = false,
  ): ClerkResult<SignIn, ClerkErrorResponse> {
    return GoogleCredentialAuthenticationService.signInWithGoogleCredential(
      credentialTypes = listOf(SignIn.CredentialType.PASSKEY),
      allowedCredentialIds = allowedCredentialIds,
      preferImmediatelyAvailableCredentials = preferImmediatelyAvailableCredentials,
    )
  }

  suspend fun authenticateWithPasskey(
    signIn: SignIn,
    allowedCredentialIds: List<String> = emptyList(),
  ): ClerkResult<SignIn, ClerkErrorResponse> {
    return GoogleCredentialAuthenticationService.authenticateWithPasskey(
      signIn = signIn,
      allowedCredentialIds = allowedCredentialIds,
    )
  }

  suspend fun verifySessionWithPasskey(
    session: Session,
    allowedCredentialIds: List<String> = emptyList(),
    level: SessionVerification.Level = SessionVerification.Level.FIRST_FACTOR,
  ): ClerkResult<SessionVerification, ClerkErrorResponse> {
    return GoogleCredentialAuthenticationService.verifySessionWithPasskey(
      session = session,
      allowedCredentialIds = allowedCredentialIds,
      level = level,
    )
  }

  suspend fun createPasskey(): ClerkResult<Passkey, ClerkErrorResponse> {
    return PasskeyCreationService.createPasskey()
  }
}
