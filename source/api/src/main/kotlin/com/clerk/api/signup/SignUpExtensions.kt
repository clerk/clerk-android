@file:Suppress("unused")

package com.clerk.api.signup

import com.clerk.api.Constants.Strategy as AuthStrategy
import com.clerk.api.auth.builders.SendCodeBuilder
import com.clerk.api.auth.builders.SignUpBuilder
import com.clerk.api.auth.types.VerificationType
import com.clerk.api.network.ClerkApi
import com.clerk.api.network.model.error.ClerkErrorResponse
import com.clerk.api.network.serialization.ClerkResult

/**
 * Sends a verification code to the specified email or phone.
 *
 * @param block Builder block to configure where to send the code.
 * @return A [ClerkResult] containing the updated [SignUp] object on success, or a
 *   [ClerkErrorResponse] on failure.
 *
 * ### Example usage:
 * ```kotlin
 * signUp.sendCode { email = "newuser@email.com" }
 * // or
 * signUp.sendCode { phone = "+1234567890" }
 * ```
 */
suspend fun SignUp.sendCode(
  block: SendCodeBuilder.() -> Unit
): ClerkResult<SignUp, ClerkErrorResponse> {
  val builder = SendCodeBuilder().apply(block)
  builder.validate()

  val strategy =
    if (builder.email != null) {
      SignUp.PrepareVerificationParams.Strategy.EmailCode()
    } else {
      SignUp.PrepareVerificationParams.Strategy.PhoneCode()
    }

  return ClerkApi.signUp.prepareSignUpVerification(this.id, strategy.strategy)
}

/**
 * Verifies with the provided code and type.
 *
 * Type is required since multiple verifications can be active during sign-up (e.g., both email and
 * phone verifications).
 *
 * @param code The verification code to verify.
 * @param type The type of verification (EMAIL or PHONE).
 * @return A [ClerkResult] containing the updated [SignUp] object on success, or a
 *   [ClerkErrorResponse] on failure.
 *
 * ### Example usage:
 * ```kotlin
 * signUp.verifyCode("123456", VerificationType.EMAIL)
 * signUp.verifyCode("654321", VerificationType.PHONE)
 * ```
 */
suspend fun SignUp.verifyCode(
  code: String,
  type: VerificationType,
): ClerkResult<SignUp, ClerkErrorResponse> {
  val strategy =
    when (type) {
      VerificationType.EMAIL -> AuthStrategy.EMAIL_CODE
      VerificationType.PHONE -> AuthStrategy.PHONE_CODE
    }

  return ClerkApi.signUp.attemptSignUpVerification(
    signUpId = this.id,
    strategy = strategy,
    code = code,
  )
}

/**
 * Updates the sign-up with additional information.
 *
 * @param block Builder block to configure the update.
 * @return A [ClerkResult] containing the updated [SignUp] object on success, or a
 *   [ClerkErrorResponse] on failure.
 *
 * ### Example usage:
 * ```kotlin
 * signUp.update {
 *     firstName = "John"
 *     lastName = "Doe"
 * }
 * ```
 */
suspend fun SignUp.update(
  block: SignUpBuilder.() -> Unit
): ClerkResult<SignUp, ClerkErrorResponse> {
  val builder = SignUpBuilder().apply(block)

  val params = buildMap {
    builder.email?.let { put("email_address", it) }
    builder.phone?.let { put("phone_number", it) }
    builder.password?.let { put("password", it) }
    builder.firstName?.let { put("first_name", it) }
    builder.lastName?.let { put("last_name", it) }
    builder.username?.let { put("username", it) }
    builder.legalAccepted?.let { put("legal_accepted", it.toString()) }
  }

  return ClerkApi.signUp.updateSignUp(this.id, params)
}
