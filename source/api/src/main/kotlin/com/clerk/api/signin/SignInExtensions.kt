@file:Suppress("unused", "TooManyFunctions")

package com.clerk.api.signin

import com.clerk.api.Clerk
import com.clerk.api.Constants.Strategy.EMAIL_CODE
import com.clerk.api.Constants.Strategy.EMAIL_LINK
import com.clerk.api.Constants.Strategy.PHONE_CODE
import com.clerk.api.Constants.Strategy.RESET_PASSWORD_EMAIL_CODE
import com.clerk.api.Constants.Strategy.RESET_PASSWORD_PHONE_CODE
import com.clerk.api.auth.builders.SendCodeBuilder
import com.clerk.api.auth.reportingFailures
import com.clerk.api.auth.types.MfaType
import com.clerk.api.magiclink.NativeMagicLinkService
import com.clerk.api.network.ClerkApi
import com.clerk.api.network.model.environment.PreferredSignInStrategy
import com.clerk.api.network.model.error.ClerkErrorResponse
import com.clerk.api.network.model.error.Error
import com.clerk.api.network.model.factor.Factor
import com.clerk.api.network.model.factor.FactorComparators
import com.clerk.api.network.model.factor.isResetFactor
import com.clerk.api.network.serialization.ClerkResult
import com.clerk.api.sso.OAuthResult
import com.clerk.api.sso.SSOService

// region Factor Selection Extensions

/**
 * Retrieves a list of alternative first factors for the current sign-in attempt, excluding the
 * specified factor and certain strategy types.
 *
 * This function filters the [SignIn.supportedFirstFactors] to provide alternative options for
 * first-factor authentication. It excludes the currently provided [factor], reset factors, OAuth,
 * Enterprise SSO, and SAML strategies. The remaining factors are then sorted based on a predefined
 * order, with unknown strategies placed at the end.
 *
 * @param factor The [Factor] to exclude from the list of alternatives.
 * @return A [List] of alternative [Factor] objects, sorted by preference. Returns an empty list if
 *   no suitable alternatives are found or if [SignIn.supportedFirstFactors] is null.
 */
fun SignIn.alternativeFirstFactors(factor: Factor? = null): List<Factor> {
  val firstFactors = supportedFirstFactors?.filter {
    it != factor &&
      !it.isResetFactor() &&
      !it.strategy.contains("oauth") &&
      it.strategy != "enterprise_sso" &&
      it.strategy != "saml"
  }
  return (firstFactors ?: emptyList()).sortedWith(FactorComparators.allStrategiesButtonsComparator)
}

/**
 * Returns a list of alternative second factors, sorted by a predefined order, excluding the
 * provided factor.
 *
 * This function filters the [SignIn.supportedSecondFactors] to remove the specified [factor] and
 * then sorts the remaining factors based on the `strategySortOrderBackupCodePref` list. Factors not
 * found in the sort order list are placed at the end.
 *
 * @param factor The factor to exclude from the returned list.
 * @return A list of alternative second factors, sorted according to the predefined order.
 */
fun SignIn.alternativeSecondFactors(factor: Factor): List<Factor> {
  return supportedSecondFactors
    ?.filter { it != factor }
    .orEmpty()
    .sortedWith(comparator = FactorComparators.backupCodePrefComparator)
}

/**
 * Determines the starting first factor for a sign-in attempt based on the preferred sign-in
 * strategy.
 *
 * This property inspects the `preferredSignInStrategy` from the Clerk environment's display
 * configuration.
 * - If the preferred strategy is `PASSWORD`, it returns the result of
 *   [factorWhenPasswordIsPreferred].
 * - Otherwise (implying an OTP-based preference), it returns the result of
 *   [factorWhenOtpIsPreferred].
 *
 * @return The [Factor] to be presented as the initial first factor, or `null` if no suitable factor
 *   is found.
 */
val SignIn.startingFirstFactor: Factor?
  get() =
    when (Clerk.environment?.displayConfig?.preferredSignInStrategy) {
      PreferredSignInStrategy.PASSWORD -> this.factorWhenPasswordIsPreferred
      else -> this.factorWhenOtpIsPreferred
    }

