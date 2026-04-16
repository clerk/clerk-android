package com.clerk.api.magiclink

import com.clerk.api.Clerk
import com.clerk.api.network.ClerkApi
import com.clerk.api.network.model.error.ClerkErrorResponse
import com.clerk.api.network.model.magiclink.NativeMagicLinkCompleteRequest
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
      is ClerkResult.Success -> {
        NativeMagicLinkLogger.ticketReceived(pending.state)
        completeFromTicket(completeResult.value.ticket, pending.state)
      }
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
    ticket: String,
    state: PendingNativeMagicLinkState,
  ): ClerkResult<NativeMagicLinkAuthResult, NativeMagicLinkError> {
    return when (state) {
      PendingNativeMagicLinkState.SIGN_IN -> {
        when (val ticketSignInResult = Clerk.auth.signInWithTicket(ticket)) {
          is ClerkResult.Failure -> {
            clearPendingFlow()
            val mapped =
              ticketSignInResult.toNativeMagicLinkError(
                NativeMagicLinkReason.TICKET_SIGN_IN_FAILED
              )
            NativeMagicLinkLogger.completeFailure(mapped.reasonCode)
            ClerkResult.apiFailure(mapped)
          }
          is ClerkResult.Success -> {
            NativeMagicLinkLogger.ticketExchangeSuccess(
              state = state,
              createdSessionId = ticketSignInResult.value.createdSessionId,
            )
            completeAfterTicketSignIn(ticketSignInResult.value)
          }
        }
      }
      PendingNativeMagicLinkState.SIGN_UP -> {
        when (val ticketSignUpResult = Clerk.auth.signUpWithTicket(ticket)) {
          is ClerkResult.Failure -> {
            clearPendingFlow()
            val mapped =
              ticketSignUpResult.toNativeMagicLinkError(
                NativeMagicLinkReason.TICKET_SIGN_UP_FAILED
              )
            NativeMagicLinkLogger.completeFailure(mapped.reasonCode)
            ClerkResult.apiFailure(mapped)
          }
          is ClerkResult.Success -> {
            NativeMagicLinkLogger.ticketExchangeSuccess(
              state = state,
              createdSessionId = ticketSignUpResult.value.createdSessionId,
            )
            completeAfterTicketSignUp(ticketSignUpResult.value)
          }
        }
      }
    }
  }

  private suspend fun completeAfterTicketSignIn(
    signIn: SignIn
  ): ClerkResult<NativeMagicLinkAuthResult, NativeMagicLinkError> {
    NativeMagicLinkLogger.sessionActivationStarted(
      state = PendingNativeMagicLinkState.SIGN_IN,
      createdSessionId = signIn.createdSessionId,
    )
    val activationResult = activateCreatedSession(signIn.createdSessionId)
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
      ClerkResult.success(NativeMagicLinkAuthResult.SignIn(signIn))
    }
  }

  private suspend fun completeAfterTicketSignUp(
    signUp: SignUp
  ): ClerkResult<NativeMagicLinkAuthResult, NativeMagicLinkError> {
    NativeMagicLinkLogger.sessionActivationStarted(
      state = PendingNativeMagicLinkState.SIGN_UP,
      createdSessionId = signUp.createdSessionId,
    )
    val activationResult = activateCreatedSession(signUp.createdSessionId)
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
      ClerkResult.success(NativeMagicLinkAuthResult.SignUp(signUp))
    }
  }
}
