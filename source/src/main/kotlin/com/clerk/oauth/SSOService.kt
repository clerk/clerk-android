package com.clerk.oauth

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.net.toUri
import com.clerk.Clerk
import com.clerk.log.ClerkLog
import com.clerk.network.ClerkApi
import com.clerk.network.model.error.ClerkErrorResponse
import com.clerk.network.serialization.ClerkResult
import com.clerk.oauth.SSOService.authenticateWithRedirect
import com.clerk.signin.SignIn
import com.clerk.signin.get
import com.clerk.signup.SignUp
import kotlinx.coroutines.CompletableDeferred

internal object SSOService {
  private var currentPendingAuth:
    CompletableDeferred<ClerkResult<OAuthResult, ClerkErrorResponse>>? =
    null
  private var currentSignInId: String? = null

  /**
   * Handles OAuth authentication flows for the Clerk SDK
   *
   * This service manages redirect-based authentication flows, including initiating OAuth,
   * Enterprise SSO, and handling callback URIs to complete the authentication process. It provides
   * methods to start, complete, or cancel authentication flows that require user redirection to
   * external providers (e.g., Google, Facebook).
   *
   * For redirect-based flows, this service uses [SSOReceiverActivity] to intercept the redirect URI
   * and finalize the sign-in process.
   *
   * Note: We handle sign in with google nee Google One Tap, in [GoogleSignInService]
   */
  suspend fun authenticateWithRedirect(
    context: Context,
    params: SignIn.AuthenticateWithRedirectParams,
  ): ClerkResult<OAuthResult, ClerkErrorResponse> {
    // Clear any existing pending auth to prevent conflicts
    currentPendingAuth?.complete(
      ClerkResult.unknownFailure(
        Exception("New authentication started, cancelling previous attempt")
      )
    )
    clearCurrentAuth()

    val initialResult =
      ClerkApi.signIn.authenticateWithRedirect(
        strategy = params.provider.strategy,
        redirectUrl = params.redirectUrl,
      )

    return when (initialResult) {
      is ClerkResult.Failure -> {
        val message = initialResult.error?.errors?.first()?.message
        ClerkLog.e("Failed to authenticate with redirect: $message")
        ClerkResult.apiFailure(initialResult.error)
      }
      is ClerkResult.Success -> {
        ClerkLog.d("Successfully authenticated with redirect: $initialResult")
        val externalUrl =
          requireNotNull(
            initialResult.value.firstFactorVerification?.externalVerificationRedirectUrl
          ) {
            "External URL cannot be null"
          }

        val signInId = initialResult.value.id
        val completableDeferred =
          CompletableDeferred<ClerkResult<OAuthResult, ClerkErrorResponse>>()

        currentPendingAuth = completableDeferred
        currentSignInId = signInId

        val intent =
          Intent(context, SSOReceiverActivity::class.java).apply { data = externalUrl.toUri() }
        context.startActivity(intent)

        // This will suspend until completeAuthenticateWithRedirect is called
        completableDeferred.await()
      }
    }
  }

  /**
   * Completes the authentication flow initiated by [authenticateWithRedirect] when the user is
   * redirected back to the app after completing external authentication (e.g., OAuth or SSO
   * provider).
   *
   * This method is typically triggered internally via [SSOReceiverActivity] when the app receives a
   * redirect URI containing authentication results. It processes the redirect URI to retrieve the
   * sign-in result, resolves the corresponding pending [CompletableDeferred], and updates the
   * sign-in state.
   *
   * @param uri The redirect URI received after completion of the external authentication flow.
   *   Expected to contain a `rotating_token_nonce` query parameter to associate with the pending
   *   sign-in request.
   */
  @Suppress("TooGenericExceptionCaught")
  suspend fun completeAuthenticateWithRedirect(uri: Uri) {
    try {
      ClerkLog.d("Completing authentication with redirect: $uri")

      if (currentPendingAuth == null) {
        ClerkLog.w("No pending authentication found for redirect: $uri")
        return
      }

      val nonce = uri.getQueryParameter("rotating_token_nonce")

      // It's a sign up, so call for a transfer
      if (nonce == null) {
        handleSignUpTransfer()
      } else {
        handleSignIn(nonce)
      }
    } catch (e: Exception) {
      ClerkLog.e("Error completing authentication with redirect: ${e.message}")
      currentPendingAuth?.complete(ClerkResult.unknownFailure(e))
      clearCurrentAuth()
    }
  }

  private suspend fun handleSignIn(nonce: String) {
    val signInResult =
      requireNotNull(Clerk.signIn).get(rotatingTokenNonce = nonce).signInToOAuthResult()
    currentPendingAuth?.complete(signInResult)

    clearCurrentAuth()
  }

  private suspend fun handleSignUpTransfer() {
    ClerkLog.d("Handling sign-up transfer")
    val createResult = SignUp.create(SignUp.CreateParams.Transfer).signUpToOAuthResult()
    currentPendingAuth?.complete(createResult)

    clearCurrentAuth()
  }

  /**
   * Clears the current authentication state. Should be called when authentication completes
   * (successfully or with error).
   */
  private fun clearCurrentAuth() {
    currentPendingAuth = null
    currentSignInId = null
  }

  /**
   * Cancels any pending authentication flow. Useful for cleanup when the authentication process
   * needs to be aborted.
   */
  fun cancelPendingAuthentication() {
    currentPendingAuth?.complete(
      ClerkResult.Companion.unknownFailure(Exception("Authentication cancelled"))
    )
    clearCurrentAuth()
  }

  /** Returns true if there's currently a pending authentication flow. */
  fun hasPendingAuthentication(): Boolean {
    return currentPendingAuth != null
  }
}
