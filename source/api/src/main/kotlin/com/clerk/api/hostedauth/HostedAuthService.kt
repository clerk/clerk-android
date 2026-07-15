package com.clerk.api.hostedauth

import android.content.Context
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.net.Uri
import com.clerk.api.Clerk
import com.clerk.api.auth.HostedAuthMode
import com.clerk.api.externalaccount.ExternalAccountService
import com.clerk.api.network.ClerkApi
import com.clerk.api.network.middleware.ManualClientSyncRequest
import com.clerk.api.network.middleware.ResponseGuard
import com.clerk.api.network.middleware.outgoing.INTERNAL_HEADER_TRUE
import com.clerk.api.network.model.client.Client
import com.clerk.api.network.model.error.ClerkErrorResponse
import com.clerk.api.network.serialization.ClerkResult
import com.clerk.api.session.Session
import com.clerk.api.sso.SSOManagerActivity
import com.clerk.api.sso.SSOService
import java.security.SecureRandom
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@Suppress("TooManyFunctions")
internal object HostedAuthService {
  private val pendingAuthStore = HostedAuthPendingStore()
  private val completionScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

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
      PendingHostedAuth(
        redirectUrl = preparation.redirectUrl,
        state = preparation.request.state,
        codeVerifier = preparation.request.pkce.codeVerifier,
        deferred = CompletableDeferred(),
      )
    if (!pendingAuthStore.add(pendingAuth)) {
      return ClerkResult.unknownFailure(
        IllegalStateException("A hosted authentication session is already in progress.")
      )
    }

