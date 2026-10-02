@file:Suppress("unused")

package com.clerk.api.signup

import com.clerk.api.auth.builders.SendCodeBuilder
import com.clerk.api.auth.builders.SignUpBuilder
import com.clerk.api.auth.types.VerificationType
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
      if (isEmailLinkVerificationSupported) {
        SignUp.PrepareVerificationParams.Strategy.EmailLink()
      } else {
        SignUp.PrepareVerificationParams.Strategy.EmailCode()
      }
    } else {
      SignUp.PrepareVerificationParams.Strategy.PhoneCode()
    }

  return prepareVerification(strategy)
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
  val params =
    when (type) {
      VerificationType.EMAIL -> SignUp.AttemptVerificationParams.EmailCode(code = code)
      VerificationType.PHONE -> SignUp.AttemptVerificationParams.PhoneCode(code = code)
    }

  return attemptVerification(params)
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

  return update(
    SignUp.SignUpUpdateParams.Standard(
      emailAddress = builder.email,
      phoneNumber = builder.phone,
      password = builder.password,
      firstName = builder.firstName,
      lastName = builder.lastName,
      username = builder.username,
      legalAccepted = builder.legalAccepted,
    )
  )
}
