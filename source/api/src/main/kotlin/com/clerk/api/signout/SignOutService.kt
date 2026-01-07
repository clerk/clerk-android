package com.clerk.api.signout

import com.clerk.api.Clerk
import com.clerk.api.log.ClerkLog
import com.clerk.api.network.ClerkApi
import com.clerk.api.network.model.error.ClerkErrorResponse
import com.clerk.api.network.serialization.ClerkResult
import com.clerk.api.session.delete
import com.clerk.api.storage.StorageHelper
import com.clerk.api.storage.StorageKey

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
   * otherwise it will delete the local session. Local credentials are always cleared regardless of
   * whether the server-side sign-out succeeds, ensuring users are never stuck in a logged-in state
   * after attempting to sign out.
   *
   * @return A [com.clerk.network.serialization.ClerkResult] indicating the success or failure of
   *   the sign-out operation. Returns
   *   [com.clerk.network.serialization.ClerkResult.Companion.success] with [Unit] on successful
   *   sign-out, or [com.clerk.network.serialization.ClerkResult.Companion.unknownFailure] with
   *   error details on failure. Note: local credentials are cleared in both cases.
   */
  suspend fun signOut(): ClerkResult<Unit, ClerkErrorResponse> {
    var serverError: Exception? = null

    try {
      if (Clerk.session?.id != null) {
        Clerk.session?.id?.let { sessionId -> ClerkApi.session.removeSession(sessionId) }
      } else {
        Clerk.session?.delete()
      }
    } catch (e: Exception) {
      ClerkLog.w("Server sign-out failed: ${e.message}")
      serverError = e
    } finally {
      // Always clear local credentials regardless of server response
      StorageHelper.deleteValue(StorageKey.DEVICE_TOKEN)
      Clerk.clearSessionAndUserState()
    }

    return if (serverError != null) {
      ClerkResult.unknownFailure(Exception(serverError.message ?: "Unknown error"))
    } else {
      ClerkResult.success(Unit)
    }
  }
}
