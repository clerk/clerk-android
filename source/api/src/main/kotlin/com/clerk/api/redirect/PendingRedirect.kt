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

internal sealed class PendingRedirect<T : Any> {
  abstract val expectedState: String?

  internal val result = CompletableDeferred<ClerkResult<T, ClerkErrorResponse>>()
  internal val completionStarted = AtomicBoolean(false)
  internal val completionJob = AtomicReference<Job?>(null)

  internal abstract val supersededReason: String

  internal abstract val cancelledReason: String

  internal abstract fun cancellation(reason: String): ClerkResult<T, ClerkErrorResponse>

  internal fun completeWithCancellation(reason: String): Boolean =
    result.complete(cancellation(reason))

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