    SSOService.cancelPendingAuthentication()
    ExternalAccountService.cancelPendingExternalAccountConnection()
    return try {
      val responseGuard = ResponseGuard { sideEffect ->
        pendingAuthStore.runIfCurrent(pendingAuth, sideEffect)
      }
      when (val result = createHostedAuth(preparation, mode, responseGuard)) {
        is ClerkResult.Failure -> finishPendingAuth(pendingAuth, result)
        is ClerkResult.Success ->
          if (pendingAuthStore.isCurrent(pendingAuth)) {
            launchAndAwait(preparation.context, result.value, pendingAuth)
          } else {
            pendingAuth.deferred.await()
          }
      }
    } finally {
      val removed = pendingAuthStore.remove(pendingAuth)
      if (!pendingAuth.deferred.isCompleted && removed) {
        pendingAuth.deferred.cancel()
      }
    }
  }

  private suspend fun launchAndAwait(
    context: Context,
    hostedAuthUri: Uri,
    pendingAuth: PendingHostedAuth,
  ): ClerkResult<Session, ClerkErrorResponse> {
    val intent =
      SSOManagerActivity.createAuthorizationIntent(context, hostedAuthUri).apply {
        addFlags(FLAG_ACTIVITY_NEW_TASK)
      }
    val launchFailure =
      try {
        context.startActivity(intent)
        null
      } catch (exception: RuntimeException) {
        exception
      }
    return if (launchFailure == null) {
      pendingAuth.deferred.await()
    } else {
      finishPendingAuth(pendingAuth, ClerkResult.unknownFailure(launchFailure))
    }
  }

  suspend fun complete(uri: Uri): ClerkResult<Session, ClerkErrorResponse>? {
    val pendingAuth = pendingAuthStore.current()
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
        if (pendingAuth.completionStarted.compareAndSet(false, true)) {
          startClaimedCompletion(pendingAuth, callbackResult.value)
        } else {
          pendingAuth.deferred.await()
        }
    }
  }

  @Suppress("TooGenericExceptionCaught")
  private suspend fun startClaimedCompletion(
    pendingAuth: PendingHostedAuth,
    callback: HostedAuthCallback,
  ): ClerkResult<Session, ClerkErrorResponse> {
    val completionJob =
      completionScope.launch(start = CoroutineStart.LAZY) {
        try {
          redeemAndComplete(pendingAuth, callback)
        } catch (cancellation: CancellationException) {
          throw cancellation
        } catch (error: Exception) {
          finishPendingAuth(pendingAuth, ClerkResult.unknownFailure(error))
        }
      }
    pendingAuth.completionJob.set(completionJob)
    if (pendingAuthStore.isCurrent(pendingAuth)) {
      completionJob.start()
    } else {
      completionJob.cancel()
    }
    return pendingAuth.deferred.await()
  }

  fun canHandle(uri: Uri): Boolean =
    pendingAuthStore.current()?.let { uri.matchesHostedAuthRedirectUrl(it.redirectUrl) } == true

  fun isValidCallback(uri: Uri): Boolean {
    val pendingAuth = pendingAuthStore.current() ?: return false
    return validateHostedAuthCallback(
      uri = uri,
      redirectUrl = pendingAuth.redirectUrl,
      expectedState = pendingAuth.state,
    ) is
      ClerkResult.Success
  }

  private suspend fun redeemAndComplete(
    pendingAuth: PendingHostedAuth,
    callback: HostedAuthCallback,
  ): ClerkResult<Session, ClerkErrorResponse> {
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
        pendingAuthStore.runIfCurrent(pendingAuth) {
          Clerk.updateClient(clientResult.value)
          clientApplied = true
        }
      }
      if (!clientApplied) {
        return finishPendingAuth(
          pendingAuth,
          ClerkResult.unknownFailure(
            IllegalStateException("Hosted auth redemption response is no longer current.")
          ),
        )
      }
    }
    return if (pendingAuthStore.isCurrent(pendingAuth)) {
      when (clientResult) {
        is ClerkResult.Failure -> finishPendingAuth(pendingAuth, clientResult)
        is ClerkResult.Success -> completeRedeemedClient(pendingAuth, callback, clientResult.value)
      }
    } else {
      ClerkResult.unknownFailure(Exception(AUTHENTICATION_CANCELLED))
    }
  }

  private fun completeRedeemedClient(
    pendingAuth: PendingHostedAuth,
    callback: HostedAuthCallback,
    client: Client,
  ): ClerkResult<Session, ClerkErrorResponse> {
    // The redeemed client has already been applied locally; this only resolves the flow result.
    val createdSession = client.sessions.firstOrNull { it.id == callback.createdSessionId }
    return if (createdSession == null) {
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
    pendingAuthStore.cancel(reason)
  }

  fun hasPendingAuthentication(): Boolean = pendingAuthStore.hasPending()

  private fun finishPendingAuth(
    pendingAuth: PendingHostedAuth,
    result: ClerkResult<Session, ClerkErrorResponse>,
  ): ClerkResult<Session, ClerkErrorResponse> {
    if (!pendingAuthStore.completeIfCurrent(pendingAuth, result)) {
      return ClerkResult.unknownFailure(Exception(AUTHENTICATION_CANCELLED))
    }
    return result
  }
}

private data class PreparedHostedAuth(
  val context: Context,
  val redirectUrl: String,
  val request: HostedAuthRequest,
)

private data class PendingHostedAuth(
  val redirectUrl: String,
  val state: String,
  val codeVerifier: String,
  val deferred: CompletableDeferred<ClerkResult<Session, ClerkErrorResponse>>,
  val completionStarted: AtomicBoolean = AtomicBoolean(false),
  val completionJob: AtomicReference<Job?> = AtomicReference(null),
)

private class HostedAuthPendingStore {
  private val lock = Any()
  private var currentPendingAuth: PendingHostedAuth? = null

  fun add(pendingAuth: PendingHostedAuth): Boolean =
    synchronized(lock) {
      if (currentPendingAuth != null) {
        false
      } else {
        currentPendingAuth = pendingAuth
        true
      }
    }

  fun current(): PendingHostedAuth? = synchronized(lock) { currentPendingAuth }

  fun isCurrent(pendingAuth: PendingHostedAuth): Boolean =
    synchronized(lock) { currentPendingAuth === pendingAuth }

  fun runIfCurrent(pendingAuth: PendingHostedAuth, sideEffect: () -> Unit) {
    synchronized(lock) {
      if (currentPendingAuth === pendingAuth) {
        sideEffect()
      }
    }
  }

