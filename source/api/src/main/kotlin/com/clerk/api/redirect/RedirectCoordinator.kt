package com.clerk.api.redirect

import android.content.Context
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.net.Uri
import com.clerk.api.auth.withoutAuthErrorReporting
import com.clerk.api.externalaccount.ExternalAccountService
import com.clerk.api.hostedauth.HostedAuthCallback
import com.clerk.api.hostedauth.HostedAuthService
import com.clerk.api.hostedauth.matchesHostedAuthRedirectUrl
import com.clerk.api.hostedauth.validateHostedAuthCallback
import com.clerk.api.log.ClerkLog
import com.clerk.api.log.SafeUriLog
import com.clerk.api.magiclink.NativeMagicLinkService
import com.clerk.api.magiclink.canHandleNativeMagicLink
import com.clerk.api.network.model.error.ClerkErrorResponse
import com.clerk.api.network.serialization.ClerkResult
import com.clerk.api.sso.SSOManagerActivity
import com.clerk.api.sso.SSOService
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@Suppress("TooManyFunctions")
internal object RedirectCoordinator {
  private val lock = Any()
  private var current: PendingRedirect<*>? = null

  private var recentlyCompletedStatefulFlow: PendingRedirect<*>? = null
  private var recentUntilNanos = 0L
  private var magicLinkCompletion: Pair<String, Deferred<Boolean>>? = null
  private val pending = MutableStateFlow(false)
  private val completionScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

  val hasPendingRedirect: StateFlow<Boolean> = pending.asStateFlow()

  fun current(): PendingRedirect<*>? = synchronized(lock) { current }

  fun isCurrent(redirect: PendingRedirect<*>): Boolean = synchronized(lock) { current === redirect }

  fun begin(redirect: PendingRedirect<*>) {
    val previous =
      synchronized(lock) {
        current.also {
          current = redirect
          recentlyCompletedStatefulFlow = null
          pending.value = true
        }
      }
    previous?.let { cancelDetached(it, it.supersededReason) }
  }

  fun supersedePending() {
    detach { true }?.let { cancelDetached(it, it.supersededReason) }
  }

  fun cancelPending(reason: String? = null, matches: (PendingRedirect<*>) -> Boolean = { true }) {
    detach(matches)?.let { cancelDetached(it, reason ?: it.cancelledReason) }
  }

  fun cancelPendingUnlessCompleting() {
    cancelPending { !it.completionStarted.get() }
  }

  fun cancelPendingUnlessCompleting(redirect: PendingRedirect<*>) {
    cancelPending { it === redirect && !it.completionStarted.get() }
  }

  fun runIfCurrent(redirect: PendingRedirect<*>, sideEffect: () -> Unit): Boolean =
    synchronized(lock) {
      if (current === redirect) {
        sideEffect()
        true
      } else {
        false
      }
    }

  fun <T : Any> finish(
    redirect: PendingRedirect<T>,
    result: ClerkResult<T, ClerkErrorResponse>,
  ): Boolean =
    synchronized(lock) {
      if (current !== redirect) {
        false
      } else {
        clearSlot()
        redirect.result.complete(result)
      }
    }

  suspend fun <T : Any> launchAndAwait(
    redirect: PendingRedirect<T>,
    context: Context,
    authorizationUri: Uri,
  ): ClerkResult<T, ClerkErrorResponse> {
    val intent =
      SSOManagerActivity.createAuthorizationIntent(context, authorizationUri).apply {
        addFlags(FLAG_ACTIVITY_NEW_TASK)
      }
    val launchFailure =
      try {
        runIfCurrent(redirect) { context.startActivity(intent) }
        null
      } catch (exception: RuntimeException) {
        exception
      }
    if (launchFailure != null) {
      finish(redirect, ClerkResult.unknownFailure(launchFailure))
    }
    return await(redirect)
  }

  suspend fun <T : Any> await(redirect: PendingRedirect<T>): ClerkResult<T, ClerkErrorResponse> =
    try {
      redirect.result.await()
    } finally {
      if (!redirect.result.isCompleted) {
        cancelPending(matches = { it === redirect })
      }
    }

