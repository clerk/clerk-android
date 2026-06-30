package com.clerk.api.passkeys

import com.clerk.api.network.model.error.ClerkErrorResponse
import com.clerk.api.network.serialization.ClerkResult
import com.clerk.api.session.Session
import com.clerk.api.session.SessionVerification
import com.clerk.api.signin.SignIn

/**
 * Main entry point for passkey-related operations.
 *
 * This service acts as a facade that delegates to specialized services for passkey creation and
 * authentication operations.
 */
internal object PasskeyService {

  /**
   * Initiates a sign-in process using passkeys.
   *
   * @param allowedCredentialIds Optional list of credential IDs to filter available passkeys. If
   *   empty, all available passkeys will be considered.
   * @return A [ClerkResult] containing either a successful [SignIn] or an error response.
   */
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

  /**
   * Completes an in-session reverification flow using passkeys.
   *
   * @param session The session that should be reverified.
   * @param allowedCredentialIds Optional list of credential IDs to filter available passkeys.
   * @return A [ClerkResult] containing the resulting [SessionVerification] on success, or an error.
   */
  suspend fun verifySessionWithPasskey(
    session: Session,
    allowedCredentialIds: List<String> = emptyList(),
  ): ClerkResult<SessionVerification, ClerkErrorResponse> {
    return GoogleCredentialAuthenticationService.verifySessionWithPasskey(
      session = session,
      allowedCredentialIds = allowedCredentialIds,
    )
  }

  /**
   * Creates a new passkey for the current user.
   *
   * This method initiates the passkey creation flow, which involves:
   * 1. Requesting passkey creation from the Clerk API
   * 2. Using the Android Credential Manager to create the credential
   * 3. Verifying the created passkey with the Clerk API
   *
   * @return A [ClerkResult] containing the created [Passkey] object on success, or a
   *   [ClerkErrorResponse] on failure
   */
  suspend fun createPasskey(): ClerkResult<Passkey, ClerkErrorResponse> {
    return PasskeyCreationService.createPasskey()
  }
}
