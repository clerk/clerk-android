package com.clerk.api.auth.builders

import com.clerk.api.network.model.error.ClerkErrorResponse
import com.clerk.api.network.serialization.ClerkResult
import com.clerk.api.network.serialization.LocalFailureCodes
import com.clerk.api.network.serialization.localFailure

/**
 * DSL marker annotation for Clerk DSL builders.
 *
 * This annotation prevents implicit access to outer receivers in nested DSL scopes, improving type
 * safety and code clarity.
 */
@DslMarker annotation class ClerkDsl

/**
 * Builder for sending verification codes (email or phone).
 *
 * Use this builder to specify the channel for sending a verification code. Only one of [email] or
 * [phone] should be set.
 *
 * ### Example usage:
 * ```kotlin
 * signIn.sendCode { email = "user@email.com" }
 * // or
 * signIn.sendCode { phone = "+1234567890" }
 * ```
 */
@ClerkDsl
class SendCodeBuilder {
  /** The email address to send the verification code to. */
  var email: String? = null

  /** The phone number to send the verification code to. */
  var phone: String? = null

  /** Returns a failure when zero or two channels are set, or null when exactly one is. */
  internal fun validationFailure(): ClerkResult.Failure<ClerkErrorResponse>? =
    when {
      email == null && phone == null ->
        invalidBuilderArguments("Either email or phone must be provided")
      email != null && phone != null ->
        invalidBuilderArguments("Only one of email or phone should be provided, not both")
      else -> null
    }
}

/**
 * Builder for Enterprise SSO authentication.
 *
 * ### Example usage:
 * ```kotlin
 * clerk.auth.signInWithEnterpriseSSO { email = "user@company.com" }
 * ```
 */
@ClerkDsl
class EnterpriseSsoBuilder {
  /**
   * The email address for Enterprise SSO authentication. This is typically used to determine the
   * SSO provider based on the email domain.
   */
  var email: String? = null

  /** Returns the email address, or a failure when it is missing. */
  internal fun emailAddress(): ClerkResult<String, ClerkErrorResponse> =
    email?.let { ClerkResult.success(it) }
      ?: invalidBuilderArguments("Email must be provided for Enterprise SSO")
}

/** Failure returned when a DSL builder block leaves out or conflicts required values. */
internal fun invalidBuilderArguments(message: String): ClerkResult.Failure<ClerkErrorResponse> =
  localFailure(code = LocalFailureCodes.INVALID_ARGUMENTS, longMessage = message)
