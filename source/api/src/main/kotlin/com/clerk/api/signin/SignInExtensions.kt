@file:Suppress("unused")

package com.clerk.api.signin

import com.clerk.api.Constants.Strategy.EMAIL_CODE
import com.clerk.api.Constants.Strategy.PHONE_CODE
import com.clerk.api.Constants.Strategy.RESET_PASSWORD_EMAIL_CODE
import com.clerk.api.Constants.Strategy.RESET_PASSWORD_PHONE_CODE
import com.clerk.api.auth.builders.SendCodeBuilder
import com.clerk.api.auth.types.MfaType
import com.clerk.api.network.ClerkApi
import com.clerk.api.network.model.error.ClerkErrorResponse
import com.clerk.api.network.serialization.ClerkResult

/**
 * Sends a verification code to the specified email or phone.
 *
 * @param block Builder block to configure where to send the code.
 * @return A [ClerkResult] containing the updated [SignIn] object on success, or a
 *   [ClerkErrorResponse] on failure.
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

  val params =
    if (builder.email != null) {
      val emailAddressId =
        supportedFirstFactors?.find { it.strategy == EMAIL_CODE }?.emailAddressId ?: ""
      SignIn.PrepareFirstFactorParams.EmailCode(emailAddressId = emailAddressId)
    } else {
      val phoneNumberId =
        supportedFirstFactors?.find { it.strategy == PHONE_CODE }?.phoneNumberId ?: ""
      SignIn.PrepareFirstFactorParams.PhoneCode(phoneNumberId = phoneNumberId)
    }

  return ClerkApi.signIn.prepareSignInFirstFactor(this.id, params.toMap())
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
  // Infer the strategy from firstFactorVerification
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

  return ClerkApi.signIn.attemptFirstFactor(id = this.id, params = params.toMap())
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
  return ClerkApi.signIn.attemptFirstFactor(id = this.id, params = params.toMap())
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
  return ClerkApi.signIn.attemptFirstFactor(id = this.id, params = params.toMap())
}

/**
 * Sends an MFA verification code.
 *
 * @param block Builder block to configure where to send the MFA code.
 * @return A [ClerkResult] containing the updated [SignIn] object on success, or a
 *   [ClerkErrorResponse] on failure.
 *
 * ### Example usage:
 * ```kotlin
 * signIn.sendMfaCode { phone = "+1234567890" }
 * // or
 * signIn.sendMfaCode { email = "user@email.com" }
 * ```
 */
suspend fun SignIn.sendMfaCode(
  block: SendCodeBuilder.() -> Unit
): ClerkResult<SignIn, ClerkErrorResponse> {
  val builder = SendCodeBuilder().apply(block)
  builder.validate()

  val params =
    if (builder.phone != null) {
      val phoneNumberId =
        supportedSecondFactors
          ?.find { it.strategy == SignIn.PrepareSecondFactorParams.PHONE_CODE }
          ?.phoneNumberId
      SignIn.PrepareSecondFactorParams(
        strategy = SignIn.PrepareSecondFactorParams.PHONE_CODE,
        phoneNumberId = phoneNumberId,
      )
    } else {
      val emailAddressId =
        supportedSecondFactors
          ?.find { it.strategy == SignIn.PrepareSecondFactorParams.EMAIL_CODE }
          ?.emailAddressId
      SignIn.PrepareSecondFactorParams(
        strategy = SignIn.PrepareSecondFactorParams.EMAIL_CODE,
        emailAddressId = emailAddressId,
      )
    }

  return ClerkApi.signIn.prepareSecondFactor(id = id, params = params.toMap())
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

  return ClerkApi.signIn.attemptSecondFactor(id = this.id, params = params.toMap())
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

  val params =
    if (builder.email != null) {
      val emailAddressId =
        supportedFirstFactors?.find { it.strategy == EMAIL_CODE }?.emailAddressId ?: ""
      SignIn.PrepareFirstFactorParams.ResetPasswordEmailCode(emailAddressId = emailAddressId)
    } else {
      val phoneNumberId =
        supportedFirstFactors?.find { it.strategy == PHONE_CODE }?.phoneNumberId ?: ""
      SignIn.PrepareFirstFactorParams.ResetPasswordPhoneCode(phoneNumberId = phoneNumberId)
    }

  return ClerkApi.signIn.prepareSignInFirstFactor(this.id, params.toMap())
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
  return ClerkApi.signIn.resetPassword(id = this.id, password = newPassword, signOutOfOtherSessions)
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
  return ClerkApi.signIn.fetchSignIn(id = this.id, rotatingTokenNonce = rotatingTokenNonce)
}
