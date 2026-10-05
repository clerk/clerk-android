package com.clerk.api.hostedauth

import android.content.Context
import android.net.Uri
import androidx.core.net.toUri
import com.clerk.api.Clerk
import com.clerk.api.auth.HostedAuthMode
import com.clerk.api.network.ClerkApi
import com.clerk.api.network.middleware.ManualClientSyncRequest
import com.clerk.api.network.middleware.ResponseGuard
import com.clerk.api.network.middleware.outgoing.INTERNAL_HEADER_TRUE
import com.clerk.api.network.model.client.Client
import com.clerk.api.network.model.error.ClerkErrorResponse
import com.clerk.api.network.serialization.ClerkResult
import com.clerk.api.redirect.PendingRedirect
import com.clerk.api.redirect.RedirectCoordinator
import com.clerk.api.session.Session
import java.security.SecureRandom

/**
 * Hosted (Account Portal) sign-in. The pending flow is a [PendingRedirect.HostedAuth] held by
 * [RedirectCoordinator]; its callback must carry the `state` sent when the flow was created.
 */
@Suppress("TooManyFunctions")
internal object HostedAuthService {
  suspend fun start(
    mode: HostedAuthMode?,
    redirectUrl: String,
  ): ClerkResult<Session, ClerkErrorResponse> =
    when (val preparation = prepareHostedAuth(redirectUrl)) {
      is ClerkResult.Failure -> preparation
      is ClerkResult.Success -> startPreparedHostedAuth(preparation.value, mode)
    }

  private suspend fun startPreparedHostedAuth(
    preparation: PreparedHostedAuth,
    mode: HostedAuthMode?,
  ): ClerkResult<Session, ClerkErrorResponse> {
    val pendingAuth =
      PendingRedirect.HostedAuth(
        redirectUrl = preparation.redirectUrl,
        state = preparation.state,
        codeVerifier = preparation.codeVerifier,
      )
    // Replaces any pending redirect flow, which completes with its cancellation result.
    RedirectCoordinator.begin(pendingAuth)
    val responseGuard = ResponseGuard { sideEffect ->
      RedirectCoordinator.runIfCurrent(pendingAuth, sideEffect)
    }
    try {
      return when (val result = createHostedAuth(preparation, mode, responseGuard)) {
        is ClerkResult.Failure -> {
          finishPendingAuth(pendingAuth, result)
          RedirectCoordinator.await(pendingAuth)
        }
        is ClerkResult.Success ->
          RedirectCoordinator.launchAndAwait(pendingAuth, preparation.context, result.value)
      }
    } finally {
      // The slot is taken before the creation request; a caller cancelled (or a request that
      // threw) before the browser launched must not leave it held.
      if (!pendingAuth.result.isCompleted) {
        RedirectCoordinator.cancelPending(matches = { it === pendingAuth })
      }
    }
  }

  suspend fun complete(uri: Uri): ClerkResult<Session, ClerkErrorResponse>? {
    val pendingAuth = RedirectCoordinator.current() as? PendingRedirect.HostedAuth
    if (pendingAuth == null || !uri.matchesHostedAuthRedirectUrl(pendingAuth.redirectUrl)) {
      return null
    }
    val callbackResult =
      validateHostedAuthCallback(
        uri = uri,
        redirectUrl = pendingAuth.redirectUrl,
        expectedState = pendingAuth.state,
      )
    return when (callbackResult) {
      // An invalid callback (e.g. a forged state fired by another app) must not consume the
      // single completion slot or fail the pending flow. Report the failure to this caller and
      // keep waiting so the legitimate callback can still complete the authentication.
      is ClerkResult.Failure -> callbackResult
      is ClerkResult.Success ->
        RedirectCoordinator.complete(pendingAuth) {
          redeemAndComplete(pendingAuth, callbackResult.value)
        }
    }
  }

  fun canHandle(uri: Uri): Boolean =
    (RedirectCoordinator.current() as? PendingRedirect.HostedAuth)?.let {
      uri.matchesHostedAuthRedirectUrl(it.redirectUrl)
    } == true

  /** True when the URI targets the pending flow's callback but fails validation. */
  fun isForgedCallback(uri: Uri): Boolean {
    val pendingAuth = RedirectCoordinator.current() as? PendingRedirect.HostedAuth ?: return false
    return uri.matchesHostedAuthRedirectUrl(pendingAuth.redirectUrl) &&
      validateHostedAuthCallback(
        uri = uri,
        redirectUrl = pendingAuth.redirectUrl,
        expectedState = pendingAuth.state,
      ) is
        ClerkResult.Failure
  }

  private suspend fun redeemAndComplete(
    pendingAuth: PendingRedirect.HostedAuth,
    callback: HostedAuthCallback,
  ) {
    val manualClientSyncRequest = ManualClientSyncRequest()
    val clientResult =
      ClerkApi.client.redeemHostedAuth(
        rotatingTokenNonce = callback.rotatingTokenNonce,
        codeVerifier = pendingAuth.codeVerifier,
        manualClientSyncRequest = manualClientSyncRequest,
      )
    if (clientResult is ClerkResult.Success) {
      var clientApplied = false
      manualClientSyncRequest.runIfResponseCurrent {
        RedirectCoordinator.runIfCurrent(pendingAuth) {
          Clerk.updateClient(clientResult.value)
          clientApplied = true
        }
      }
      if (!clientApplied) {
        finishPendingAuth(
          pendingAuth,
          ClerkResult.unknownFailure(
            IllegalStateException("Hosted auth redemption response is no longer current.")
          ),
        )
        return
      }
    }
    when (clientResult) {
      is ClerkResult.Failure -> finishPendingAuth(pendingAuth, clientResult)
      is ClerkResult.Success -> completeRedeemedClient(pendingAuth, callback, clientResult.value)
    }
  }

