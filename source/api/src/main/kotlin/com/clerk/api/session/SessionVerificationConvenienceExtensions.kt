package com.clerk.api.session

import com.clerk.api.Constants.Strategy.BACKUP_CODE
import com.clerk.api.Constants.Strategy.TOTP
import com.clerk.api.network.model.error.ClerkErrorResponse
import com.clerk.api.network.serialization.ClerkResult
import com.clerk.api.passkeys.PasskeyService

/** Verifies the current session by asking the user to re-enter their password. */
suspend fun Session.verifyWithPassword(
  password: String
): ClerkResult<SessionVerification, ClerkErrorResponse> {
  return attemptFirstFactorVerification(Session.AttemptFirstFactorParams.Password(password))
}

/** Verifies the current session with a TOTP code. */
suspend fun Session.verifyWithTOTP(
  code: String
): ClerkResult<SessionVerification, ClerkErrorResponse> {
  return attemptSecondFactorVerification(strategy = TOTP, code = code)
}

/** Verifies the current session with a backup code. */
suspend fun Session.verifyWithBackupCode(
  code: String
): ClerkResult<SessionVerification, ClerkErrorResponse> {
  return attemptSecondFactorVerification(strategy = BACKUP_CODE, code = code)
}

/** Verifies the current session with a passkey via Android Credential Manager. */
suspend fun Session.verifyWithPasskey(
  allowedCredentialIds: List<String> = emptyList()
): ClerkResult<SessionVerification, ClerkErrorResponse> {
  return PasskeyService.verifySessionWithPasskey(this, allowedCredentialIds)
}
