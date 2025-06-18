package com.clerk.sso

import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import androidx.core.net.toUri
import com.clerk.Clerk
import com.clerk.log.ClerkLog
import com.clerk.network.ClerkApi
import com.clerk.network.model.account.ExternalAccount
import com.clerk.network.model.client.Client
import com.clerk.network.model.error.ClerkErrorResponse
import com.clerk.network.model.verification.Verification
import com.clerk.network.serialization.ClerkResult
import com.clerk.network.serialization.longErrorMessageOrNull
import com.clerk.network.serialization.onSuccess
import com.clerk.user.User.CreateExternalAccountParams
import com.clerk.user.toMap
import kotlinx.coroutines.CompletableDeferred

/**
 * Internal service that handles external account connections for the Clerk SDK.
 *
 * This service manages the process of connecting external OAuth accounts (Google, Facebook, GitHub,
 * etc.) to existing user accounts. It allows already signed-in users to link their account with
 * external providers for future authentication.
 *
 * ## Key Features:
 * - External account connection for existing users
 * - OAuth provider authorization flows
 * - Account verification and status checking
 * - Automatic cleanup of pending connection state
 * - Error handling and logging throughout the flow
 *
 * The external provider must be enabled in the Clerk Dashboard settings before it can be used.
 */
internal object ExternalAccountService {
  /**
   * Tracks the current pending external account connection flow. This deferred completes when the
   * external account connection process finishes.
   */
  private var currentPendingExternalAccountConnection:
    CompletableDeferred<ClerkResult<ExternalAccount, ClerkErrorResponse>>? =
    null

  /**
   * Stores the ID of the external account being connected. Internal visibility allows
   * SSOManagerActivity (which starts in a new task) to check for its presence and determine if
   * we're in an external connection flow or a sign in/sign up flow.
   */
  private var currentPendingExternalAccountConnectionId: String? = null

  /**
   * Initiates the process of connecting an external account to an existing user.
   *
   * This method allows already signed-in users to link their account with external providers (such
   * as Google, Facebook, GitHub, etc.) for future authentication. The external provider must be
   * enabled in the Clerk Dashboard settings.
   *
   * The process involves:
   * 1. Creating an external account connection request
   * 2. Redirecting the user to the external provider for authorization
   * 3. Handling the callback to complete the connection
   * 4. Verifying the connection was successful
   *
   * @param params The parameters for creating the external account connection, including the OAuth
   *   provider and optional redirect URLs
   * @return A [ClerkResult] containing the connected [ExternalAccount] on success, or
   *   [ClerkErrorResponse] on failure
   */
  suspend fun connectExternalAccount(
    params: CreateExternalAccountParams
  ): ClerkResult<ExternalAccount, ClerkErrorResponse> {
    // Clear any existing pending external account connections
    currentPendingExternalAccountConnection = null
    val initialResult = ClerkApi.user.createExternalAccount(params.toMap())
    return when (initialResult) {
      is ClerkResult.Failure -> {
        ClerkLog.e("Failed to create external account: ${initialResult.error}")
        mapErrorToSpecificType(initialResult)
      }
      is ClerkResult.Success -> {
        ClerkLog.d("External account creation initiated: $initialResult")
        val externalUrl =
          requireNotNull(initialResult.value.verification?.externalVerificationRedirectUrl) {
            "External verification redirect URL is missing"
          }
        val completableDeferred =
          CompletableDeferred<ClerkResult<ExternalAccount, ClerkErrorResponse>>()
        currentPendingExternalAccountConnection = completableDeferred
        currentPendingExternalAccountConnectionId = initialResult.value.id
        val intent =
          Intent(Clerk.applicationContext?.get(), SSOReceiverActivity::class.java).apply {
            data = externalUrl.toUri()
            addFlags(FLAG_ACTIVITY_NEW_TASK)
          }
        Clerk.applicationContext?.get()?.startActivity(intent)
        completableDeferred.await()
      }
    }
  }

