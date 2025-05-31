package com.clerk.sdk.service

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.clerk.sdk.Clerk
import com.clerk.sdk.model.error.ClerkErrorResponse
import com.clerk.sdk.model.response.ClientPiggybackedResponse
import com.clerk.sdk.model.signin.SignIn
import com.clerk.sdk.model.signin.get
import com.clerk.sdk.network.ClerkApi
import com.clerk.sdk.network.serialization.ClerkApiResult
import com.clerk.sdk.sso.SSOReceiverActivity
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CompletableDeferred

internal object SSOService {
  private val pendingResults =
    ConcurrentHashMap<
      String,
      CompletableDeferred<ClerkApiResult<ClientPiggybackedResponse<SignIn>, ClerkErrorResponse>>,
    >()

  /**
   * Handles Single Sign-On (SSO) and OAuth authentication flows for the Clerk SDK.
   *
   * This service manages redirect-based authentication flows, including initiating OAuth,
   * Enterprise SSO, and handling callback URIs to complete the authentication process. It provides
   * methods to start, complete, or cancel authentication flows that require user redirection to
   * external providers (e.g., Google, Facebook).
   *
   * For redirect-based flows, this service uses [SSOReceiverActivity] to intercept the redirect URI
   * and finalize the sign-in process.
   */
  suspend fun authenticateWithRedirect(
    context: Context,
    params: SignIn.AuthenticateWithRedirectParams,
  ): ClerkApiResult<ClientPiggybackedResponse<SignIn>, ClerkErrorResponse> {
    val initialResult =
      ClerkApi.instance.authenticateWithRedirect(
        strategy = params.strategy,
        redirectUrl = params.redirectUrl,
      )

    return when (initialResult) {
      is ClerkApiResult.Failure -> {
        ClerkApiResult.apiFailure(initialResult.error)
      }
      is ClerkApiResult.Success -> {
        val externalUrl =
          requireNotNull(
            initialResult.value.response.firstFactorVerification?.externalVerificationRedirectUrl
          ) {
            "External URL cannot be null"
          }

        val signInId = initialResult.value.response.id
        val completableDeferred =
          CompletableDeferred<
            ClerkApiResult<ClientPiggybackedResponse<SignIn>, ClerkErrorResponse>
          >()
        pendingResults[signInId] = completableDeferred

        val intent =
          Intent(context, SSOReceiverActivity::class.java).apply { data = Uri.parse(externalUrl) }
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
   * This method is typically triggered internally via [SSOReceiverActivity] when the app receives a
   * redirect URI containing authentication results. It processes the redirect URI to retrieve the
   * sign-in result, resolves the corresponding pending [CompletableDeferred], and updates the
   * sign-in state.
   *
   * @param uri The redirect URI received after completion of the external authentication flow.
   *   Expected to contain a `rotating_token_nonce` query parameter to associate with the pending
   *   sign-in request.
   */
  suspend fun completeAuthenticateWithRedirect(uri: Uri) {
    try {
      val nonce = uri.getQueryParameter("rotating_token_nonce")

      // It's a sign up, so call for a transfer
      if (nonce == null) {
        //        SignIn.create()
      }

      val signInResult = requireNotNull(Clerk.signIn).get(rotatingTokenNonce = nonce)

      when (signInResult) {
        is ClerkApiResult.Success -> {
          val signIn = signInResult.value.response
          val deferred = pendingResults.remove(signIn.id)
          deferred?.complete(ClerkApiResult.success(signInResult.value))
        }
        is ClerkApiResult.Failure -> {
          // If we can't determine which sign-in failed, complete all pending ones
          val deferredResults = pendingResults.values.toList()
          pendingResults.clear()
          deferredResults.forEach { it.complete(ClerkApiResult.apiFailure((signInResult.error))) }
        }
      }
    } catch (e: IllegalArgumentException) {
      val deferreds = pendingResults.values.toList()
      pendingResults.clear()
      deferreds.forEach { it.complete(ClerkApiResult.unknownFailure(e)) }
    }
  }

  fun cancelAuthenticateWithRedirect(signInId: String? = null) {
    if (signInId != null) {
      pendingResults
        .remove(signInId)
        ?.complete(ClerkApiResult.unknownFailure(error("Authentication canceled")))
    } else {
      val deferreds = pendingResults.values.toList()
      pendingResults.clear()
      deferreds.forEach {
        it.complete(ClerkApiResult.unknownFailure(error("Authentication canceled")))
      }
    }
  }
}
