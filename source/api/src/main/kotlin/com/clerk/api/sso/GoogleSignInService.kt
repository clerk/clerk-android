package com.clerk.api.sso

import android.app.Activity
import androidx.credentials.Credential
import androidx.credentials.CustomCredential
import androidx.credentials.exceptions.GetCredentialException
import com.clerk.api.log.ClerkLog
import com.clerk.api.network.ClerkApi
import com.clerk.api.network.model.error.ClerkErrorResponse
import com.clerk.api.network.serialization.ClerkResult
import com.clerk.api.signin.SignIn
import com.clerk.api.signup.SignUp
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential

/**
 * Service for handling Sign In with Google, previously known as Google One Tap.
 *
 * Note: Before using this service, you must configure Google Sign In in your Clerk Dashboard, as
 * well as add your Google Cloud Client ID to your Google OAuth configuration.
 */
internal class GoogleSignInService(
  val googleCredentialManager: GoogleCredentialManager = GoogleCredentialManagerImpl()
) {

  suspend fun signInWithGoogle(activity: Activity): ClerkResult<OAuthResult, ClerkErrorResponse> {

    return try {
      val result = googleCredentialManager.getSignInWithGoogleCredential(activity)
      handleSignInResult(result.credential)
    } catch (e: GetCredentialException) {
      ClerkLog.e("Error retrieving Google ID token: ${e.message}")
      ClerkResult.unknownFailure(e)
    }
  }

  suspend fun handleSignInResult(
    credential: Credential
  ): ClerkResult<OAuthResult, ClerkErrorResponse> {
    return if (
      credential is CustomCredential &&
        credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
    ) {
      val idToken = googleCredentialManager.getIdTokenFromCredential(credential.data)

      // First try to authenticate (sign in)
      val authResult: ClerkResult<SignIn, ClerkErrorResponse> =
        ClerkApi.signIn.authenticateWithGoogle(token = idToken)

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
      ClerkResult.unknownFailure(
        IllegalStateException("Unsupported credential type: ${credential.type}")
      )
    }
  }
}
