package com.clerk.api.auth.builders

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

  internal fun validate() {
    require(email != null || phone != null) { "Either email or phone must be provided" }
    require(email == null || phone == null) {
      "Only one of email or phone should be provided, not both"
    }
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

  internal fun validate() {
    require(email != null) { "Email must be provided for Enterprise SSO" }
  }
}
