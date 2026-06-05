package com.clerk.api.session

import com.clerk.api.network.ClerkApi
import com.clerk.api.network.model.error.ClerkErrorResponse
import com.clerk.api.network.serialization.ClerkResult

/** Starts an in-session reverification flow. */
suspend fun Session.startVerification(
  level: SessionVerification.Level
): ClerkResult<SessionVerification, ClerkErrorResponse> {
  return ClerkApi.session.startVerification(
    sessionId = id,
    params = Session.StartVerificationParams(level = level.value).toMap(),
  )
}

/** Prepares the first factor of an in-session reverification flow. */
internal suspend fun Session.prepareFirstFactorVerification(
  strategy: String,
  emailAddressId: String? = null,
  phoneNumberId: String? = null,
  enterpriseConnectionId: String? = null,
  redirectUrl: String? = null,
): ClerkResult<SessionVerification, ClerkErrorResponse> {
  return prepareFirstFactorVerification(
    Session.PrepareFirstFactorParams(
      strategy = strategy,
      emailAddressId = emailAddressId,
      phoneNumberId = phoneNumberId,
      enterpriseConnectionId = enterpriseConnectionId,
      redirectUrl = redirectUrl,
    )
  )
}

/** Prepares the first factor of an in-session reverification flow. */
internal suspend fun Session.prepareFirstFactorVerification(
  params: Session.PrepareFirstFactorParams
): ClerkResult<SessionVerification, ClerkErrorResponse> {
  return ClerkApi.session.prepareFirstFactorVerification(sessionId = id, params = params.toMap())
}

/** Attempts the first factor of an in-session reverification flow. */
internal suspend fun Session.attemptFirstFactorVerification(
  params: Session.AttemptFirstFactorParams
): ClerkResult<SessionVerification, ClerkErrorResponse> {
  return ClerkApi.session.attemptFirstFactorVerification(sessionId = id, params = params.toMap())
}

/** Attempts the first factor of an in-session reverification flow. */
internal suspend fun Session.attemptFirstFactorVerification(
  strategy: String,
  code: String? = null,
  password: String? = null,
  publicKeyCredential: String? = null,
): ClerkResult<SessionVerification, ClerkErrorResponse> {
  val params =
    when {
      password != null -> Session.AttemptFirstFactorParams.Password(password = password)
      publicKeyCredential != null ->
        Session.AttemptFirstFactorParams.Passkey(publicKeyCredential = publicKeyCredential)
      code != null -> Session.AttemptFirstFactorParams.Code(strategy = strategy, code = code)
      else -> error("One of code, password, or publicKeyCredential is required")
    }

  return attemptFirstFactorVerification(params)
}

/** Prepares the second factor of an in-session reverification flow. */
internal suspend fun Session.prepareSecondFactorVerification(
  strategy: String,
  phoneNumberId: String? = null,
): ClerkResult<SessionVerification, ClerkErrorResponse> {
  return ClerkApi.session.prepareSecondFactorVerification(
    sessionId = id,
    params =
      Session.PrepareSecondFactorParams(strategy = strategy, phoneNumberId = phoneNumberId).toMap(),
  )
}

/** Attempts the second factor of an in-session reverification flow. */
internal suspend fun Session.attemptSecondFactorVerification(
  strategy: String,
  code: String,
): ClerkResult<SessionVerification, ClerkErrorResponse> {
  return ClerkApi.session.attemptSecondFactorVerification(
    sessionId = id,
    params = Session.AttemptSecondFactorParams(strategy = strategy, code = code).toMap(),
  )
}
