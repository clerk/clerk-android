package com.clerk.api.auth.builders

import com.clerk.api.auth.types.IdTokenProvider
import com.clerk.api.network.model.error.ClerkErrorResponse
import com.clerk.api.network.serialization.ClerkResult

/**
 * Builder for sign-in with identifier.
 *
 * Use this builder to specify the identifier for starting a sign-in flow. Only one of [email],
 * [phone], [username], or [identifier] should be set.
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
public class SignInIdentifierBuilder {
  /** The email address to sign in with. */
  public var email: String? = null

  /** The phone number to sign in with. */
  public var phone: String? = null

  /** The username to sign in with. */
  public var username: String? = null

  /**
   * An identifier of any type, such as text a user typed into a single "email, phone or username"
   * field. Clerk works out which kind it is.
   */
  public var identifier: String? = null

  /**
   * Returns the identifier to sign in with, or a failure when none was provided or [identifier] was
   * combined with a typed field.
   */
  internal fun resolvedIdentifier(): ClerkResult<String, ClerkErrorResponse> {
    val typed = email ?: phone ?: username
    val identifier = identifier
    return when {
      identifier != null && (email != null || phone != null || username != null) ->
        invalidBuilderArguments("identifier cannot be combined with email, phone, or username")
      identifier != null -> ClerkResult.success(identifier)
      typed != null -> ClerkResult.success(typed)
      else ->
        invalidBuilderArguments(
          "At least one of email, phone, username, or identifier must be provided"
        )
    }
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
public class SignInWithPasswordBuilder {
  /** The identifier (email, phone, or username) to sign in with. */
  public var identifier: String? = null

  /** The password for authentication. */
  public var password: String? = null

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
public class SignInWithOtpBuilder {
  /** The email address to send the OTP to. */
  public var email: String? = null

  /** The phone number to send the OTP to. */
  public var phone: String? = null

  /** Returns the channel to send the OTP to, or a failure when zero or two are set. */
  internal fun channel(): ClerkResult<CodeChannel, ClerkErrorResponse> = codeChannel(email, phone)
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
public class SignInWithIdTokenBuilder {
  /** The ID token from the identity provider. */
  public var token: String? = null

  /** The identity provider that issued the token. */
  public var provider: IdTokenProvider? = null

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
