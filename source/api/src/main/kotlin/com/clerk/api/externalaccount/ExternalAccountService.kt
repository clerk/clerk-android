package com.clerk.api.externalaccount

import androidx.core.net.toUri
import com.clerk.api.Clerk
import com.clerk.api.externalaccount.ExternalAccountService.connectExternalAccount
import com.clerk.api.log.ClerkLog
import com.clerk.api.network.ClerkApi
import com.clerk.api.network.model.client.Client
import com.clerk.api.network.model.error.ClerkErrorResponse
import com.clerk.api.network.model.verification.Verification
import com.clerk.api.network.serialization.ClerkResult
import com.clerk.api.network.serialization.errorMessage
import com.clerk.api.redirect.PendingRedirect
import com.clerk.api.redirect.RedirectCoordinator
import com.clerk.api.redirect.RedirectState
import com.clerk.api.user.User
import com.clerk.api.user.toMap

internal object ExternalAccountService {
  suspend fun connectExternalAccount(
    params: User.CreateExternalAccountParams
  ): ClerkResult<ExternalAccount, ClerkErrorResponse> {
    RedirectCoordinator.supersedePending()
    val state = RedirectState.generate()
    val fields =
      params.toMap() + (REDIRECT_URL to RedirectState.withState(params.redirectUrl, state))
    val initialResult = ClerkApi.user.createExternalAccount(fields)
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
        val pendingConnection =
          PendingRedirect.ExternalAccountConnection(
            expectedState = state,
            externalAccountId = initialResult.value.id,
          )
        RedirectCoordinator.begin(pendingConnection)
        RedirectCoordinator.launchAndAwait(pendingConnection, context, externalUrl.toUri())
      }
    }
  }

  /**
   * Completes the external account connection started by [connectExternalAccount], once its
   * redirect callback has passed [RedirectCoordinator]'s state check.
   *
   * The method performs the following steps:
   * 1. Retrieves the current client state
   * 2. Locates the external account by its ID in the active session
   * 3. Verifies that the account's verification status is confirmed
   * 4. Completes the pending connection with the result
   */
  internal suspend fun completeConnection(
    pendingConnection: PendingRedirect.ExternalAccountConnection
  ) {
    ClerkLog.d("Completing external connection")
    val accountId = pendingConnection.externalAccountId
    val result: ClerkResult<ExternalAccount, ClerkErrorResponse> =
      when (val clientResult = Client.get()) {
        is ClerkResult.Failure -> {
          ClerkLog.e(
            "Failed to refresh client for external connection: ${clientResult.errorMessage}"
          )
          clientResult
        }
        is ClerkResult.Success -> {
          val client = clientResult.value
          val externalAccount =
            client.sessions
              .find { it.id == client.lastActiveSessionId }
              ?.user
              ?.externalAccounts
              ?.find { it.id == accountId }
          when {
            externalAccount == null -> failure("External account not found for ID: $accountId")
            externalAccount.verification?.status != Verification.Status.VERIFIED ->
              failure("External account verification failed: ${externalAccount.verification}")
            else -> {
              ClerkLog.d("External account verified successfully")
              ClerkResult.success(externalAccount)
            }
          }
        }
      }
    RedirectCoordinator.finish(pendingConnection, result)
  }

  private fun failure(message: String): ClerkResult<ExternalAccount, ClerkErrorResponse> =
    ClerkResult.unknownFailure(Exception(message))

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

  private const val REDIRECT_URL = "redirect_url"
}
