package com.clerk.api.auth.builders

import com.clerk.api.network.model.error.ClerkErrorResponse
import com.clerk.api.network.serialization.ClerkResult
import com.clerk.api.network.serialization.LocalFailureCodes
import com.clerk.api.network.serialization.localFailure
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

  internal fun channel(): ClerkResult<CodeChannel, ClerkErrorResponse> = codeChannel(email, phone)
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

  internal fun emailAddress(): ClerkResult<String, ClerkErrorResponse> =
    email?.let { ClerkResult.success(it) }
      ?: invalidBuilderArguments("Email must be provided for Enterprise SSO")
}

internal sealed interface CodeChannel {
  val value: String

  data class Email(override val value: String) : CodeChannel

  data class Phone(override val value: String) : CodeChannel
}

internal fun codeChannel(
  email: String?,
  phone: String?,
): ClerkResult<CodeChannel, ClerkErrorResponse> =
  when {
    email != null && phone != null ->
      invalidBuilderArguments("Only one of email or phone should be provided, not both")
    email != null -> ClerkResult.success(CodeChannel.Email(email))
    phone != null -> ClerkResult.success(CodeChannel.Phone(phone))
    else -> invalidBuilderArguments("Either email or phone must be provided")
  }

internal fun invalidBuilderArguments(message: String): ClerkResult.Failure<ClerkErrorResponse> =
  localFailure(code = LocalFailureCodes.INVALID_ARGUMENTS, longMessage = message)
