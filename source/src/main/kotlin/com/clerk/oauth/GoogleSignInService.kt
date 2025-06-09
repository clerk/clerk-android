package com.clerk.oauth

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialException
import com.clerk.Clerk
import com.clerk.log.ClerkLog
import com.clerk.network.ClerkApi
import com.clerk.network.model.error.ClerkErrorResponse
import com.clerk.network.serialization.ClerkResult
import com.clerk.signin.SignIn
import com.clerk.signup.SignUp
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import java.util.UUID

/**
 * Service for handling Sign In with Google, previously known as Google One Tap.
 *
 * Note: Before using this service, you must configure Google Sign In in your Clerk Dashboard, as
 * well as add your Google Cloud Client ID to your Google OAuth configuration.
 */
internal object GoogleSignInService {

  suspend fun signInWithGoogle(context: Context): ClerkResult<OAuthResult, ClerkErrorResponse> {
    val oneTapClientId =
      requireNotNull(Clerk.environment.displayConfig.googleOneTapClientId) {
        "Google One Tap is not configured for this application." +
          " Please add a Google One Tap Client ID in your Clerk Dashboard."
      }
    val googleIdOption: GetGoogleIdOption =
      GetGoogleIdOption.Builder()
        .setFilterByAuthorizedAccounts(false)
        .setServerClientId(oneTapClientId)
        .setNonce(UUID.randomUUID().toString())
        .setAutoSelectEnabled(true)
        .build()

    val request: GetCredentialRequest =
      GetCredentialRequest.Builder().addCredentialOption(googleIdOption).build()

    val credentialManager = CredentialManager.create(context)

    return try {
      val result = credentialManager.getCredential(request = request, context = context)
      handleSignInResult(result)
    } catch (e: GetCredentialException) {
      ClerkLog.e("Error retrieving Google ID token: ${e.message}")
      ClerkResult.unknownFailure(e)
    }
  }

  private suspend fun handleSignInResult(
    result: GetCredentialResponse
  ): ClerkResult<OAuthResult, ClerkErrorResponse> {
    val credential = result.credential
    return if (
      credential is CustomCredential &&
        credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
    ) {
      val idToken = GoogleIdTokenCredential.createFrom(credential.data).idToken

      // First try to authenticate (sign in)
      val authResult: ClerkResult<SignIn, ClerkErrorResponse> =
        ClerkApi.instance.authenticateWithGoogle(token = idToken)

      // Handle the result
      when (authResult) {
        is ClerkResult.Success -> authResult.signInToOAuthResult()
        is ClerkResult.Failure -> {
          // Check if we need to create a new account instead
          if (authResult.error?.errors?.first()?.code == "external_account_not_found") {
            // Account doesn't exist, so create it via sign up
            SignUp.create(SignUp.CreateParams.GoogleOneTap(token = idToken)).signUpToOAuthResult()
          } else {
            // Some other error occurred
            authResult.signInToOAuthResult()
          }
        }
      }
    } else {
      ClerkResult.unknownFailure(error("Unsupported credential type: ${credential.type}"))
    }
  }
}
