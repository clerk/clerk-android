package com.clerk.api.hostedauth

import android.content.Context
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.net.Uri
import com.clerk.api.Clerk
import com.clerk.api.auth.HostedAuthMode
import com.clerk.api.externalaccount.ExternalAccountService
import com.clerk.api.network.ClerkApi
import com.clerk.api.network.middleware.ResponseGuard
import com.clerk.api.network.model.client.Client
import com.clerk.api.network.model.error.ClerkErrorResponse
import com.clerk.api.network.serialization.ClerkResult
import com.clerk.api.session.Session
import com.clerk.api.sso.SSOManagerActivity
import com.clerk.api.sso.SSOService
import java.security.SecureRandom
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
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
      SSOManagerActivity.createAuthorizationIntent(
          context,
          hostedAuthUri,
        )
        .apply { addFlags(FLAG_ACTIVITY_NEW_TASK) }
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

  @Suppress("TooGenericExceptionCaught")
  suspend fun complete(uri: Uri): ClerkResult<Session, ClerkErrorResponse>? {
    val pendingAuth = pendingAuthStore.current()
    return when {
      pendingAuth == null -> null
      !uri.matchesHostedAuthRedirectUrl(pendingAuth.redirectUrl) -> null
      pendingAuth.completionStarted.compareAndSet(false, true) -> {
        val completionJob =
          completionScope.launch(start = CoroutineStart.LAZY) {
            try {
              completePendingAuth(uri, pendingAuth)
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
        pendingAuth.deferred.await()
      }
      else -> pendingAuth.deferred.await()
    }
  }

  fun canHandle(uri: Uri): Boolean =
    pendingAuthStore.current()?.let { uri.matchesHostedAuthRedirectUrl(it.redirectUrl) } == true

  private suspend fun completePendingAuth(
    uri: Uri,
    pendingAuth: PendingHostedAuth,
  ): ClerkResult<Session, ClerkErrorResponse> =
    when (
      val callbackResult =
        validateHostedAuthCallback(
          uri = uri,
          redirectUrl = pendingAuth.redirectUrl,
          expectedState = pendingAuth.state,
        )
    ) {
      is ClerkResult.Failure -> finishPendingAuth(pendingAuth, callbackResult)
      is ClerkResult.Success -> redeemAndComplete(pendingAuth, callbackResult.value)
    }

  private suspend fun redeemAndComplete(
    pendingAuth: PendingHostedAuth,
    callback: HostedAuthCallback,
  ): ClerkResult<Session, ClerkErrorResponse> {
    val clientResult =
      ClerkApi.client.redeemHostedAuth(
        rotatingTokenNonce = callback.rotatingTokenNonce,
        codeVerifier = pendingAuth.codeVerifier,
      )
    // A successful redemption consumed the single-use nonce and rotated this client's token on
    // the server, so the redeemed client is authoritative local state regardless of how the rest
    // of the flow ends (cancellation, activation failure, etc.).
    if (clientResult is ClerkResult.Success) {
      Clerk.updateClient(clientResult.value)
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
      synchronized(lock) {
        currentPendingAuth.also { currentPendingAuth = null }
      } ?: return
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
        PreparedHostedAuth(
          context = context,
          redirectUrl = redirectUrl,
          request = it,
        )
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
  val result =
    ClerkApi.client.createHostedAuth(
      redirectUrl = preparation.redirectUrl,
      codeChallenge = preparation.request.pkce.codeChallenge,
      state = preparation.request.state,
      mode = mode?.value,
      responseGuard = responseGuard,
    )
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

private data class HostedAuthRequest(val state: String, val pkce: HostedAuthPkce)

private const val AUTHENTICATION_CANCELLED = "Authentication cancelled"