val SignIn.startingSecondFactor: Factor?
  get() {
    supportedSecondFactors
      ?.firstOrNull { it.strategy == "passkey" }
      ?.let {
        return it
      }
    supportedSecondFactors
      ?.firstOrNull { it.strategy == "totp" }
      ?.let {
        return it
      }
    supportedSecondFactors
      ?.firstOrNull { it.strategy == "phone_code" }
      ?.let {
        return it
      }
    return supportedSecondFactors?.firstOrNull()
  }

private val SignIn.factorWhenPasswordIsPreferred: Factor?
  get() {
    val availableFirstFactors = supportedFirstFactors ?: return null

    availableFirstFactors
      .firstOrNull { it.strategy == "passkey" }
      ?.let {
        return it
      }

    availableFirstFactors
      .firstOrNull { it.strategy == "password" }
      ?.let {
        return it
      }

    availableFirstFactors.emailLinkFactorForIdentifier(identifier)?.let {
      return it
    }

    val sorted = availableFirstFactors.sortedWith(FactorComparators.passwordPrefComparator)
    return availableFirstFactors.firstOrNull { it.safeIdentifier == identifier }
      ?: sorted.firstOrNull()
  }

private val SignIn.factorWhenOtpIsPreferred: Factor?
  get() {
    val availableFirstFactors = supportedFirstFactors ?: return null

    availableFirstFactors
      .firstOrNull { it.strategy == "passkey" }
      ?.let {
        return it
      }

    availableFirstFactors.emailLinkFactorForIdentifier(identifier)?.let {
      return it
    }

    val sorted = availableFirstFactors.sortedWith(FactorComparators.otpPrefComparator)
    return sorted.firstOrNull { it.safeIdentifier == identifier } ?: sorted.firstOrNull()
  }

private fun List<Factor>.emailLinkFactorForIdentifier(identifier: String?): Factor? {
  val isEmailIdentifier = !identifier.isNullOrBlank() && identifier.contains("@")
  val matchingEmailFactor =
    if (isEmailIdentifier) {
      firstOrNull { it.isEmailFactor && it.safeIdentifier == identifier }
    } else {
      null
    }

  return when {
    !isEmailIdentifier -> null
    matchingEmailFactor == null -> filter { it.strategy == EMAIL_LINK }.singleOrNull()
    else ->
      firstOrNull { it.strategy == EMAIL_LINK && it.hasSameIdentityAs(matchingEmailFactor) }
        ?: matchingEmailFactor.takeIf { it.strategy == EMAIL_LINK }
  }
}

private val Factor.isEmailFactor: Boolean
  get() = strategy == EMAIL_LINK || strategy == EMAIL_CODE

private fun Factor.hasSameIdentityAs(other: Factor): Boolean {
  return (!emailAddressId.isNullOrBlank() && emailAddressId == other.emailAddressId) ||
    (!phoneNumberId.isNullOrBlank() && phoneNumberId == other.phoneNumberId) ||
    (!safeIdentifier.isNullOrBlank() && safeIdentifier == other.safeIdentifier)
}

// endregion

// region Auth Namespace Extension Functions

/**
 * Continues a prepared redirect verification.
 *
 * Use this after a sign-in has prepared a first factor that returned an
 * `externalVerificationRedirectUrl`, such as Enterprise SSO.
 *
 * @param transferable Whether this authentication flow allows transferring to a sign-up if the user
 *   doesn't have an account. Defaults to `true`.
 * @return A [ClerkResult] containing the redirect authentication result on success, or a
 *   [ClerkErrorResponse] on failure.
 */
suspend fun SignIn.authenticateWithPreparedRedirect(
  transferable: Boolean = true
): ClerkResult<OAuthResult, ClerkErrorResponse> =
  Clerk.auth.reportingFailures {
    val externalVerificationRedirectUrl =
      firstFactorVerification?.externalVerificationRedirectUrl
        ?: return@reportingFailures ClerkResult.unknownFailure(
          IllegalStateException("External verification redirect URL is missing")
        )

    SSOService.authenticateWithPreparedRedirect(
      externalVerificationRedirectUrl = externalVerificationRedirectUrl,
      transferable = transferable,
    )
  }

