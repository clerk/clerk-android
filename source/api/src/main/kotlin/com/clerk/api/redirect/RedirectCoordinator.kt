package com.clerk.api.redirect

import android.content.Context
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.net.Uri
import com.clerk.api.externalaccount.ExternalAccountService
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

/**
 * Owns the single pending browser redirect (SSO, external account, hosted auth) and routes every
 * callback, from [SSOManagerActivity] or from `Clerk.auth.handle`, through [dispatch].
 *
 * - One slot behind one lock. Starting a flow completes the previous one with its cancellation
 *   result.
 * - A callback must pass the pending flow's state check before it can touch the flow.
 * - Completion runs once per flow in a process-wide scope, so an Activity being recreated never
 *   interrupts it; a duplicate callback joins the running completion.
 * - Every exit (callback, cancellation, newer flow, caller cancelled) completes the caller's result
 *   and frees the slot.
 *
 * Native magic links are routed here too, but their pending flow is not held in the slot: it is
 * persisted by [NativeMagicLinkService] because the email link may be opened after process death,
 * and it is checked by its `flow_id` and PKCE verifier rather than a redirect state.
 */
@Suppress("TooManyFunctions")
internal object RedirectCoordinator {
  private val lock = Any()
  private var current: PendingRedirect<*>? = null

  /** The last flow a callback completed, so a repeat of that callback joins its result. */
  private var recent: PendingRedirect<*>? = null
  private var magicLinkCompletion: Pair<String, Deferred<Boolean>>? = null
  private val pending = MutableStateFlow(false)
  private val completionScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

  /** Whether a redirect flow is pending. Emits `false` as soon as the slot is freed. */
  val hasPendingRedirect: StateFlow<Boolean> = pending.asStateFlow()

  fun current(): PendingRedirect<*>? = synchronized(lock) { current }

  fun isCurrent(redirect: PendingRedirect<*>): Boolean = synchronized(lock) { current === redirect }

  /** Makes [redirect] the pending flow. The flow it replaces completes with its cancellation. */
  fun begin(redirect: PendingRedirect<*>) {
    val previous =
      synchronized(lock) {
        current.also {
          current = redirect
          recent = null
          pending.value = true
        }
      }
    previous?.let { cancelDetached(it, it.supersededReason) }
  }

  /** Cancels the pending flow, if any, because a new flow is about to start. */
  fun supersedePending() {
    detach { true }?.let { cancelDetached(it, it.supersededReason) }
  }

  /** Cancels the pending flow when [matches] accepts it. */
  fun cancelPending(reason: String? = null, matches: (PendingRedirect<*>) -> Boolean = { true }) {
    detach(matches)?.let { cancelDetached(it, reason ?: it.cancelledReason) }
  }

  /**
   * Cancels the pending flow unless a callback is already completing it. Used when the user comes
   * back from the browser without a callback.
   */
  fun cancelPendingUnlessCompleting() {
    cancelPending { !it.completionStarted.get() }
  }

  /** Runs [sideEffect] under the slot lock only while [redirect] is the pending flow. */
  fun runIfCurrent(redirect: PendingRedirect<*>, sideEffect: () -> Unit): Boolean =
    synchronized(lock) {
      if (current === redirect) {
        sideEffect()
        true
      } else {
        false
      }
    }

  /**
   * Completes [redirect] with [result] and frees the slot. Returns `false`, changing nothing, when
   * [redirect] is no longer the pending flow (it was cancelled or replaced, and already has its
   * result).
   */
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

