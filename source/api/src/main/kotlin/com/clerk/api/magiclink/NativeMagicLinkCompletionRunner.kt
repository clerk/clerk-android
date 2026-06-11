package com.clerk.api.magiclink

import com.clerk.api.Clerk
import com.clerk.api.auth.AuthEvent
import com.clerk.api.network.ClerkApi
import com.clerk.api.network.model.error.ClerkErrorResponse
import com.clerk.api.network.model.magiclink.NativeMagicLinkCompleteRequest
import com.clerk.api.network.model.magiclink.NativeMagicLinkCompleteResponse
import com.clerk.api.network.serialization.ClerkResult
import com.clerk.api.signin.SignIn
import com.clerk.api.signup.SignUp

internal class NativeMagicLinkCompletionRunner(
  private val attestationProvider: NativeMagicLinkAttestationProvider?,
  private val clearPendingFlow: suspend () -> Unit,
  private val activateCreatedSession: suspend (String?) -> ClerkResult<Unit, NativeMagicLinkError>,
  private val refreshClientState: suspend () -> Unit,
) {
  suspend fun complete(
    flowId: String,
    approvalToken: String,
    pending: PendingNativeMagicLinkFlow,
  ): ClerkResult<NativeMagicLinkAuthResult, NativeMagicLinkError> {
    val completeRequest =
      NativeMagicLinkCompleteRequest(
        flowId = flowId,
        approvalToken = approvalToken,
        codeVerifier = pending.codeVerifier,
        attestation = attestationProvider?.attestation(),
      )

    NativeMagicLinkLogger.completeApiStarted(
      state = pending.state,
      hasAttestation = completeRequest.attestation != null,
    )

    return when (val completeResult = ClerkApi.magicLink.complete(completeRequest.toFields())) {
      is ClerkResult.Failure -> handleCompleteApiFailure(completeResult)
      is ClerkResult.Success -> completeFromResponse(completeResult.value, pending.state)
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

  private suspend fun completeFromResponse(
    response: NativeMagicLinkCompleteResponse,
    state: PendingNativeMagicLinkState,
  ): ClerkResult<NativeMagicLinkAuthResult, NativeMagicLinkError> {
    return when (state) {
      PendingNativeMagicLinkState.SIGN_IN -> {
        when (response) {
          is NativeMagicLinkCompleteResponse.Ticket -> {
            NativeMagicLinkLogger.ticketReceived(state)
            completeSignInFromTicket(response.ticket)
          }
          is NativeMagicLinkCompleteResponse.SignUpResult -> handleUnexpectedCompleteResponse()
        }
      }
      PendingNativeMagicLinkState.SIGN_UP -> {
        when (response) {
          is NativeMagicLinkCompleteResponse.SignUpResult -> completeAfterSignUp(response.signUp)
          is NativeMagicLinkCompleteResponse.Ticket -> handleUnexpectedCompleteResponse()
        }
      }
    }
  }

  private suspend fun completeSignInFromTicket(
    ticket: String
  ): ClerkResult<NativeMagicLinkAuthResult, NativeMagicLinkError> {
    return when (val ticketSignInResult = Clerk.auth.signInWithTicket(ticket)) {
      is ClerkResult.Failure -> {
        clearPendingFlow()
        val mapped =
          ticketSignInResult.toNativeMagicLinkError(NativeMagicLinkReason.TICKET_SIGN_IN_FAILED)
        NativeMagicLinkLogger.completeFailure(mapped.reasonCode)
        ClerkResult.apiFailure(mapped)
      }
      is ClerkResult.Success -> {
        NativeMagicLinkLogger.ticketExchangeSuccess(
          state = PendingNativeMagicLinkState.SIGN_IN,
          createdSessionId = ticketSignInResult.value.createdSessionId,
        )
        completeAfterTicketSignIn(ticketSignInResult.value)
      }
    }
  }

  private suspend fun handleUnexpectedCompleteResponse():
    ClerkResult.Failure<NativeMagicLinkError> {
    clearPendingFlow()
    val error = NativeMagicLinkError(reasonCode = NativeMagicLinkReason.COMPLETE_FAILED.code)
    NativeMagicLinkLogger.completeFailure(error.reasonCode)
    return ClerkResult.apiFailure(error)
  }

  private suspend fun completeAfterTicketSignIn(
    signIn: SignIn
  ): ClerkResult<NativeMagicLinkAuthResult, NativeMagicLinkError> {
    if (signIn.createdSessionId == null) {
      clearPendingFlow()
      NativeMagicLinkLogger.completeSuccess()
      return ClerkResult.success(NativeMagicLinkAuthResult.SignIn(signIn))
    }

    NativeMagicLinkLogger.sessionActivationStarted(
      state = PendingNativeMagicLinkState.SIGN_IN,
      createdSessionId = signIn.createdSessionId,
    )
    val activationResult = activateCreatedSession(signIn.createdSessionId)
    return if (activationResult is ClerkResult.Failure) {
      clearPendingFlow()
      val reasonCode =
        activationResult.error?.reasonCode ?: NativeMagicLinkReason.SESSION_ACTIVATION_FAILED.code
      val error = activationResult.error ?: NativeMagicLinkError(reasonCode = reasonCode)
      NativeMagicLinkLogger.completeFailure(reasonCode)
      ClerkResult.apiFailure(error)
    } else {
      clearPendingFlow()
      refreshClientState()
      NativeMagicLinkLogger.completeSuccess()
      ClerkResult.success(NativeMagicLinkAuthResult.SignIn(signIn))
    }
  }

  private suspend fun completeAfterSignUp(
    signUp: SignUp
  ): ClerkResult<NativeMagicLinkAuthResult, NativeMagicLinkError> {
    clearPendingFlow()
    if (signUp.status == SignUp.Status.COMPLETE) {
      Clerk.auth.send(AuthEvent.SignUpCompleted(signUp))
    }
    NativeMagicLinkLogger.completeSuccess()
    return ClerkResult.success(NativeMagicLinkAuthResult.SignUp(signUp))
  }
}