  @Suppress("TooGenericExceptionCaught")
  suspend fun <T : Any> complete(
    redirect: PendingRedirect<T>,
    work: suspend () -> Unit,
  ): ClerkResult<T, ClerkErrorResponse> {
    if (redirect.completionStarted.compareAndSet(false, true)) {
      val job =
        completionScope.launch(start = CoroutineStart.LAZY) {
          try {
            withoutAuthErrorReporting { work() }
            finish(redirect, ClerkResult.unknownFailure(IllegalStateException(NO_RESULT)))
          } catch (cancellation: CancellationException) {
            finish(
              redirect,
              ClerkResult.unknownFailure(IllegalStateException(INTERRUPTED, cancellation)),
            )
            throw cancellation
          } catch (error: Exception) {
            ClerkLog.e("Redirect completion failed: ${error.message}")
            finish(redirect, ClerkResult.unknownFailure(error))
          }
        }
      redirect.completionJob.set(job)
      if (isCurrent(redirect)) job.start() else job.cancel()
    }
    return redirect.result.await()
  }

  suspend fun dispatch(uri: Uri): CallbackOutcome =
    when (val verdict = classify(uri)) {
      CallbackVerdict.MagicLink -> CallbackOutcome.Completed(completeMagicLink(uri))
      is CallbackVerdict.Accepted -> CallbackOutcome.Completed(completeAccepted(verdict, uri))
      is CallbackVerdict.Rejected -> {
        logDroppedCallback(uri, verdict.reason)
        CallbackOutcome.Ignored
      }
      CallbackVerdict.Unmatched ->
        if (uri.isClerkScheme()) {
          logDroppedCallback(uri, NO_MATCHING_FLOW)
          CallbackOutcome.Ignored
        } else {
          CallbackOutcome.NotHandled
        }
    }

  fun receiverDelivery(uri: Uri): ReceiverDelivery {
    val hasPending = current() != null
    return when (val verdict = classify(uri)) {
      is CallbackVerdict.Accepted -> ReceiverDelivery.FORWARD
      is CallbackVerdict.Rejected -> {
        logDroppedCallback(uri, verdict.reason)
        ReceiverDelivery.DROP
      }
      CallbackVerdict.MagicLink ->
        if (hasPending) ReceiverDelivery.COMPLETE_IN_BACKGROUND else ReceiverDelivery.FORWARD
      CallbackVerdict.Unmatched ->
        if (hasPending) {
          logDroppedCallback(uri, NO_MATCHING_FLOW)
          ReceiverDelivery.DROP
        } else {
          ReceiverDelivery.FORWARD
        }
    }
  }

  fun dispatchInBackground(uri: Uri) {
    completionScope.launch { dispatch(uri) }
  }

  fun isCallbackIntentData(uri: Uri): Boolean {
    val hostedAuth = current() as? PendingRedirect.HostedAuth
    return hostedAuth?.let { uri.matchesHostedAuthRedirectUrl(it.redirectUrl) } == true ||
      uri.isClerkScheme() ||
      canHandleNativeMagicLink(uri) ||
      RedirectState.isPresentIn(uri)
  }

  private fun classify(uri: Uri): CallbackVerdict {
    val (pendingRedirect, recentRedirect) =
      synchronized(lock) {
        current to
          recentlyCompletedStatefulFlow?.takeIf { System.nanoTime() - recentUntilNanos < 0 }
      }
    return when {
      canHandleNativeMagicLink(uri) -> CallbackVerdict.MagicLink
      pendingRedirect != null -> classifyFor(pendingRedirect, uri)
      else ->
        recentRedirect?.let { classifyFor(it, uri) }?.takeIf { it is CallbackVerdict.Accepted }
          ?: CallbackVerdict.Unmatched
    }
  }

  private fun classifyFor(redirect: PendingRedirect<*>, uri: Uri): CallbackVerdict =
    when (redirect) {
      is PendingRedirect.HostedAuth -> classifyHostedAuth(redirect, uri)
      is PendingRedirect.Sso,
      is PendingRedirect.ExternalAccountConnection ->
        when {
          !uri.isClerkScheme() && !RedirectState.isPresentIn(uri) -> CallbackVerdict.Unmatched
          redirect.expectedState == null || RedirectState.of(uri) == redirect.expectedState ->
            CallbackVerdict.Accepted(redirect)
          RedirectState.isPresentIn(uri) -> CallbackVerdict.Rejected(STATE_MISMATCH)
          else -> CallbackVerdict.Rejected(STATE_MISSING)
        }
    }

  private fun classifyHostedAuth(redirect: PendingRedirect.HostedAuth, uri: Uri): CallbackVerdict {
    if (!uri.matchesHostedAuthRedirectUrl(redirect.redirectUrl)) return CallbackVerdict.Unmatched
    val callback = validateHostedAuthCallback(uri, redirect.redirectUrl, redirect.state)
    return when (callback) {
      is ClerkResult.Failure ->
        CallbackVerdict.Rejected(callback.throwable?.message ?: HOSTED_AUTH_INVALID)
      is ClerkResult.Success -> CallbackVerdict.Accepted(redirect, callback.value)
    }
  }

