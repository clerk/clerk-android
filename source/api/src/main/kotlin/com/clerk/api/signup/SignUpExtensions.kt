@file:Suppress("unused")

package com.clerk.api.signup

import com.clerk.api.Clerk
import com.clerk.api.auth.builders.SendCodeBuilder
import com.clerk.api.auth.builders.SignUpBuilder
import com.clerk.api.auth.reportingFailures
import com.clerk.api.auth.types.VerificationType
import com.clerk.api.network.model.error.ClerkErrorResponse
import com.clerk.api.network.serialization.ClerkResult

/**
 * Sends a verification code to the specified email or phone.
 *
 * When the value differs from the sign-up's current [SignUp.emailAddress] or [SignUp.phoneNumber],
 * the sign-up is first updated to it, so the code goes to the address the caller named.
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
public suspend fun SignUp.sendCode(
  block: SendCodeBuilder.() -> Unit
): ClerkResult<SignUp, ClerkErrorResponse> {
  val builder = SendCodeBuilder().apply(block)
  builder.validate()
  val email = builder.email
  val phone = builder.phone

  return Clerk.auth.reportingFailures {
    val target =
      when {
        email != null && !email.trim().equals(emailAddress?.trim(), ignoreCase = true) ->
          updateImpl(SignUp.SignUpUpdateParams.Standard(emailAddress = email))
        email == null && phone != null && phone.digits() != phoneNumber?.digits() ->
          updateImpl(SignUp.SignUpUpdateParams.Standard(phoneNumber = phone))
        else -> ClerkResult.success(this)
      }
    when (target) {
      is ClerkResult.Failure -> target
      is ClerkResult.Success ->
        target.value.prepareVerificationImpl(target.value.sendCodeStrategy(email))
    }
  }
}

private fun SignUp.sendCodeStrategy(email: String?): SignUp.PrepareVerificationParams.Strategy =
  when {
    email == null -> SignUp.PrepareVerificationParams.Strategy.PhoneCode()
    isEmailLinkVerificationSupported -> SignUp.PrepareVerificationParams.Strategy.EmailLink()
    else -> SignUp.PrepareVerificationParams.Strategy.EmailCode()
  }

private fun String.digits(): String = filter(Char::isDigit)

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
public suspend fun SignUp.verifyCode(
  code: String,
  type: VerificationType,
): ClerkResult<SignUp, ClerkErrorResponse> {
  val params =
    when (type) {
      VerificationType.EMAIL -> SignUp.AttemptVerificationParams.EmailCode(code = code)
      VerificationType.PHONE -> SignUp.AttemptVerificationParams.PhoneCode(code = code)
    }

  return attemptVerificationImpl(params)
}

/**
 * Updates the sign-up with additional information.
 *
 * Sending [SignUpBuilder.unsafeMetadata] on update is not supported yet. Setting it here throws
 * [IllegalArgumentException] instead of silently dropping it.
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
public suspend fun SignUp.update(
  block: SignUpBuilder.() -> Unit
): ClerkResult<SignUp, ClerkErrorResponse> {
  val builder = SignUpBuilder().apply(block)
  require(builder.unsafeMetadata == null) { "update { } cannot change unsafeMetadata" }

  return updateImpl(
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
