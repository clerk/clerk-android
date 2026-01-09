package com.clerk.api.auth

import android.net.Uri
import com.clerk.api.Clerk
import com.clerk.api.network.ClerkApi
import com.clerk.api.network.model.error.ClerkErrorResponse
import com.clerk.api.network.serialization.ClerkResult
import com.clerk.api.session.GetTokenOptions
import com.clerk.api.session.Session
import com.clerk.api.session.fetchToken
import com.clerk.api.session.revoke
import com.clerk.api.signout.SignOutService
import com.clerk.api.sso.SSOService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Main Auth class providing all authentication entry points.
 *
 * Access via `Clerk.auth`.
 *
 * This class provides a centralized, DSL-style API for all authentication operations including:
 * - Sign in with various methods (password, OTP, OAuth, passkey, etc.)
 * - Sign up with various methods
 * - Session management (sign out, set active session, get tokens)
 * - Deep link handling for OAuth/SSO callbacks
 *
 * ### Example usage:
 * ```kotlin
 * // Sign in with email (requires separate sendCode call)
 * val signIn = clerk.auth.signIn { email = "user@email.com" }
 *
 * // Sign in with password
 * val signIn = clerk.auth.signInWithPassword {
 *     identifier = "user@email.com"
 *     password = "password"
 * }
 *
 * // Sign in with OTP (automatically sends code)
 * val signIn = clerk.auth.signInWithOtp { email = "user@email.com" }
 *
 * // Sign out
 * clerk.auth.signOut()
 * ```
 */
class Auth internal constructor() {

  private val _events = MutableSharedFlow<AuthEvent>()

  /**
   * Flow of authentication events.
   *
   * Subscribe to this flow to receive notifications about authentication state changes, including
   * sign-in, sign-out, session changes, and errors.
   */
  val events: Flow<AuthEvent> = _events.asSharedFlow()

  // region Session Management

  /**
   * Signs out the current session or a specific session.
   *
   * @param sessionId Optional session ID to sign out. If null, signs out the current session.
   * @return A [ClerkResult] with Unit on success, or a [ClerkErrorResponse] on failure.
   *
   * ### Example usage:
   * ```kotlin
   * clerk.auth.signOut()
   * ```
   */
  suspend fun signOut(sessionId: String? = null): ClerkResult<Unit, ClerkErrorResponse> {
    return if (sessionId != null) {
      // Sign out a specific session
      when (val result = ClerkApi.session.removeSession(sessionId)) {
        is ClerkResult.Success -> ClerkResult.success(Unit)
        is ClerkResult.Failure -> ClerkResult.apiFailure(result.error)
      }
    } else {
      SignOutService.signOut()
    }
  }

  /**
   * Sets the active session.
   *
   * @param sessionId The ID of the session to set as active.
   * @param organizationId Optional organization ID to set as active for the session.
   * @return A [ClerkResult] containing the [Session] on success, or a [ClerkErrorResponse] on
   *   failure.
   *
   * ### Example usage:
   * ```kotlin
   * clerk.auth.setActive(sessionId, organizationId)
   * ```
   */
  suspend fun setActive(
    sessionId: String,
    organizationId: String? = null,
  ): ClerkResult<Session, ClerkErrorResponse> {
    return ClerkApi.client.setActive(sessionId, organizationId)
  }

  /**
   * Gets a token for the current session.
   *
   * @param options Optional token retrieval options.
   * @return A [ClerkResult] containing the token string on success, or a [ClerkErrorResponse] on
   *   failure.
   *
   * ### Example usage:
   * ```kotlin
   * val token = clerk.auth.getToken()
   * // or with options
   * val token = clerk.auth.getToken(GetTokenOptions(template = "my-template"))
   * ```
   */
  suspend fun getToken(options: GetTokenOptions? = null): ClerkResult<String, ClerkErrorResponse> {
    val session =
      Clerk.session
        ?: return ClerkResult.apiFailure(
          ClerkErrorResponse(errors = emptyList(), clerkTraceId = "no-session")
        )

    return when (val result = session.fetchToken(options ?: GetTokenOptions())) {
      is ClerkResult.Success -> ClerkResult.success(result.value.jwt)
      is ClerkResult.Failure -> ClerkResult.apiFailure(result.error)
    }
  }

  /**
   * Revokes a session.
   *
   * @param session The session to revoke.
   * @return A [ClerkResult] with Unit on success, or a [ClerkErrorResponse] on failure.
   *
   * ### Example usage:
   * ```kotlin
   * clerk.auth.revokeSession(session)
   * ```
   */
  suspend fun revokeSession(session: Session): ClerkResult<Unit, ClerkErrorResponse> {
    return when (val result = session.revoke()) {
      is ClerkResult.Success -> ClerkResult.success(Unit)
      is ClerkResult.Failure -> ClerkResult.apiFailure(result.error)
    }
  }

  // endregion

  // region Deep Link Handling

  /**
   * Handles OAuth/SSO deep link callbacks.
   *
   * Call this method from your Activity when receiving a deep link callback from an OAuth or SSO
   * provider.
   *
   * @param uri The deep link URI received from the callback.
   * @return true if the URI was handled, false otherwise.
   *
   * ### Example usage:
   * ```kotlin
   * // In your Activity's onCreate or onNewIntent
   * clerk.auth.handle(intent.data)
   * ```
   */
  fun handle(uri: Uri?): Boolean {
    // Check if this is a Clerk OAuth callback
    val isClerkCallback = uri?.scheme?.startsWith("clerk") == true

    if (isClerkCallback) {
      // Let the SSO service handle the callback
      kotlinx.coroutines.runBlocking { SSOService.completeAuthenticateWithRedirect(uri) }
    }

    return isClerkCallback
  }

  // endregion
}