/**
 * Sends a verification code to the specified email or phone.
 *
 * The code is sent to the first factor whose identifier matches [SendCodeBuilder.email] or
 * [SendCodeBuilder.phone]. When the sign-in only exposes a masked identifier for its single
 * matching factor (for example after identifying by username), that factor is used.
 *
 * @param block Builder block to configure where to send the code.
 * @return A [ClerkResult] containing the updated [SignIn] object on success, or a
 *   [ClerkErrorResponse] on failure. Fails without contacting Clerk when no first factor matches.
 *
 * ### Example usage:
 * ```kotlin
 * signIn.sendCode { email = "user@email.com" }
 * // or
 * signIn.sendCode { phone = "+1234567890" }
 * ```
 */
suspend fun SignIn.sendCode(
  block: SendCodeBuilder.() -> Unit
): ClerkResult<SignIn, ClerkErrorResponse> {
  val builder = SendCodeBuilder().apply(block)
  builder.validate()

  return Clerk.auth.reportingFailures {
    val email = builder.email
    if (email != null) {
      val factor = firstFactorFor(listOf(EMAIL_CODE), email, ::sameEmail)
      val emailAddressId =
        factor?.emailAddressId ?: return@reportingFailures noMatchingFactor(EMAIL_CODE)
      prepareFirstFactor(SignIn.PrepareFirstFactorParams.EmailCode(emailAddressId))
    } else {
      val factor = firstFactorFor(listOf(PHONE_CODE), builder.phone!!, ::samePhone)
      val phoneNumberId =
        factor?.phoneNumberId ?: return@reportingFailures noMatchingFactor(PHONE_CODE)
      prepareFirstFactor(SignIn.PrepareFirstFactorParams.PhoneCode(phoneNumberId))
    }
  }
}

/**
 * Finds the first factor of one of [strategies] that belongs to [value].
 *
 * Falls back to the only candidate when its identifier is masked or absent, since Clerk masks
 * identifiers the user has not typed in this sign-in and the caller cannot know the masked form.
 */
private fun SignIn.firstFactorFor(
  strategies: List<String>,
  value: String,
  matches: (String, String) -> Boolean,
): Factor? {
  val candidates = supportedFirstFactors.orEmpty().filter { it.strategy in strategies }
  val matching = candidates.filter { factor ->
    factor.safeIdentifier?.let { matches(it, value) } == true
  }
  val onlyTarget = candidates.distinctBy { it.emailAddressId ?: it.phoneNumberId }.singleOrNull()
  return strategies.firstNotNullOfOrNull { strategy ->
    matching.firstOrNull { it.strategy == strategy }
  }
    ?: onlyTarget?.takeIf {
      it.safeIdentifier.isNullOrBlank() || MASK_CHARACTER in it.safeIdentifier
    }
}

private fun sameEmail(a: String, b: String): Boolean = a.trim().equals(b.trim(), ignoreCase = true)

private fun samePhone(a: String, b: String): Boolean {
  val digits = a.filter(Char::isDigit)
  return digits.isNotEmpty() && digits == b.filter(Char::isDigit)
}

private fun noMatchingFactor(strategy: String): ClerkResult.Failure<ClerkErrorResponse> =
  invalidPrepareState(
    code = "first_factor_strategy_not_supported",
    longMessage = "No $strategy first factor matches the requested identifier",
  )

private const val MASK_CHARACTER = '*'

/**
 * Sends a verification link to the user's email address for first factor authentication.
 *
 * This is a convenience method that prepares the email link verification strategy. The link will be
 * sent to the email address associated with the sign-in. After the user opens the link, handle the
 * deep link callback with [com.clerk.api.auth.Auth.handle].
 *
 * @param emailAddressId Optional ID of the email address to send the link to. If not provided, the
 *   email address ID will be automatically retrieved from the supported first factors.
 * @return A [ClerkResult] containing the updated [SignIn] object on success, or a
 *   [ClerkErrorResponse] on failure.
 */
