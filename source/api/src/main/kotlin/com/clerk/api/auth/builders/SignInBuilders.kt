package com.clerk.api.auth.builders

import com.clerk.api.auth.types.IdTokenProvider
import com.clerk.api.network.model.error.ClerkErrorResponse
import com.clerk.api.network.serialization.ClerkResult

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

  /** Returns the identifier to sign in with, or a failure when none was provided. */
  internal fun identifier(): ClerkResult<String, ClerkErrorResponse> {
    val identifier =
      email
        ?: phone
        ?: username
        ?: return invalidBuilderArguments(
          "At least one of email, phone, or username must be provided"
        )
    return ClerkResult.success(identifier)
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

  /** Returns the identifier and password, or a failure when either is missing. */
  internal fun credentials(): ClerkResult<PasswordCredentials, ClerkErrorResponse> {
    val identifier = identifier
    val password = password
    return when {
      identifier == null -> invalidBuilderArguments("Identifier must be provided")
      password == null -> invalidBuilderArguments("Password must be provided")
      else -> ClerkResult.success(PasswordCredentials(identifier = identifier, password = password))
    }
  }

  internal data class PasswordCredentials(val identifier: String, val password: String)
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

  /** Returns the single channel to send the OTP to, or a failure when zero or two are set. */
  internal fun channel(): ClerkResult<OtpChannel, ClerkErrorResponse> {
    val email = email
    val phone = phone
    return when {
      email != null && phone != null ->
        invalidBuilderArguments("Only one of email or phone should be provided, not both")
      email != null -> ClerkResult.success(OtpChannel(identifier = email, isEmail = true))
      phone != null -> ClerkResult.success(OtpChannel(identifier = phone, isEmail = false))
      else -> invalidBuilderArguments("Either email or phone must be provided")
    }
  }

  internal data class OtpChannel(val identifier: String, val isEmail: Boolean)
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

  /** Returns the token and its provider, or a failure when either is missing. */
  internal fun idToken(): ClerkResult<IdToken, ClerkErrorResponse> {
    val token = token
    val provider = provider
    return when {
      token == null -> invalidBuilderArguments("Token must be provided")
      provider == null -> invalidBuilderArguments("Provider must be provided")
      else -> ClerkResult.success(IdToken(token = token, provider = provider))
    }
  }

  internal data class IdToken(val token: String, val provider: IdTokenProvider)
}