  fun completeIfCurrent(
    pendingAuth: PendingHostedAuth,
    result: ClerkResult<Session, ClerkErrorResponse>,
  ): Boolean =
    synchronized(lock) {
      if (currentPendingAuth !== pendingAuth) {
        false
      } else {
        pendingAuth.deferred.complete(result)
      }
    }

  fun remove(pendingAuth: PendingHostedAuth): Boolean =
    synchronized(lock) {
      if (currentPendingAuth !== pendingAuth) {
        false
      } else {
        currentPendingAuth = null
        true
      }
    }

  fun cancel(reason: String) {
    val pendingAuth =
      synchronized(lock) { currentPendingAuth.also { currentPendingAuth = null } } ?: return
    pendingAuth.completionJob.get()?.cancel()
    pendingAuth.deferred.complete(ClerkResult.unknownFailure(Exception(reason)))
  }

  fun hasPending(): Boolean = synchronized(lock) { currentPendingAuth != null }
}

private fun prepareHostedAuth(
  redirectUrl: String
): ClerkResult<PreparedHostedAuth, ClerkErrorResponse> {
  val context = Clerk.applicationContext?.get()
  if (context == null) {
    return ClerkResult.unknownFailure(
      IllegalStateException("Clerk must be initialized before starting hosted auth.")
    )
  }
  val request = runCatching {
    val random = SecureRandom()
    HostedAuthRequest(
      state = generateHostedAuthState(random),
      pkce = generateHostedAuthPkce(random),
    )
  }
  return request.fold(
    onSuccess = {
      ClerkResult.success(
        PreparedHostedAuth(context = context, redirectUrl = redirectUrl, request = it)
      )
    },
    onFailure = { ClerkResult.unknownFailure(it) },
  )
}

private suspend fun createHostedAuth(
  preparation: PreparedHostedAuth,
  mode: HostedAuthMode?,
  responseGuard: ResponseGuard,
): ClerkResult<Uri, ClerkErrorResponse> {
  var result = requestHostedAuth(preparation, mode, responseGuard, skipClientId = false)
  if (result is ClerkResult.Failure && result.isSignedOutFailure()) {
    // DeviceTokenSavingMiddleware has already persisted any replacement token from the 401.
    result = requestHostedAuth(preparation, mode, responseGuard, skipClientId = true)
  }
  return when (result) {
    is ClerkResult.Failure -> result
    is ClerkResult.Success -> {
      val hostedAuthUri = result.value.authenticationUri()
      if (hostedAuthUri == null) {
        ClerkResult.unknownFailure(
          IllegalStateException("Hosted auth creation returned an invalid response.")
        )
      } else {
        ClerkResult.success(hostedAuthUri)
      }
    }
  }
}

private suspend fun requestHostedAuth(
  preparation: PreparedHostedAuth,
  mode: HostedAuthMode?,
  responseGuard: ResponseGuard,
  skipClientId: Boolean,
) =
  ClerkApi.client.createHostedAuth(
    redirectUrl = preparation.redirectUrl,
    codeChallenge = preparation.request.pkce.codeChallenge,
    state = preparation.request.state,
    mode = mode?.value,
    skipClientId = if (skipClientId) INTERNAL_HEADER_TRUE else null,
    responseGuard = responseGuard,
  )

private fun ClerkResult.Failure<ClerkErrorResponse>.isSignedOutFailure(): Boolean =
  errorType == ClerkResult.Failure.ErrorType.HTTP &&
    code == HTTP_UNAUTHORIZED &&
    error?.errors?.any { it.code == SIGNED_OUT_ERROR_CODE } == true

private data class HostedAuthRequest(val state: String, val pkce: HostedAuthPkce)

private const val HTTP_UNAUTHORIZED = 401
private const val SIGNED_OUT_ERROR_CODE = "signed_out"
private const val AUTHENTICATION_CANCELLED = "Authentication cancelled"

/** Cancellation reason used when a newly started flow supersedes a pending hosted auth attempt. */
internal const val HOSTED_AUTH_CANCELLED_BY_NEW_FLOW =
  "New authentication started, cancelling previous hosted auth attempt"