suspend fun SignIn.sendEmailLink(
  emailAddressId: String? = null
): ClerkResult<SignIn, ClerkErrorResponse> {
  val emailId =
    emailAddressId
      ?: supportedFirstFactors?.find { it.strategy == EMAIL_LINK }?.emailAddressId
      ?: error("No email address found for email_link strategy")
  val supportedFirstFactorStrategies = supportedFirstFactors?.map { it.strategy }.orEmpty()
  val validationError =
    when {
      status != SignIn.Status.NEEDS_FIRST_FACTOR ->
        invalidEmailLinkPrepareState(
          code = "sign_in_status_invalid",
          longMessage = "Cannot prepare first factor while sign-in status is ${status.name}",
        )
      EMAIL_LINK !in supportedFirstFactorStrategies ->
        invalidEmailLinkPrepareState(
          code = "first_factor_strategy_not_supported",
          longMessage = "$EMAIL_LINK is not supported for this sign-in attempt",
        )
      else -> null
    }

  return Clerk.auth.reportingFailures {
    validationError ?: NativeMagicLinkService.prepareSignInEmailLink(this, emailId)
  }
}

private fun invalidEmailLinkPrepareState(
  code: String,
  longMessage: String,
): ClerkResult.Failure<ClerkErrorResponse> {
  return ClerkResult.apiFailure(
    ClerkErrorResponse(
      errors = listOf(Error(message = "is invalid", longMessage = longMessage, code = code))
    )
  )
}

/**
 * Verifies the first factor with the provided code.
 *
 * The verification channel (email or phone) is automatically inferred from the
 * [SignIn.firstFactorVerification] state.
 *
 * @param code The verification code to verify.
 * @return A [ClerkResult] containing the updated [SignIn] object on success, or a
 *   [ClerkErrorResponse] on failure.
 *
 * ### Example usage:
 * ```kotlin
 * signIn.verifyCode("123456")
 * ```
 */
suspend fun SignIn.verifyCode(code: String): ClerkResult<SignIn, ClerkErrorResponse> {
  val strategy = firstFactorVerification?.strategy ?: EMAIL_CODE

  val params =
    when (strategy) {
      PHONE_CODE -> SignIn.AttemptFirstFactorParams.PhoneCode(code = code)
      RESET_PASSWORD_EMAIL_CODE ->
        SignIn.AttemptFirstFactorParams.ResetPasswordEmailCode(code = code)
      RESET_PASSWORD_PHONE_CODE ->
        SignIn.AttemptFirstFactorParams.ResetPasswordPhoneCode(code = code)
      else -> SignIn.AttemptFirstFactorParams.EmailCode(code = code)
    }

  return Clerk.auth.reportingFailures {
    ClerkApi.signIn.attemptFirstFactor(id = this.id, params = params.toMap())
  }
}

/**
 * Verifies the first factor with a password.
 *
 * @param password The password to verify.
 * @return A [ClerkResult] containing the updated [SignIn] object on success, or a
 *   [ClerkErrorResponse] on failure.
 *
 * ### Example usage:
 * ```kotlin
 * signIn.verifyWithPassword("secretpassword")
 * ```
 */
suspend fun SignIn.verifyWithPassword(password: String): ClerkResult<SignIn, ClerkErrorResponse> {
  val params = SignIn.AttemptFirstFactorParams.Password(password = password)
  return Clerk.auth.reportingFailures {
    ClerkApi.signIn.attemptFirstFactor(id = this.id, params = params.toMap())
  }
}

/**
 * Verifies the first factor with a passkey credential.
 *
 * @param credential The passkey credential for authentication.
 * @return A [ClerkResult] containing the updated [SignIn] object on success, or a
 *   [ClerkErrorResponse] on failure.
 *
 * ### Example usage:
 * ```kotlin
 * signIn.verifyWithPasskey(credential)
 * ```
 */
suspend fun SignIn.verifyWithPasskey(credential: String): ClerkResult<SignIn, ClerkErrorResponse> {
  val params = SignIn.AttemptFirstFactorParams.Passkey(publicKeyCredential = credential)
  return Clerk.auth.reportingFailures {
    ClerkApi.signIn.attemptFirstFactor(id = this.id, params = params.toMap())
  }
}

/**
 * Verifies MFA with the provided code and type.
 *
 * @param code The MFA verification code.
 * @param type The type of MFA being verified.
 * @return A [ClerkResult] containing the updated [SignIn] object on success, or a
 *   [ClerkErrorResponse] on failure.
 *
 * ### Example usage:
 * ```kotlin
 * signIn.verifyMfaCode("123456", MfaType.PHONE_CODE)
 * signIn.verifyMfaCode("123456", MfaType.TOTP)
 * signIn.verifyMfaCode("backup123", MfaType.BACKUP_CODE)
 * ```
 */