  private suspend fun completeAccepted(verdict: CallbackVerdict.Accepted, uri: Uri): Boolean {
    val accepted = verdict.redirect
    if (!isCurrent(accepted)) return accepted.result.await() is ClerkResult.Success
    val result =
      when (val redirect = accepted) {
        is PendingRedirect.HostedAuth ->
          HostedAuthService.complete(redirect, requireNotNull(verdict.hostedAuthCallback))
        is PendingRedirect.Sso -> complete(redirect) { SSOService.completeRedirect(redirect, uri) }
        is PendingRedirect.ExternalAccountConnection ->
          complete(redirect) { ExternalAccountService.completeConnection(redirect) }
      }
    return result is ClerkResult.Success
  }

  private suspend fun completeMagicLink(uri: Uri): Boolean {
    val key = uri.toString()
    val completion =
      synchronized(lock) {
        magicLinkCompletion?.takeIf { it.first == key }?.second
          ?: completionScope
            .async { redeemMagicLink(uri) }
            .also { deferred ->
              magicLinkCompletion = key to deferred
              deferred.invokeOnCompletion {
                synchronized(lock) {
                  if (magicLinkCompletion?.second === deferred) magicLinkCompletion = null
                }
              }
            }
      }
    return completion.await()
  }

  @Suppress("TooGenericExceptionCaught")
  private suspend fun redeemMagicLink(uri: Uri): Boolean =
    try {
      NativeMagicLinkService.handleMagicLinkDeepLink(uri) is ClerkResult.Success
    } catch (cancellation: CancellationException) {
      throw cancellation
    } catch (error: Exception) {
      ClerkLog.w("Background magic link completion failed: ${error.message}")
      false
    }

  private fun logDroppedCallback(uri: Uri, reason: String) {
    ClerkLog.w("Dropped redirect callback ($reason): ${SafeUriLog.describe(uri)}")
  }

  private fun detach(matches: (PendingRedirect<*>) -> Boolean): PendingRedirect<*>? =
    synchronized(lock) {
      current?.takeIf(matches)?.also { clearSlot() }
    }

  private fun clearSlot() {
    recentlyCompletedStatefulFlow = current?.takeIf {
      it.completionStarted.get() && it.expectedState != null
    }
    recentUntilNanos = System.nanoTime() + RECENT_WINDOW_NANOS
    current = null
    pending.value = false
  }

  private fun cancelDetached(redirect: PendingRedirect<*>, reason: String) {
    redirect.completionJob.get()?.cancel()
    redirect.completeWithCancellation(reason)
  }

  private fun Uri.isClerkScheme(): Boolean = scheme?.startsWith("clerk") == true

  internal fun resetForTests() {
    cancelPending()
    synchronized(lock) {
      magicLinkCompletion = null
      recentlyCompletedStatefulFlow = null
    }
    RedirectState.resetForTests()
  }

  private val RECENT_WINDOW_NANOS = TimeUnit.SECONDS.toNanos(60)
  private const val NO_MATCHING_FLOW = "no pending redirect flow matches it"
  private const val STATE_MISSING = "missing ${RedirectState.QUERY_PARAMETER}"
  private const val STATE_MISMATCH =
    "${RedirectState.QUERY_PARAMETER} does not match the pending flow"
  private const val HOSTED_AUTH_INVALID = "hosted auth callback failed validation"
  private const val NO_RESULT = "Redirect completion ended without a result."
  private const val INTERRUPTED =
    "Authentication was interrupted before it could complete. Please try again."
}

internal enum class ReceiverDelivery {
  FORWARD,
  COMPLETE_IN_BACKGROUND,
  DROP,
}

internal sealed interface CallbackOutcome {
  data object NotHandled : CallbackOutcome

  data object Ignored : CallbackOutcome

  data class Completed(val success: Boolean) : CallbackOutcome
}

private sealed interface CallbackVerdict {
  data object MagicLink : CallbackVerdict

  data class Accepted(
    val redirect: PendingRedirect<*>,
    val hostedAuthCallback: HostedAuthCallback? = null,
  ) : CallbackVerdict

  data class Rejected(val reason: String) : CallbackVerdict

  data object Unmatched : CallbackVerdict
}
