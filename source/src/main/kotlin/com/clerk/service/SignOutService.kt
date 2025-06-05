@file:Suppress("TooGenericExceptionCaught")

package com.clerk.service

import com.clerk.Clerk
import com.clerk.model.error.ClerkErrorResponse
import com.clerk.model.session.delete
import com.clerk.network.ClerkApi
import com.clerk.network.serialization.ClerkResult

/**
 * Service responsible for signing out users by removing their active session.
 *
 * The SignOutService handles the complete sign-out process, including removing the session from the
 * Clerk API and cleaning up local session state. It performs network operations asynchronously and
 * provides proper error handling.
 */
object SignOutService {

  /**
   * Signs out the currently authenticated user by removing their active session.
   *
   * This method will attempt to remove the session from the Clerk API if a session ID exists,
   * otherwise it will delete the local session. The operation is performed asynchronously and
   * includes proper error handling.
   *
   * @return A [ClerkResult] indicating the success or failure of the sign-out operation. Returns
   *   [ClerkResult.success] with [Unit] on successful sign-out, or [ClerkResult.unknownFailure]
   *   with error details on failure.
   */
  suspend fun signOut(): ClerkResult<Unit, ClerkErrorResponse> {
    try {
      if (Clerk.session?.id != null) {
        Clerk.session?.id?.let { sessionId -> ClerkApi.instance.removeSession(sessionId) }
      } else {
        Clerk.session?.delete()
      }
      return ClerkResult.success(Unit)
    } catch (e: Exception) {
      return ClerkResult.unknownFailure(error(e.message ?: "Unknown error"))
    }
  }
}
