package com.clerk.ui.auth

import com.clerk.api.Clerk
import com.clerk.api.SignUp
import com.clerk.api.VerificationStrategy

private val fieldOrder = listOf("email_address", "phone_number", "username", "password")
internal val SignUp.firstFieldToCollect: String?
  get() =
    missingFields
      .filter { it in requiredFields }
      .map { it.rawValue }
      .sortedBy { fieldOrder.indexOf(it).takeIf { it >= 0 } ?: Int.MAX_VALUE }
      .firstOrNull()
internal val SignUp.firstFieldToVerify: String?
  get() =
    unverifiedFields
      .map { it.rawValue }
      .sortedBy { fieldOrder.indexOf(it).takeIf { it >= 0 } ?: Int.MAX_VALUE }
      .firstOrNull()

internal fun SignUp.emailVerificationStrategy(clerk: Clerk): String =
  verifications.emailAddress.strategy
    ?: if (
      clerk.environment.userSettings.attributes["email_address"]
        ?.verifications
        ?.contains(VerificationStrategy.EmailLink) == true
    )
      "email_link"
    else "email_code"
