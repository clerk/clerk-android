package com.clerk.api.redirect

import com.clerk.api.hostedauth.HOSTED_AUTH_CANCELLED_BY_NEW_FLOW
import com.clerk.api.hostedauth.HostedAuthCancellationException
import com.clerk.api.network.model.error.ClerkErrorResponse
import com.clerk.api.network.serialization.ClerkResult
import com.clerk.api.session.Session
import com.clerk.api.signup.SignUp
import com.clerk.api.sso.OAuthResult
import com.clerk.api.sso.SSOCancellationException
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Job

/**
 * One browser redirect flow waiting for its callback. [RedirectCoordinator] holds at most one.
 *
 * The caller that started the flow awaits [result]. Every exit path (callback, cancellation, a
 * newer flow, the caller going away) completes it, so the caller never hangs.
 */
internal sealed class PendingRedirect<T : Any> {
  /**
   * The value the callback must carry in [RedirectState.QUERY_PARAMETER] (or `state` for hosted
   * auth). `null` only for a redirect prepared somewhere that did not put a state in its redirect
   * URL; such a flow accepts any callback, as before.
   */
  abstract val expectedState: String?

  internal val result = CompletableDeferred<ClerkResult<T, ClerkErrorResponse>>()
  internal val completionStarted = AtomicBoolean(false)
  internal val completionJob = AtomicReference<Job?>(null)

  /** Reason given to this flow's caller when a newer redirect flow replaces it. */
  internal abstract val supersededReason: String

  /** Reason given when the user or the SDK cancels this flow. */
  internal abstract val cancelledReason: String

  internal abstract fun cancellation(reason: String): ClerkResult<T, ClerkErrorResponse>

  internal fun completeWithCancellation(reason: String): Boolean =
    result.complete(cancellation(reason))

  /** OAuth or enterprise SSO sign-in or sign-up. */
  internal class Sso(
    override val expectedState: String?,
    val transferable: Boolean,
    val redirectFlow: RedirectFlow,
    val signUp: SignUp?,
  ) : PendingRedirect<OAuthResult>() {
    override val supersededReason = "New authentication started, cancelling previous attempt"
    override val cancelledReason = "Authentication cancelled"

    override fun cancellation(reason: String): ClerkResult<OAuthResult, ClerkErrorResponse> =
      ClerkResult.unknownFailure(SSOCancellationException(reason))
  }

  /** Connecting an OAuth account to the signed-in user. */
  internal class ExternalAccountConnection(
    override val expectedState: String,
    val externalAccountId: String,
  ) : PendingRedirect<com.clerk.api.externalaccount.ExternalAccount>() {
    override val supersededReason =
      "New external account connection started, cancelling previous attempt"
    override val cancelledReason = "External account connection cancelled"

    override fun cancellation(
      reason: String
    ): ClerkResult<com.clerk.api.externalaccount.ExternalAccount, ClerkErrorResponse> =
      ClerkResult.unknownFailure(SSOCancellationException(reason))
  }

  /** Hosted (Account Portal) sign-in or sign-up. */
  internal class HostedAuth(
    val redirectUrl: String,
    val state: String,
    val codeVerifier: String,
  ) : PendingRedirect<Session>() {
    override val expectedState: String = state
    override val supersededReason = HOSTED_AUTH_CANCELLED_BY_NEW_FLOW
    override val cancelledReason = "Authentication cancelled"

    override fun cancellation(reason: String): ClerkResult<Session, ClerkErrorResponse> =
      ClerkResult.unknownFailure(HostedAuthCancellationException(reason))
  }

  internal enum class RedirectFlow {
    SIGN_IN,
    SIGN_UP,
  }
}
