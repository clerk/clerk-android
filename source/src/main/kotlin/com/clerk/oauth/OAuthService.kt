package com.clerk.oauth

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.net.toUri
import com.clerk.Clerk
import com.clerk.log.ClerkLog
import com.clerk.model.error.ClerkErrorResponse
import com.clerk.network.ClerkApi
import com.clerk.network.serialization.ClerkResult
import com.clerk.oauth.OAuthService.authenticateWithRedirect
import com.clerk.signin.SignIn
import com.clerk.signin.get
import com.clerk.signin.toSSOResult
import com.clerk.signup.SignUp
import com.clerk.signup.toSSOResult
import kotlinx.coroutines.CompletableDeferred

internal object OAuthService {
  private var currentPendingAuth:
    CompletableDeferred<ClerkResult<OAuthResult, ClerkErrorResponse>>? =
    null
  private var currentSignInId: String? = null

  /**
   * Handles Single Sign-On (SSO) and OAuth authentication flows for the Clerk
   *
   * This service manages redirect-based authentication flows, including initiating OAuth,
   * Enterprise SSO, and handling callback URIs to complete the authentication process. It provides
   * methods to start, complete, or cancel authentication flows that require user redirection to
   * external providers (e.g., Google, Facebook).
   *
   * For redirect-based flows, this service uses [OAuthReceiverActivity] to intercept the redirect
   * URI and finalize the sign-in process.
   */
  suspend fun authenticateWithRedirect(
    context: Context,
    params: SignIn.AuthenticateWithRedirectParams,
  ): ClerkResult<OAuthResult, ClerkErrorResponse> {
    // Clear any existing pending auth to prevent conflicts
    currentPendingAuth?.complete(
      ClerkResult.Companion.unknownFailure(
        Exception("New authentication started, cancelling previous attempt")
      )
    )
    clearCurrentAuth()

    val initialResult =
      ClerkApi.instance.authenticateWithRedirect(
        strategy = params.provider.strategy,
        redirectUrl = params.redirectUrl,
      )

    return when (initialResult) {
      is ClerkResult.Failure -> {
        val message = initialResult.error?.errors?.first()?.message
        ClerkLog.e("Failed to authenticate with redirect: $message")
        ClerkResult.Companion.apiFailure(initialResult.error)
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
          Intent(context, OAuthReceiverActivity::class.java).apply { data = externalUrl.toUri() }
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
   * This method is typically triggered internally via [OAuthReceiverActivity] when the app receives
   * a redirect URI containing authentication results. It processes the redirect URI to retrieve the
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
      currentPendingAuth?.complete(ClerkResult.Companion.unknownFailure(e))
      clearCurrentAuth()
    }
  }

  private suspend fun handleSignIn(nonce: String) {
    val signInResult = requireNotNull(Clerk.signIn).get(rotatingTokenNonce = nonce)

    when (signInResult) {
      is ClerkResult.Success -> {
        ClerkLog.d("Successfully completed sign-in with nonce: $nonce")
        currentPendingAuth?.complete(
          ClerkResult.Companion.success(signInResult.value.toSSOResult())
        )
        clearCurrentAuth()
      }

      is ClerkResult.Failure -> {
        val errorMessage = signInResult.error?.errors?.first()?.longMessage
        ClerkLog.e(
          "Failed to complete sign-in with rotating token nonce $nonce, error: $errorMessage"
        )
        currentPendingAuth?.complete(ClerkResult.Companion.apiFailure(signInResult.error))
        clearCurrentAuth()
      }
    }
  }

  private suspend fun handleSignUpTransfer() {
    ClerkLog.d("Handling sign-up transfer")
    val createResult = SignUp.Companion.create(SignUp.SignUpCreateParams.Transfer)

    when (createResult) {
      is ClerkResult.Success -> {
        ClerkLog.d("Successfully completed sign-up transfer")
        currentPendingAuth?.complete(
          ClerkResult.Companion.success(createResult.value.toSSOResult())
        )
        clearCurrentAuth()
      }

      is ClerkResult.Failure -> {
        val errorMessage = createResult.error?.errors?.first()?.longMessage
        ClerkLog.e("Failed to complete sign-up transfer, error: $errorMessage")
        currentPendingAuth?.complete(ClerkResult.Companion.apiFailure(createResult.error))
        clearCurrentAuth()
      }
    }
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
