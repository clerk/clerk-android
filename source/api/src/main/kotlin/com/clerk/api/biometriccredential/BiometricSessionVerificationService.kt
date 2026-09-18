package com.clerk.api.biometriccredential

import com.clerk.api.Clerk
import com.clerk.api.Constants.Strategy.TRUSTED_DEVICE
import com.clerk.api.network.model.error.ClerkErrorResponse
import com.clerk.api.network.serialization.ClerkResult
import com.clerk.api.network.serialization.flatMap
import com.clerk.api.network.serialization.fold
import com.clerk.api.session.Session
import com.clerk.api.session.SessionTokenFetcher
import com.clerk.api.session.SessionVerification
import com.clerk.api.session.attemptFirstFactorVerification
import com.clerk.api.session.attemptSecondFactorVerification
import com.clerk.api.session.prepareFirstFactorVerification
import com.clerk.api.session.prepareSecondFactorVerification
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

/** Coordinates session reverification using the existing biometric credential store and signer. */
internal object BiometricSessionVerificationService {
  suspend fun verifySession(
    session: Session,
    promptTitle: String?,
    promptSubtitle: String?,
    level: SessionVerification.Level,
  ): ClerkResult<SessionVerification, ClerkErrorResponse> {
    return selectCredential(session, level).flatMap { credential ->
      prepareAndSign(session, credential, level, promptTitle, promptSubtitle).flatMap { signature ->
        currentCoroutineContext().ensureActive()
        when (val result = attempt(session, credential.id, signature, level)) {
          is ClerkResult.Failure ->
            BiometricCredentials.handleBiometricCredentialError(result, credential)
          is ClerkResult.Success ->
            result.also {
              if (it.value.status == SessionVerification.Status.COMPLETE) {
                SessionTokenFetcher.shared.invalidateSession(session.id)
              }
            }
        }
      }
    }
  }

  private suspend fun prepareAndSign(
    session: Session,
    credential: BiometricCredentialLocalRecord,
    level: SessionVerification.Level,
    promptTitle: String?,
    promptSubtitle: String?,
  ): ClerkResult<BiometricCredentialKeySignature, ClerkErrorResponse> {
    currentCoroutineContext().ensureActive()
    return prepare(session, credential.id, level)
      .fold(
        onFailure = { BiometricCredentials.handleBiometricCredentialError(it, credential) },
        onSuccess = { prepared ->
          val factor =
            if (level == SessionVerification.Level.SECOND_FACTOR) {
              prepared.secondFactorVerification
            } else {
              prepared.firstFactorVerification
            }
          val challenge = factor?.biometricCredentialChallenge
          if (factor?.strategy != TRUSTED_DEVICE || challenge == null) {
            BiometricCredentials.clientFailure(
              "Biometric reverification did not return a matching challenge."
            )
          } else {
            BiometricCredentials.signChallenge(challenge, credential, promptTitle, promptSubtitle)
          }
        },
      )
  }

  private fun selectCredential(
    session: Session,
    level: SessionVerification.Level,
  ): ClerkResult<BiometricCredentialLocalRecord, ClerkErrorResponse> {
    // Reverification responses omit the user from their embedded session.
    val userId =
      session.user?.id
        ?: Clerk.clientFlow.value?.sessions?.firstOrNull { it.id == session.id }?.user?.id
    return when {
      level != SessionVerification.Level.FIRST_FACTOR &&
        level != SessionVerification.Level.SECOND_FACTOR ->
        ClerkResult.unknownFailure(
          IllegalArgumentException(
            "Biometric verification level must be first_factor or second_factor"
          )
        )
      !session.status.allowsBiometricCredentialEnrollment || userId == null ->
        BiometricCredentials.clientFailure(
          "Biometric reverification requires an active or pending session with a user."
        )
      else -> BiometricCredentials.localCredentialForReverification(userId)
    }
  }

  private suspend fun prepare(
    session: Session,
    credentialId: String,
    level: SessionVerification.Level,
  ): ClerkResult<SessionVerification, ClerkErrorResponse> =
    if (level == SessionVerification.Level.SECOND_FACTOR) {
      session.prepareSecondFactorVerification(
        Session.PrepareSecondFactorParams(TRUSTED_DEVICE, biometricCredentialId = credentialId)
      )
    } else {
      session.prepareFirstFactorVerification(
        Session.PrepareFirstFactorParams(TRUSTED_DEVICE, biometricCredentialId = credentialId)
      )
    }

  private suspend fun attempt(
    session: Session,
    credentialId: String,
    signature: BiometricCredentialKeySignature,
    level: SessionVerification.Level,
  ): ClerkResult<SessionVerification, ClerkErrorResponse> =
    if (level == SessionVerification.Level.SECOND_FACTOR) {
      session.attemptSecondFactorVerification(
        Session.AttemptSecondFactorParams.BiometricCredential(
          credentialId,
          signature.clientData,
          signature.signature,
          signature.algorithm,
        )
      )
    } else {
      session.attemptFirstFactorVerification(
        Session.AttemptFirstFactorParams.BiometricCredential(
          credentialId,
          signature.clientData,
          signature.signature,
          signature.algorithm,
        )
      )
    }
}
