package com.clerk.api.auth.builders

import com.clerk.api.sso.RedirectConfiguration

/**
 * DSL marker annotation for Clerk DSL builders.
 *
 * This annotation prevents implicit access to outer receivers in nested DSL scopes, improving type
 * safety and code clarity.
 */
@DslMarker public annotation class ClerkDsl

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
public class SendCodeBuilder {
  /** The email address to send the verification code to. */
  public var email: String? = null

  /** The phone number to send the verification code to. */
  public var phone: String? = null

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
public class EnterpriseSsoBuilder {
  /**
   * The email address for Enterprise SSO authentication. This is typically used to determine the
   * SSO provider based on the email domain.
   */
  public var email: String? = null

  /**
   * The native callback URL. Defaults to the callback registered by the SDK. A custom value needs
   * an intent filter that routes it to `com.clerk.api.sso.SSOReceiverActivity` in the application
   * manifest.
   */
  public var redirectUrl: String = RedirectConfiguration.DEFAULT_REDIRECT_URL

  internal fun validate() {
    require(email != null) { "Email must be provided for Enterprise SSO" }
  }
}
