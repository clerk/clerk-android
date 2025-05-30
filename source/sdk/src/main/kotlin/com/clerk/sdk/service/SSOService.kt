package com.clerk.sdk.service

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.clerk.sdk.Clerk
import com.clerk.sdk.log.ClerkLog
import com.clerk.sdk.model.error.ClerkErrorResponse
import com.clerk.sdk.model.signin.SignIn
import com.clerk.sdk.model.signin.get
import com.clerk.sdk.network.ClerkApi
import com.clerk.sdk.network.serialization.ClerkApiResult
import com.clerk.sdk.sso.SSOReceiverActivity
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CompletableDeferred

internal object SSOService {
  private val pendingResults =
    ConcurrentHashMap<String, CompletableDeferred<ClerkApiResult<SignIn, ClerkErrorResponse>>>()

  /**
   * Authenticates with redirect and suspends until the OAuth flow completes. This function will
   * wait for the user to complete or cancel the OAuth flow.
   */
  suspend fun authenticateWithRedirect(
    context: Context,
    params: SignIn.AuthenticateWithRedirectParams,
  ): ClerkApiResult<SignIn, ClerkErrorResponse> {
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
        val completableDeferred = CompletableDeferred<ClerkApiResult<SignIn, ClerkErrorResponse>>()
        pendingResults[signInId] = completableDeferred

        val intent =
          Intent(context, SSOReceiverActivity::class.java).apply { data = Uri.parse(externalUrl) }
        context.startActivity(intent)

        // This will suspend until completeAuthenticateWithRedirect is called
        completableDeferred.await()
      }
    }
  }

  suspend fun completeAuthenticateWithRedirect(uri: Uri) {
    ClerkLog.e("QQQ completeAuthenticateWithRedirect $uri")
    try {
      val nonce =
        uri.getQueryParameter("rotating_token_nonce") ?: error("No nonce found in redirect URI")

      val signInResult = requireNotNull(Clerk.signIn).get(rotatingTokenNonce = nonce)

      when (signInResult) {
        is ClerkApiResult.Success -> {
          val signIn = signInResult.value.response
          val deferred = pendingResults.remove(signIn.id)
          deferred?.complete(ClerkApiResult.success(signIn))
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
