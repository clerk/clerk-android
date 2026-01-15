package com.clerk.api.auth.builders

/**
 * Builder for sign-up with standard details.
 *
 * ### Example usage:
 * ```kotlin
 * clerk.auth.signUp {
 *     email = "newuser@email.com"
 *     password = "secretpassword"
 *     firstName = "John"
 *     lastName = "Doe"
 * }
 * ```
 */
@ClerkDsl
class SignUpBuilder {
  /** The email address for the new account. */
  var email: String? = null

  /** The phone number for the new account. */
  var phone: String? = null

  /** The password for the new account. */
  var password: String? = null

  /** The user's first name. */
  var firstName: String? = null

  /** The user's last name. */
  var lastName: String? = null

  /** The username for the new account. */
  var username: String? = null

  /**
   * Custom metadata that will be attached to the created user. This metadata is not validated by
   * Clerk and should not contain sensitive information.
   */
  var unsafeMetadata: Map<String, Any>? = null

  /** Whether the user has accepted the legal terms (privacy policy and terms of service). */
  var legalAccepted: Boolean? = null
}

/**
 * Builder for sign-up with ID token.
 *
 * This builder allows providing additional user information when signing up with an ID token from
 * an identity provider.
 *
 * ### Example usage:
 * ```kotlin
 * clerk.auth.signUpWithIdToken(token, IdTokenProvider.GOOGLE) {
 *     firstName = "John"
 *     lastName = "Doe"
 * }
 * ```
 */
@ClerkDsl
class SignUpWithIdTokenBuilder {
  /** The user's first name. */
  var firstName: String? = null

  /** The user's last name. */
  var lastName: String? = null
}
