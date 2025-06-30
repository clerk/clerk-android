package com.clerk.passkeys

import com.clerk.network.model.error.ClerkErrorResponse
import com.clerk.network.serialization.ClerkResult
import com.clerk.signin.SignIn

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
    allowedCredentialIds: List<String> = emptyList()
  ): ClerkResult<SignIn, ClerkErrorResponse> {
    return GoogleCredentialAuthenticationService.signInWithGoogleCredential(
      allowedCredentialIds,
      listOf(SignIn.CredentialType.PASSKEY),
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
