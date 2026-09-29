package com.clerk.api.externalaccount

import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import androidx.core.net.toUri
import com.clerk.api.Clerk
import com.clerk.api.externalaccount.ExternalAccountService.connectExternalAccount
import com.clerk.api.hostedauth.HOSTED_AUTH_CANCELLED_BY_NEW_FLOW
import com.clerk.api.hostedauth.HostedAuthService
import com.clerk.api.log.ClerkLog
import com.clerk.api.network.ClerkApi
import com.clerk.api.network.model.client.Client
import com.clerk.api.network.model.error.ClerkErrorResponse
import com.clerk.api.network.model.verification.Verification
import com.clerk.api.network.serialization.ClerkResult
import com.clerk.api.network.serialization.errorMessage
import com.clerk.api.network.serialization.onSuccess
import com.clerk.api.sso.SSOManagerActivity
import com.clerk.api.user.User
import com.clerk.api.user.toMap
import kotlinx.coroutines.CompletableDeferred

internal object ExternalAccountService {
  private var currentPendingExternalAccountConnection:
    CompletableDeferred<ClerkResult<ExternalAccount, ClerkErrorResponse>>? =
    null

  private var currentPendingExternalAccountConnectionId: String? = null

  suspend fun connectExternalAccount(
    params: User.CreateExternalAccountParams
  ): ClerkResult<ExternalAccount, ClerkErrorResponse> {
    HostedAuthService.cancelPendingAuthentication(HOSTED_AUTH_CANCELLED_BY_NEW_FLOW)
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
        val context =
          Clerk.applicationContext?.get()
            ?: return ClerkResult.unknownFailure(
              IllegalStateException(
                "Clerk must be initialized before connecting an external account"
              )
            )
        val completableDeferred =
          CompletableDeferred<ClerkResult<ExternalAccount, ClerkErrorResponse>>()
        currentPendingExternalAccountConnection = completableDeferred
        currentPendingExternalAccountConnectionId = initialResult.value.id
        val intent =
          SSOManagerActivity.createAuthorizationIntent(context, externalUrl.toUri()).apply {
            addFlags(FLAG_ACTIVITY_NEW_TASK)
          }
        context.startActivity(intent)
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

      Client.Companion.get().onSuccess { client ->
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
            pendingConnection.complete(ClerkResult.Companion.success(externalAccount))
          }
        }
      }
    } catch (e: Exception) {
      ClerkLog.e("Failed to complete external connection: ${e.message}")
      currentPendingExternalAccountConnection?.complete(ClerkResult.Companion.unknownFailure(e))
    } finally {
      clearExternalConnectionState()
    }
  }

  fun hasPendingExternalAccountConnection(): Boolean {
    return currentPendingExternalAccountConnectionId != null
  }

  fun cancelPendingExternalAccountConnection() {
    currentPendingExternalAccountConnection?.complete(
      ClerkResult.Companion.unknownFailure(Exception("External account connection cancelled"))
    )
    clearExternalConnectionState()
  }

  private fun completeWithError(
    pendingConnection: CompletableDeferred<ClerkResult<ExternalAccount, ClerkErrorResponse>>,
    message: String,
  ) {
    pendingConnection.complete(ClerkResult.Companion.unknownFailure(Exception(message)))
  }

  private fun clearExternalConnectionState() {
    currentPendingExternalAccountConnection = null
    currentPendingExternalAccountConnectionId = null
  }

  private fun mapErrorToSpecificType(
    initialResult: ClerkResult.Failure<ClerkErrorResponse>
  ): ClerkResult.Failure<ClerkErrorResponse> =
    when (initialResult.errorType) {
      ClerkResult.Failure.ErrorType.API -> ClerkResult.Companion.apiFailure(initialResult.error)
      ClerkResult.Failure.ErrorType.HTTP ->
        ClerkResult.Companion.httpFailure(
          code = initialResult.code ?: -1,
          error = initialResult.error,
        )

      ClerkResult.Failure.ErrorType.UNKNOWN ->
        ClerkResult.Companion.unknownFailure(Exception("${initialResult.errorMessage}"))
    }
}