  private fun completeRedeemedClient(
    pendingAuth: PendingRedirect.HostedAuth,
    callback: HostedAuthCallback,
    client: Client,
  ) {
    // The redeemed client has already been applied locally; this only resolves the flow result.
    val createdSession = client.sessions.firstOrNull { it.id == callback.createdSessionId }
    if (createdSession == null) {
      finishPendingAuth(
        pendingAuth,
        ClerkResult.unknownFailure(
          IllegalStateException("Hosted auth completion did not include the created session.")
        ),
      )
    } else {
      finishPendingAuth(pendingAuth, ClerkResult.success(createdSession))
    }
  }

  fun cancelPendingAuthentication(reason: String = AUTHENTICATION_CANCELLED) {
    RedirectCoordinator.cancelPending(reason) { it is PendingRedirect.HostedAuth }
  }

  fun hasPendingAuthentication(): Boolean =
    RedirectCoordinator.current() is PendingRedirect.HostedAuth

  private fun finishPendingAuth(
    pendingAuth: PendingRedirect.HostedAuth,
    result: ClerkResult<Session, ClerkErrorResponse>,
  ) {
    RedirectCoordinator.finish(pendingAuth, result)
  }
}

private data class PreparedHostedAuth(
  val context: Context,
  val redirectUrl: String,
  val state: String,
  val codeVerifier: String,
  val codeChallenge: String,
)

private fun prepareHostedAuth(
  redirectUrl: String
): ClerkResult<PreparedHostedAuth, ClerkErrorResponse> {
  val context = Clerk.applicationContext?.get()
  val scheme = redirectUrl.toUri().scheme
  return when {
    context == null ->
      ClerkResult.unknownFailure(
        IllegalStateException("Clerk must be initialized before starting hosted auth.")
      )
    scheme.equals("http", ignoreCase = true) || scheme.equals("https", ignoreCase = true) ->
      ClerkResult.unknownFailure(
        IllegalArgumentException("Hosted auth requires a custom-scheme redirect URL.")
      )
    else -> {
      val random = SecureRandom()
      val pkce = generateHostedAuthPkce(random)
      ClerkResult.success(
        PreparedHostedAuth(
          context = context,
          redirectUrl = redirectUrl,
          state = generateHostedAuthState(random),
          codeVerifier = pkce.codeVerifier,
          codeChallenge = pkce.codeChallenge,
        )
      )
    }
  }
}

private suspend fun createHostedAuth(
  preparation: PreparedHostedAuth,
  mode: HostedAuthMode?,
  responseGuard: ResponseGuard,
): ClerkResult<Uri, ClerkErrorResponse> {
  var result = requestHostedAuth(preparation, mode, responseGuard, skipClientId = false)
  if (
    result is ClerkResult.Failure &&
      result.isSignedOutFailure() &&
      refreshSignedOutClient(responseGuard)
  ) {
    result = requestHostedAuth(preparation, mode, responseGuard, skipClientId = true)
  }
  return when (result) {
    is ClerkResult.Failure -> result
    is ClerkResult.Success ->
      result.value.authenticationUri()?.let { ClerkResult.success(it) }
        ?: ClerkResult.unknownFailure(
          IllegalStateException("Hosted auth creation returned an invalid response.")
        )
  }
}

private suspend fun refreshSignedOutClient(responseGuard: ResponseGuard): Boolean {
  val manualClientSyncRequest = ManualClientSyncRequest()
  val updateCountAtStart = Clerk.clientUpdateCount
  val result =
    ClerkApi.client.getSkippingClientId(manualClientSyncRequest = manualClientSyncRequest)
  if (result !is ClerkResult.Success) return false
  var flowIsCurrent = false
  responseGuard.runIfAllowed {
    flowIsCurrent = true
    manualClientSyncRequest.runIfResponseCurrent {
      Clerk.updateClientIfUnchangedSince(updateCountAtStart, result)
    }
  }
  return flowIsCurrent
}

private suspend fun requestHostedAuth(
  preparation: PreparedHostedAuth,
  mode: HostedAuthMode?,
  responseGuard: ResponseGuard,
  skipClientId: Boolean,
) =
  ClerkApi.client.createHostedAuth(
    redirectUrl = preparation.redirectUrl,
    codeChallenge = preparation.codeChallenge,
    state = preparation.state,
    mode = mode?.value,
    skipClientId = if (skipClientId) INTERNAL_HEADER_TRUE else null,
    responseGuard = responseGuard,
  )

private fun ClerkResult.Failure<ClerkErrorResponse>.isSignedOutFailure(): Boolean =
  errorType == ClerkResult.Failure.ErrorType.HTTP &&
    code == HTTP_UNAUTHORIZED &&
    error?.errors?.any { it.code == SIGNED_OUT_ERROR_CODE } == true

private const val HTTP_UNAUTHORIZED = 401
private const val SIGNED_OUT_ERROR_CODE = "signed_out"
private const val AUTHENTICATION_CANCELLED = "Authentication cancelled"

internal const val HOSTED_AUTH_CANCELLED_BY_NEW_FLOW =
  "New authentication started, cancelling previous hosted auth attempt"
