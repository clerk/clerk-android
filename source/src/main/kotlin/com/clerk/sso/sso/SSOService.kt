package com.clerk.sso.sso

import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.net.Uri
import androidx.core.net.toUri
import com.clerk.Clerk
import com.clerk.externalaccount.ExternalAccount
import com.clerk.externalaccount.ExternalAccountService
import com.clerk.log.ClerkLog
import com.clerk.network.ClerkApi
import com.clerk.network.model.error.ClerkErrorResponse
import com.clerk.network.serialization.ClerkResult
import com.clerk.network.serialization.longErrorMessageOrNull
import com.clerk.signin.SignIn
import com.clerk.signin.get
import com.clerk.signup.SignUp
import com.clerk.sso.RedirectConfiguration
import com.clerk.sso.sso.SSOService.authenticateWithRedirect
import com.clerk.user.User.CreateExternalAccountParams
import kotlinx.coroutines.CompletableDeferred

/**
 * Internal service that handles OAuth authentication flows for the Clerk SDK.
 *
 * This service manages redirect-based authentication flows, including initiating OAuth, Enterprise
 * SSO, and handling callback URIs to complete the authentication process. It provides methods to
 * start, complete, or cancel authentication flows that require user redirection to external
 * providers (e.g., Google, Facebook, GitHub, etc.).
 *
 * The service maintains internal state to track pending authentication attempts and uses
 * [SSOReceiverActivity] to intercept redirect URIs and finalize the authentication process.
 *
 * ## Key Features:
 * - OAuth provider authentication (Google, Facebook, GitHub, etc.)
 * - Enterprise SSO authentication flows
 * - Automatic cleanup of pending authentication state
 * - Error handling and logging throughout the flow
 *
 * For external account connections to existing users, see [ExternalAccountService]. For Google One
 * Tap authentication, see [com.clerk.sso.GoogleSignInService].
 */
internal object SSOService {
  /**
   * Tracks the current pending OAuth authentication flow. This deferred completes when the
   * authentication process finishes (success or failure).
   */
  private var currentPendingAuth:
    CompletableDeferred<ClerkResult<OAuthResult, ClerkErrorResponse>>? =
    null

  /**
   * Initiates an OAuth authentication flow with redirect to an external provider.
   *
   * This method handles redirect-based authentication flows by:
   * 1. Starting the authentication request with the specified strategy and redirect URL
   * 2. Launching the external provider's authentication page via [SSOReceiverActivity]
   * 3. Suspending until the user completes authentication and returns to the app
   * 4. Processing the authentication result and returning the appropriate [ClerkResult]
   *
   * The method automatically cancels any existing pending authentication to prevent conflicts.
   *
   * @param params The [SignIn.AuthenticateWithRedirectParams] for the authentication request.
   * @return A [ClerkResult] containing the [OAuthResult] on success, or [ClerkErrorResponse] on
   *   failure
   */
  suspend fun authenticateWithRedirect(
    strategy: String,
    redirectUrl: String = RedirectConfiguration.DEFAULT_REDIRECT_URL,
    identifier: String? = null,
    emailAddress: String? = null,
    legalAccepted: Boolean? = null,
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
        strategy = strategy,
        redirectUrl = redirectUrl,
        identifier = identifier,
        emailAddress = emailAddress,
        legalAccepted = legalAccepted,
      )

