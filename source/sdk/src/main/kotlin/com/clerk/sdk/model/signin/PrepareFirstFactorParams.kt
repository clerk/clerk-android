package com.clerk.sdk.model.signin

import kotlinx.serialization.Serializable

/** A parameter object for preparing the first factor verification. */
@Serializable
data class PrepareFirstFactorParams(
  /**
   * The strategy value depends on the object's identifier value. Each authentication identifier
   * supports different verification strategies.
   */
  val strategy: String,

  /**
   * Unique identifier for the user's email address that will receive an email message with the
   * one-time authentication code. This parameter will work only when the `email_code` strategy is
   * specified.
   */
  val emailAddressId: String? = null,

  /**
   * Unique identifier for the user's phone number that will receive an SMS message with the
   * one-time authentication code. This parameter will work only when the `phone_code` strategy is
   * specified.
   */
  val phoneNumberId: String? = null,

  /**
   * The URL that the OAuth provider should redirect to, on successful authorization on their part.
   * This parameter is required only if you set the strategy param to an OAuth strategy like
   * `oauth_<provider>`.
   */
  val redirectUrl: String? = null,
)

/**
 * Represents the strategies for beginning the first factor verification process.
 *
 * The `PrepareFirstFactorStrategy` sealed class defines the different methods available for
 * verifying the first factor in the sign-in process. Each strategy corresponds to a specific type
 * of authentication.
 */
sealed class PrepareFirstFactorStrategy {
  /** The user will receive a one-time authentication code via email. */
  data class EmailCode(val emailAddressId: String? = null) : PrepareFirstFactorStrategy()

  /** The user will receive a one-time authentication code via SMS. */
  data class PhoneCode(val phoneNumberId: String? = null) : PrepareFirstFactorStrategy()

  /** The user will be authenticated with their social connection account. */
  data class OAuth(val provider: OAuthProvider, val redirectUrl: String? = null) :
    PrepareFirstFactorStrategy()

  /**
   * The user will be authenticated either through SAML or OIDC, depending on the configuration of
   * their enterprise SSO account.
   */
  data class EnterpriseSSO(val redirectUrl: String? = null) : PrepareFirstFactorStrategy()

  /** The verification will attempt to be completed using the user's passkey. */
  object Passkey : PrepareFirstFactorStrategy()

  /** Used during a password reset flow. The user will receive a one-time code via email. */
  data class ResetPasswordEmailCode(val emailAddressId: String? = null) :
    PrepareFirstFactorStrategy()

  /** Used during a password reset flow. The user will receive a one-time code via SMS. */
  data class ResetPasswordPhoneCode(val phoneNumberId: String? = null) :
    PrepareFirstFactorStrategy()

  /** Returns the strategy string for the current verification method. */
  val strategy: String
    get() =
      when (this) {
        is EmailCode -> "email_code"
        is PhoneCode -> "phone_code"
        is OAuth -> provider
        is EnterpriseSSO -> "enterprise_sso"
        is Passkey -> "passkey"
        is ResetPasswordEmailCode -> "reset_password_email_code"
        is ResetPasswordPhoneCode -> "reset_password_phone_code"
      }.toString()
}
