package com.clerk.api.signout

import com.clerk.api.Clerk
import com.clerk.api.log.ClerkLog
import com.clerk.api.network.ClerkApi
import com.clerk.api.network.model.client.Client
import com.clerk.api.network.model.error.ClerkErrorResponse
import com.clerk.api.network.serialization.ClerkResult
import com.clerk.api.network.serialization.errorMessage
import com.clerk.api.storage.StorageHelper
import com.clerk.api.storage.StorageKey

/**
 * Service responsible for signing out users by removing every session on the current client.
 *
 * The SignOutService handles the complete all-account sign-out process, including deleting client
 * sessions from the Clerk API and cleaning up local client state. Single-session sign-out is
 * handled by [com.clerk.api.auth.Auth.signOut] when a session ID is provided.
 */
internal object SignOutService {

  /**
   * Signs out all accounts by deleting every session on the current client.
   *
   * Local credentials are always cleared regardless of whether the server-side sign-out succeeds,
   * ensuring users are never stuck in a logged-in state after attempting to sign out.
   *
   * @return A [ClerkResult] indicating the success or failure of the sign-out operation. Returns
   *   [ClerkResult.Companion.success] with [Unit] on successful sign-out, or
   *   [ClerkResult.Companion.unknownFailure] with error details on failure. Note: local credentials
   *   are cleared in both cases.
   */
  suspend fun signOut(): ClerkResult<Unit, ClerkErrorResponse> {
    var serverError: Exception? = null

    try {
      when (val result = ClerkApi.session.deleteSessions()) {
        is ClerkResult.Success -> Clerk.updateClient(result.value)
        is ClerkResult.Failure ->
          serverError = result.throwable as? Exception ?: Exception(result.errorMessage)
      }
    } catch (e: Exception) {
      ClerkLog.w("Server sign-out failed: ${e.message}")
      serverError = e
    } finally {
      // Always clear local credentials regardless of server response
      StorageHelper.deleteValue(StorageKey.DEVICE_TOKEN)
      Clerk.updateClient(Client())

      // Best-effort refresh of the in-memory client while skipping current client id.
      // This clears stale in-progress sign-in/sign-up state that can otherwise persist after
      // sign-out when the host remounts AuthView within the same process/activity lifecycle.
      runCatching {
          when (val clientResult = Client.getSkippingClientId()) {
            is ClerkResult.Success -> Clerk.updateClient(clientResult.value)
            is ClerkResult.Failure ->
              ClerkLog.w("Client refresh after sign-out failed: ${clientResult.errorMessage}")
          }
        }
        .onFailure { ClerkLog.w("Client refresh after sign-out failed: ${it.message}") }
    }

    return if (serverError != null) {
      ClerkResult.unknownFailure(Exception(serverError.message ?: "Unknown error"))
    } else {
      ClerkResult.success(Unit)
    }
  }
}