  /**
   * Completes the external account connection process initiated by [connectExternalAccount].
   *
   * This method is called when the user returns from the external provider after completing the
   * authorization flow. It verifies that the external account was successfully connected and is in
   * a verified state.
   *
   * The method performs the following steps:
   * 1. Retrieves the current client state
   * 2. Locates the external account by its ID in the active session
   * 3. Verifies that the account's verification status is confirmed
   * 4. Completes the pending connection with the result
   *
   * If any step fails, the connection is completed with an appropriate error.
   */
  suspend fun completeExternalConnection() {
    try {
      ClerkLog.d("Completing external connection")

      val pendingConnection = currentPendingExternalAccountConnection
      if (pendingConnection == null) {
        ClerkLog.e("No pending external account connection found")
        return
      }

      val accountId = currentPendingExternalAccountConnectionId
      if (accountId == null) {
        completeWithError(pendingConnection, "External account ID is null")
        return
      }

      Client.get().onSuccess { client ->
        val externalAccount =
          client.sessions
            .find { it.id == client.lastActiveSessionId }
            ?.user
            ?.externalAccounts
            ?.find { it.id == accountId }

        when {
          externalAccount == null ->
            completeWithError(pendingConnection, "External account not found for ID: $accountId")

          externalAccount.verification?.status != Verification.Status.VERIFIED ->
            completeWithError(
              pendingConnection,
              "External account verification failed: ${externalAccount.verification}",
            )

          else -> {
            ClerkLog.d("External account verified successfully")
            pendingConnection.complete(ClerkResult.success(externalAccount))
          }
        }
      }
    } catch (e: Exception) {
      ClerkLog.e("Failed to complete external connection: ${e.message}")
      currentPendingExternalAccountConnection?.complete(ClerkResult.unknownFailure(e))
    } finally {
      clearExternalConnectionState()
    }
  }

  /**
   * Checks if there's currently a pending external account connection.
   *
   * @return `true` if there's an active external account connection waiting for completion, `false`
   *   otherwise
   */
  fun hasPendingExternalAccountConnection(): Boolean {
    return currentPendingExternalAccountConnectionId != null
  }

  /**
   * Cancels any pending external account connection flow.
   *
   * This method completes any pending external account connection with a cancellation error and
   * clears the connection state. Useful for cleanup when the connection process needs to be aborted
   * (e.g., user navigates away, app is backgrounded, etc.).
   */
  fun cancelPendingExternalAccountConnection() {
    currentPendingExternalAccountConnection?.complete(
      ClerkResult.unknownFailure(Exception("External account connection cancelled"))
    )
    clearExternalConnectionState()
  }

  /**
   * Completes a pending external account connection with an error result.
   *
   * This helper method is used internally to complete failed external account connections with a
   * standardized error format.
   *
   * @param pendingConnection The deferred result to complete with an error
   * @param message The error message describing what went wrong
   */
  private fun completeWithError(
    pendingConnection: CompletableDeferred<ClerkResult<ExternalAccount, ClerkErrorResponse>>,
    message: String,
  ) {
    pendingConnection.complete(ClerkResult.unknownFailure(Exception(message)))
  }

  /**
   * Clears the internal state related to external account connections.
   *
   * This method resets both the pending connection deferred and the connection ID to prepare for
   * future external account connection attempts.
   */
  private fun clearExternalConnectionState() {
    currentPendingExternalAccountConnection = null
    currentPendingExternalAccountConnectionId = null
  }

  /**
   * Maps a generic [ClerkResult.Failure] to a more specific failure type based on the error type.
   *
   * This method ensures that error results are properly categorized as API failures, HTTP failures,
   * or unknown failures, maintaining consistency in error handling throughout the authentication
   * flow.
   *
   * @param initialResult The original failure result to be mapped
   * @return A properly typed [ClerkResult.Failure] with the appropriate error classification
   */
  private fun mapErrorToSpecificType(
    initialResult: ClerkResult.Failure<ClerkErrorResponse>
  ): ClerkResult.Failure<ClerkErrorResponse> =
    when (initialResult.errorType) {
      ClerkResult.Failure.ErrorType.API -> ClerkResult.apiFailure(initialResult.error)
      ClerkResult.Failure.ErrorType.HTTP ->
        ClerkResult.httpFailure(code = initialResult.code ?: -1, error = initialResult.error)

      ClerkResult.Failure.ErrorType.UNKNOWN ->
        ClerkResult.unknownFailure(error("${initialResult.longErrorMessageOrNull}"))
    }
}
