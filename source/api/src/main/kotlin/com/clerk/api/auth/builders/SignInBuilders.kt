package com.clerk.api.auth.builders

import com.clerk.api.auth.types.IdTokenProvider

/**
 * Builder for sign-in with identifier.
 *
 * Use this builder to specify the identifier for starting a sign-in flow. Only one of [email],
 * [phone], or [username] should be set.
 *
 * ### Example usage:
 * ```kotlin
 * clerk.auth.signIn { email = "user@email.com" }
 * // or
 * clerk.auth.signIn { phone = "+1234567890" }
 * // or
 * clerk.auth.signIn { username = "johndoe" }
 * ```
 */
@ClerkDsl
class SignInIdentifierBuilder {
  /** The email address to sign in with. */
  var email: String? = null

  /** The phone number to sign in with. */
  var phone: String? = null

  /** The username to sign in with. */
  var username: String? = null

  internal fun validate() {
    require(email != null || phone != null || username != null) {
      "At least one of email, phone, or username must be provided"
    }
  }

  internal fun getIdentifier(): String {
    return email ?: phone ?: username ?: error("No identifier provided")
  }
}

/**
 * Builder for sign-in with password.
 *
 * ### Example usage:
 * ```kotlin
 * clerk.auth.signInWithPassword {
 *     identifier = "user@email.com"
 *     password = "secretpassword"
 * }
 * ```
 */
@ClerkDsl
class SignInWithPasswordBuilder {
  /** The identifier (email, phone, or username) to sign in with. */
  var identifier: String? = null

  /** The password for authentication. */
  var password: String? = null

  internal fun validate() {
    require(identifier != null) { "Identifier must be provided" }
    require(password != null) { "Password must be provided" }
  }
}

/**
 * Builder for sign-in with OTP (one-shot, automatically sends code).
 *
 * Use this builder to specify the channel for OTP authentication. Only one of [email] or [phone]
 * should be set.
 *
 * ### Example usage:
 * ```kotlin
 * clerk.auth.signInWithOtp { email = "user@email.com" }
 * // or
 * clerk.auth.signInWithOtp { phone = "+1234567890" }
 * ```
 */
@ClerkDsl
class SignInWithOtpBuilder {
  /** The email address to send the OTP to. */
  var email: String? = null

  /** The phone number to send the OTP to. */
  var phone: String? = null

  internal fun validate() {
    require(email != null || phone != null) { "Either email or phone must be provided" }
    require(email == null || phone == null) {
      "Only one of email or phone should be provided, not both"
    }
  }
}

/**
 * Builder for sign-in with ID token.
 *
 * Use this builder to sign in with an ID token from an identity provider such as Google.
 *
 * ### Example usage:
 * ```kotlin
 * clerk.auth.signInWithIdToken {
 *     token = "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9..."
 *     provider = IdTokenProvider.GOOGLE
 * }
 * ```
 */
@ClerkDsl
class SignInWithIdTokenBuilder {
  /** The ID token from the identity provider. */
  var token: String? = null

  /** The identity provider that issued the token. */
  var provider: IdTokenProvider? = null

  internal fun validate() {
    require(token != null) { "Token must be provided" }
    require(provider != null) { "Provider must be provided" }
  }
}
