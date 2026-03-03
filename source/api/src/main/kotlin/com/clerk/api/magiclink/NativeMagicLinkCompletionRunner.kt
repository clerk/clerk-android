package com.clerk.api.magiclink

import com.clerk.api.Clerk
import com.clerk.api.network.ClerkApi
import com.clerk.api.network.model.error.ClerkErrorResponse
import com.clerk.api.network.model.magiclink.NativeMagicLinkCompleteRequest
import com.clerk.api.network.serialization.ClerkResult
import com.clerk.api.signin.SignIn

internal class NativeMagicLinkCompletionRunner(
  private val attestationProvider: NativeMagicLinkAttestationProvider?,
  private val clearPendingFlow: suspend () -> Unit,
  private val activateCreatedSession: suspend (SignIn) -> ClerkResult<Unit, NativeMagicLinkError>,
  private val refreshClientState: suspend () -> Unit,
) {
  suspend fun complete(
    flowId: String,
    approvalToken: String,
    pending: PendingNativeMagicLinkFlow,
  ): ClerkResult<SignIn, NativeMagicLinkError> {
    val completeRequest =
      NativeMagicLinkCompleteRequest(
        flowId = flowId,
        approvalToken = approvalToken,
        codeVerifier = pending.codeVerifier,
        attestation = attestationProvider?.attestation(),
      )

    return when (val completeResult = ClerkApi.magicLink.complete(completeRequest.toFields())) {
      is ClerkResult.Failure -> handleCompleteApiFailure(completeResult)
      is ClerkResult.Success -> completeFromTicket(completeResult.value.ticket)
    }
  }

  private suspend fun handleCompleteApiFailure(
    completeResult: ClerkResult.Failure<ClerkErrorResponse>
  ): ClerkResult.Failure<NativeMagicLinkError> {
    val mapped = completeResult.toNativeMagicLinkError(NativeMagicLinkReason.COMPLETE_FAILED)
    if (mapped.reasonCode in TERMINAL_REASON_CODES) {
      clearPendingFlow()
    }
    NativeMagicLinkLogger.completeFailure(mapped.reasonCode)
    return ClerkResult.apiFailure(mapped)
  }

  private suspend fun completeFromTicket(
    ticket: String
  ): ClerkResult<SignIn, NativeMagicLinkError> {
    return when (val ticketSignInResult = Clerk.auth.signInWithTicket(ticket)) {
      is ClerkResult.Failure -> {
        clearPendingFlow()
        val mapped =
          ticketSignInResult.toNativeMagicLinkError(NativeMagicLinkReason.TICKET_SIGN_IN_FAILED)
        NativeMagicLinkLogger.completeFailure(mapped.reasonCode)
        ClerkResult.apiFailure(mapped)
      }
      is ClerkResult.Success -> completeAfterTicketSignIn(ticketSignInResult.value)
    }
  }

  private suspend fun completeAfterTicketSignIn(
    signIn: SignIn
  ): ClerkResult<SignIn, NativeMagicLinkError> {
    val activationResult = activateCreatedSession(signIn)
    return if (activationResult is ClerkResult.Failure) {
      clearPendingFlow()
      val reasonCode =
        activationResult.error?.reasonCode ?: NativeMagicLinkReason.SESSION_ACTIVATION_FAILED.code
      NativeMagicLinkLogger.completeFailure(reasonCode)
      ClerkResult.apiFailure(activationResult.error)
    } else {
      clearPendingFlow()
      refreshClientState()
      NativeMagicLinkLogger.completeSuccess()
      ClerkResult.success(signIn)
    }
  }
}
