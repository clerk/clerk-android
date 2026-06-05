@file:Suppress("unused", "TooManyFunctions")

package com.clerk.api.session

import com.clerk.api.Constants.Strategy.BACKUP_CODE
import com.clerk.api.Constants.Strategy.EMAIL_CODE
import com.clerk.api.Constants.Strategy.ENTERPRISE_SSO
import com.clerk.api.Constants.Strategy.PHONE_CODE
import com.clerk.api.Constants.Strategy.TOTP
import com.clerk.api.network.model.error.ClerkErrorResponse
import com.clerk.api.network.serialization.ClerkResult
import com.clerk.api.passkeys.PasskeyService
import com.clerk.api.sso.RedirectConfiguration

/** Sends a verification code to the email address for first-factor reverification. */
suspend fun Session.sendEmailCode(
  emailAddressId: String
): ClerkResult<SessionVerification, ClerkErrorResponse> {
  return prepareFirstFactorVerification(strategy = EMAIL_CODE, emailAddressId = emailAddressId)
}

/** Sends a verification code to the phone number for first-factor reverification. */
suspend fun Session.sendPhoneCode(
  phoneNumberId: String
): ClerkResult<SessionVerification, ClerkErrorResponse> {
  return prepareFirstFactorVerification(strategy = PHONE_CODE, phoneNumberId = phoneNumberId)
}

/** Verifies the current session with an email code. */
suspend fun Session.verifyWithEmailCode(
  code: String
): ClerkResult<SessionVerification, ClerkErrorResponse> {
  return attemptFirstFactorVerification(strategy = EMAIL_CODE, code = code)
}

/** Verifies the current session with a phone code as a first factor. */
suspend fun Session.verifyWithPhoneCode(
  code: String
): ClerkResult<SessionVerification, ClerkErrorResponse> {
  return attemptFirstFactorVerification(strategy = PHONE_CODE, code = code)
}

/** Verifies the current session by asking the user to re-enter their password. */
suspend fun Session.verifyWithPassword(
  password: String
): ClerkResult<SessionVerification, ClerkErrorResponse> {
  return attemptFirstFactorVerification(Session.AttemptFirstFactorParams.Password(password))
}

/** Starts Enterprise SSO for first-factor reverification. */
suspend fun Session.startEnterpriseSso(
  emailAddressId: String? = null,
  enterpriseConnectionId: String? = null,
  redirectUrl: String? = null,
): ClerkResult<SessionVerification, ClerkErrorResponse> {
  return prepareFirstFactorVerification(
    strategy = ENTERPRISE_SSO,
    emailAddressId = emailAddressId,
    enterpriseConnectionId = enterpriseConnectionId,
    redirectUrl = redirectUrl ?: RedirectConfiguration.DEFAULT_REDIRECT_URL,
  )
}

/** Sends an MFA code to the phone number for second-factor reverification. */
suspend fun Session.sendMfaPhoneCode(
  phoneNumberId: String
): ClerkResult<SessionVerification, ClerkErrorResponse> {
  return prepareSecondFactorVerification(strategy = PHONE_CODE, phoneNumberId = phoneNumberId)
}

/** Verifies the current session with a phone code as a second factor. */
suspend fun Session.verifyWithMfaPhoneCode(
  code: String
): ClerkResult<SessionVerification, ClerkErrorResponse> {
  return attemptSecondFactorVerification(strategy = PHONE_CODE, code = code)
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