suspend fun SignIn.verifyMfaCode(
  code: String,
  type: MfaType,
): ClerkResult<SignIn, ClerkErrorResponse> {
  val params =
    when (type) {
      MfaType.PHONE_CODE -> SignIn.AttemptSecondFactorParams.PhoneCode(code = code)
      MfaType.EMAIL_CODE -> SignIn.AttemptSecondFactorParams.EmailCode(code = code)
      MfaType.TOTP -> SignIn.AttemptSecondFactorParams.TOTP(code = code)
      MfaType.BACKUP_CODE -> SignIn.AttemptSecondFactorParams.BackupCode(code = code)
    }

  return Clerk.auth.reportingFailures {
    ClerkApi.signIn.attemptSecondFactor(id = this.id, params = params.toMap())
  }
}

/**
 * Sends a password reset verification code.
 *
 * @param block Builder block to configure where to send the reset code.
 * @return A [ClerkResult] containing the updated [SignIn] object on success, or a
 *   [ClerkErrorResponse] on failure.
 *
 * ### Example usage:
 * ```kotlin
 * signIn.sendResetPasswordCode { email = "user@email.com" }
 * // or
 * signIn.sendResetPasswordCode { phone = "+1234567890" }
 * ```
 */
suspend fun SignIn.sendResetPasswordCode(
  block: SendCodeBuilder.() -> Unit
): ClerkResult<SignIn, ClerkErrorResponse> {
  val builder = SendCodeBuilder().apply(block)
  builder.validate()

  return Clerk.auth.reportingFailures {
    val email = builder.email
    if (email != null) {
      val factor = firstFactorFor(listOf(RESET_PASSWORD_EMAIL_CODE, EMAIL_CODE), email, ::sameEmail)
      val emailAddressId =
        factor?.emailAddressId
          ?: return@reportingFailures noMatchingFactor(RESET_PASSWORD_EMAIL_CODE)
      prepareFirstFactor(SignIn.PrepareFirstFactorParams.ResetPasswordEmailCode(emailAddressId))
    } else {
      val factor =
        firstFactorFor(listOf(RESET_PASSWORD_PHONE_CODE, PHONE_CODE), builder.phone!!, ::samePhone)
      val phoneNumberId =
        factor?.phoneNumberId
          ?: return@reportingFailures noMatchingFactor(RESET_PASSWORD_PHONE_CODE)
      prepareFirstFactor(SignIn.PrepareFirstFactorParams.ResetPasswordPhoneCode(phoneNumberId))
    }
  }
}

/**
 * Resets the password after verification.
 *
 * @param newPassword The new password to set.
 * @param signOutOfOtherSessions Whether to sign out of other sessions. Defaults to `false`.
 * @return A [ClerkResult] containing the updated [SignIn] object on success, or a
 *   [ClerkErrorResponse] on failure.
 *
 * ### Example usage:
 * ```kotlin
 * signIn.resetPassword(
 *     newPassword = "newpassword",
 *     signOutOfOtherSessions = true
 * )
 * ```
 */
suspend fun SignIn.resetPassword(
  newPassword: String,
  signOutOfOtherSessions: Boolean = false,
): ClerkResult<SignIn, ClerkErrorResponse> {
  return Clerk.auth.reportingFailures {
    ClerkApi.signIn.resetPassword(id = this.id, password = newPassword, signOutOfOtherSessions)
  }
}

/**
 * Reloads the SignIn from the server.
 *
 * This function can be used to refresh the SignIn object and get the latest status and verification
 * information.
 *
 * @param rotatingTokenNonce Optional nonce for rotating token validation.
 * @return A [ClerkResult] containing the refreshed [SignIn] object on success, or a
 *   [ClerkErrorResponse] on failure.
 *
 * ### Example usage:
 * ```kotlin
 * signIn.reload()
 * ```
 */
suspend fun SignIn.reload(
  rotatingTokenNonce: String? = null
): ClerkResult<SignIn, ClerkErrorResponse> {
  return Clerk.auth.reportingFailures {
    ClerkApi.signIn.fetchSignIn(id = this.id, rotatingTokenNonce = rotatingTokenNonce)
  }
}

// endregion