    return when (initialResult) {
      is ClerkResult.Failure -> {
        val message = initialResult.longErrorMessageOrNull
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

        val completableDeferred =
          CompletableDeferred<ClerkResult<OAuthResult, ClerkErrorResponse>>()

        currentPendingAuth = completableDeferred

        val intent =
          Intent(Clerk.applicationContext?.get(), SSOReceiverActivity::class.java).apply {
            data = externalUrl.toUri()
            addFlags(FLAG_ACTIVITY_NEW_TASK)
          }
        Clerk.applicationContext?.get()?.startActivity(intent)

        // This will suspend until completeAuthenticateWithRedirect is called
        completableDeferred.await()
      }
    }
  }

  /**
   * Completes the authentication flow initiated by [authenticateWithRedirect].
   *
   * This method is called when the user is redirected back to the app after completing external
   * authentication (e.g., OAuth or SSO provider). It processes the redirect URI to retrieve the
   * authentication result and resolves the pending authentication flow.
   *
   * The method handles two authentication scenarios:
   * 1. **Sign In**: When the URI contains a `rotating_token_nonce` parameter
   * 2. **Sign Up Transfer**: When no nonce is present, indicating a new user registration
   *
   * This method is typically triggered internally via [SSOReceiverActivity] when the app receives a
   * redirect URI containing authentication results.
   *
   * @param uri The redirect URI received after completion of the external authentication flow.
   *   Expected to contain a `rotating_token_nonce` query parameter for sign-in flows.
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

  /**
   * Initiates the process of connecting an external account to an existing user.
   *
   * This method delegates to [ExternalAccountService.connectExternalAccount] to handle the external
   * account connection process.
   *
   * @param params The parameters for creating the external account connection, including the OAuth
   *   provider and optional redirect URLs
   * @return A [ClerkResult] containing the connected [ExternalAccount] on success, or
   *   [ClerkErrorResponse] on failure
   * @see ExternalAccountService.connectExternalAccount
   */
  suspend fun connectExternalAccount(
    params: CreateExternalAccountParams
  ): ClerkResult<ExternalAccount, ClerkErrorResponse> {
    return ExternalAccountService.connectExternalAccount(params)
  }

  /**
   * Completes the external account connection process.
   *
   * This method delegates to [ExternalAccountService.completeExternalConnection] to handle the
   * completion of external account connections.
   *
   * @see ExternalAccountService.completeExternalConnection
   */
  suspend fun completeExternalConnection() {
    ExternalAccountService.completeExternalConnection()
  }

  /**
   * Handles the sign-in completion process using the provided rotating token nonce.
   *
   * This method is called when a redirect URI contains a `rotating_token_nonce` parameter,
   * indicating that the user is completing a sign-in flow. It uses the nonce to fetch the updated
   * sign-in state and converts it to an OAuth result.
   *
   * @param nonce The rotating token nonce from the redirect URI, used to identify and retrieve the
   *   specific sign-in attempt
   */
  private suspend fun handleSignIn(nonce: String) {
    val signInResult =
      requireNotNull(Clerk.signIn).get(rotatingTokenNonce = nonce).signInToOAuthResult()
    currentPendingAuth?.complete(signInResult)

    clearCurrentAuth()
  }

  /**
   * Handles the sign-up transfer process for new user registrations.
   *
   * This method is called when a redirect URI does not contain a rotating token nonce, indicating
   * that a new user is completing a sign-up flow via OAuth. It creates a transfer-type sign-up to
   * complete the registration process.
   */
  private suspend fun handleSignUpTransfer() {
    ClerkLog.d("Handling sign-up transfer")
    val createResult = SignUp.create(SignUp.CreateParams.Transfer).signUpToOAuthResult()
    currentPendingAuth?.complete(createResult)

    clearCurrentAuth()
  }

  /**
   * Clears the current authentication state.
   *
   * This method resets the pending authentication deferred to null, effectively cleaning up the
   * internal state. Should be called when authentication completes (successfully or with error) to
   * prevent memory leaks and state conflicts.
   */
  private fun clearCurrentAuth() {
    currentPendingAuth = null
  }

  /**
   * Cancels any pending authentication flow.
   *
   * This method completes any pending authentication with a cancellation error and clears the
   * authentication state. Useful for cleanup when the authentication process needs to be aborted
   * (e.g., user navigates away, app is backgrounded, etc.).
   */
  fun cancelPendingAuthentication() {
    currentPendingAuth?.complete(
      ClerkResult.Companion.unknownFailure(Exception("Authentication cancelled"))
    )
    clearCurrentAuth()
  }

  /**
   * Checks if there's currently a pending authentication flow.
   *
   * @return `true` if there's an active authentication flow waiting for completion, `false`
   *   otherwise
   */
  fun hasPendingAuthentication(): Boolean {
    return currentPendingAuth != null
  }

  /**
   * Checks if there's currently a pending external account connection.
   *
   * This method delegates to [ExternalAccountService.hasPendingExternalAccountConnection] to check
   * the external account connection state.
   *
   * @return `true` if there's an active external account connection waiting for completion, `false`
   *   otherwise
   * @see ExternalAccountService.hasPendingExternalAccountConnection
   */
  fun hasPendingExternalAccountConnection(): Boolean {
    return ExternalAccountService.hasPendingExternalAccountConnection()
  }
}