  /**
   * Opens [authorizationUri] in the browser for [redirect] and waits for its result. When the
   * caller is cancelled the flow is abandoned and the slot freed.
   */
  suspend fun <T : Any> launchAndAwait(
    redirect: PendingRedirect<T>,
    context: Context,
    authorizationUri: Uri,
  ): ClerkResult<T, ClerkErrorResponse> {
    val intent =
      SSOManagerActivity.createAuthorizationIntent(context, authorizationUri).apply {
        addFlags(FLAG_ACTIVITY_NEW_TASK)
      }
    // The launch holds the slot lock so a concurrent cancellation cannot slip in between the
    // ownership check and the activity start; when the flow is no longer current nothing launches
    // and its result already carries the cancellation.
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

  /** Waits for [redirect]'s result, abandoning the flow if the caller is cancelled first. */
  suspend fun <T : Any> await(redirect: PendingRedirect<T>): ClerkResult<T, ClerkErrorResponse> =
    try {
      redirect.result.await()
    } finally {
      if (!redirect.result.isCompleted) {
        cancelPending(matches = { it === redirect })
      }
    }

  /**
   * Runs [work] for [redirect] once, in the process-wide scope, and waits for the result. A second
   * call (a duplicate callback, or the Activity re-attaching after recreation) joins the first.
   * [work] reports its result with [finish]; if it ends without doing so the flow still completes.
   */
  @Suppress("TooGenericExceptionCaught")
  suspend fun <T : Any> complete(
    redirect: PendingRedirect<T>,
    work: suspend () -> Unit,
  ): ClerkResult<T, ClerkErrorResponse> {
    if (redirect.completionStarted.compareAndSet(false, true)) {
      val job =
        completionScope.launch(start = CoroutineStart.LAZY) {
          try {
            work()
            finish(redirect, ClerkResult.unknownFailure(IllegalStateException(NO_RESULT)))
          } catch (cancellation: CancellationException) {
            // Cancelled by the coordinator: the flow already has its result. Thrown from inside the
            // work instead: report an interruption rather than leave the caller waiting.
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

  /** Routes a callback URI to the flow it belongs to. */
  suspend fun dispatch(uri: Uri): CallbackOutcome =
    when (val verdict = classify(uri)) {
      CallbackVerdict.MagicLink -> CallbackOutcome.Completed(completeMagicLink(uri))
      is CallbackVerdict.Accepted -> CallbackOutcome.Completed(completeAccepted(verdict, uri))
      CallbackVerdict.Rejected -> {
        ClerkLog.w("Ignoring redirect callback with invalid state: ${SafeUriLog.describe(uri)}")
        CallbackOutcome.Ignored
      }
      CallbackVerdict.Unmatched ->
        if (uri.isClerkScheme()) {
          ClerkLog.w("No pending redirect flow for callback: ${SafeUriLog.describe(uri)}")
          CallbackOutcome.Ignored
        } else {
          CallbackOutcome.NotHandled
        }
    }

  /** True when [uri] targets the pending flow but fails its state check. */
  fun isRejected(uri: Uri): Boolean = classify(uri) == CallbackVerdict.Rejected

  /** Whether [uri] has the shape of a redirect callback of any flow. */
  fun looksLikeCallback(uri: Uri): Boolean {
    val hostedAuth = current() as? PendingRedirect.HostedAuth
    return hostedAuth?.let { uri.matchesHostedAuthRedirectUrl(it.redirectUrl) } == true ||
      uri.isClerkScheme() ||
      canHandleNativeMagicLink(uri) ||
      CALLBACK_PARAMETERS.any { runCatching { uri.getQueryParameter(it) }.getOrNull() != null }
  }

  private fun classify(uri: Uri): CallbackVerdict {
    val (pendingRedirect, recentRedirect) = synchronized(lock) { current to recent }
    return when {
      canHandleNativeMagicLink(uri) -> CallbackVerdict.MagicLink
      pendingRedirect != null -> classifyFor(pendingRedirect, uri)
      // A repeat of the callback that completed the last flow (a duplicate delivery, or the
      // activity re-attaching after recreation once the work already finished) joins its result.
      else ->
        recentRedirect?.let { classifyFor(it, uri) }?.takeIf { it is CallbackVerdict.Accepted }
          ?: CallbackVerdict.Unmatched
    }
  }

  private fun classifyFor(redirect: PendingRedirect<*>, uri: Uri): CallbackVerdict =
    when (redirect) {
      is PendingRedirect.HostedAuth ->
        when {
          !uri.matchesHostedAuthRedirectUrl(redirect.redirectUrl) -> CallbackVerdict.Unmatched
          validateHostedAuthCallback(uri, redirect.redirectUrl, redirect.state) is
            ClerkResult.Failure -> CallbackVerdict.Rejected
          else -> CallbackVerdict.Accepted(redirect)
        }
      is PendingRedirect.Sso,
      is PendingRedirect.ExternalAccountConnection ->
        when {
          !looksLikeCallback(uri) -> CallbackVerdict.Unmatched
          redirect.expectedState == null || RedirectState.of(uri) == redirect.expectedState ->
            CallbackVerdict.Accepted(redirect)
          else -> CallbackVerdict.Rejected
        }
    }

  private suspend fun completeAccepted(verdict: CallbackVerdict.Accepted, uri: Uri): Boolean {
    val accepted = verdict.redirect
    if (!isCurrent(accepted)) return accepted.result.await() is ClerkResult.Success
    val result =
      when (val redirect = accepted) {
        is PendingRedirect.HostedAuth -> HostedAuthService.complete(uri)
        is PendingRedirect.Sso -> complete(redirect) { SSOService.completeRedirect(redirect, uri) }
        is PendingRedirect.ExternalAccountConnection ->
          complete(redirect) { ExternalAccountService.completeConnection(redirect) }
      }
    return result is ClerkResult.Success
  }

  /**
   * Completes a native magic link in the process-wide scope. A second delivery of the same link
   * while the first is running joins it instead of redeeming the approval token twice.
   */
  private suspend fun completeMagicLink(uri: Uri): Boolean {
    val key = uri.toString()
    val completion =
      synchronized(lock) {
        magicLinkCompletion?.takeIf { it.first == key }?.second
          ?: completionScope
            .async {
              runCatching { NativeMagicLinkService.handleMagicLinkDeepLink(uri) }.getOrNull() is
                ClerkResult.Success
            }
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

  private fun detach(matches: (PendingRedirect<*>) -> Boolean): PendingRedirect<*>? =
    synchronized(lock) {
      current?.takeIf(matches)?.also { clearSlot() }
    }

  private fun clearSlot() {
    recent = current?.takeIf { it.completionStarted.get() }
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
      recent = null
    }
    RedirectState.resetForTests()
  }

  private val CALLBACK_PARAMETERS =
    listOf(
      RedirectState.QUERY_PARAMETER,
      "rotating_token_nonce",
      "__clerk_status",
      "__clerk_error_code",
      "error",
      "error_description",
    )

  private const val NO_RESULT = "Redirect completion ended without a result."
  private const val INTERRUPTED =
    "Authentication was interrupted before it could complete. Please try again."
}

internal sealed interface CallbackOutcome {
  /** Not a Clerk callback; the app should handle it. */
  data object NotHandled : CallbackOutcome

  /** A Clerk callback with no flow to complete, or one that failed the flow's state check. */
  data object Ignored : CallbackOutcome

  /** The callback completed (or joined the completion of) a flow. */
  data class Completed(val success: Boolean) : CallbackOutcome
}

private sealed interface CallbackVerdict {
  data object MagicLink : CallbackVerdict

  data class Accepted(val redirect: PendingRedirect<*>) : CallbackVerdict

  data object Rejected : CallbackVerdict

  data object Unmatched : CallbackVerdict
}
