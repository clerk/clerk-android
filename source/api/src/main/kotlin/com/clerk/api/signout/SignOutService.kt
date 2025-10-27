package com.clerk.api.signout

import com.clerk.api.Clerk
import com.clerk.api.network.ClerkApi
import com.clerk.api.network.model.error.ClerkErrorResponse
import com.clerk.api.network.serialization.ClerkResult
import com.clerk.api.session.delete

/**
 * Service responsible for signing out users by removing their active session.
 *
 * The SignOutService handles the complete sign-out process, including removing the session from the
 * Clerk API and cleaning up local session state. It performs network operations asynchronously and
 * provides proper error handling.
 */
internal object SignOutService {

  /**
   * Signs out the currently authenticated user by removing their active session.
   *
   * This method will attempt to remove the session from the Clerk API if a session ID exists,
   * otherwise it will delete the local session. The operation is performed asynchronously and
   * includes proper error handling.
   *
   * @return A [com.clerk.network.serialization.ClerkResult] indicating the success or failure of
   *   the sign-out operation. Returns
   *   [com.clerk.network.serialization.ClerkResult.Companion.success] with [Unit] on successful
   *   sign-out, or [com.clerk.network.serialization.ClerkResult.Companion.unknownFailure] with
   *   error details on failure.
   */
  suspend fun signOut(): ClerkResult<Unit, ClerkErrorResponse> {
    try {
      if (Clerk.session?.id != null) {
        Clerk.session?.id?.let { sessionId -> ClerkApi.session.removeSession(sessionId) }
      } else {
        Clerk.session?.delete()
      }
      // Clean up local session state immediately after successful logout
      Clerk.clearSessionAndUserState()
      return ClerkResult.success(Unit)
    } catch (e: Exception) {
      return ClerkResult.unknownFailure(Exception(e.message ?: "Unknown error"))
    }
  }
}
