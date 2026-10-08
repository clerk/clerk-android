package com.clerk.api.auth.types

import com.clerk.api.sso.OAuthProvider
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/**
 * A typed authentication or verification strategy, such as `email_code` or `oauth_google`.
 *
 * The Clerk API exchanges strategies as plain strings, and the SDK's models keep exposing those raw
 * strings (for example [com.clerk.api.network.model.factor.Factor.strategy]). Use the typed
 * accessors next to them (for example [com.clerk.api.network.model.factor.Factor.strategyType]) to
 * compare strategies without string literals:
 * ```kotlin
 * when (val strategy = factor.strategyType) {
 *   Strategy.EmailCode -> showEmailCodeInput()
 *   Strategy.Password -> showPasswordInput()
 *   is Strategy.OAuth -> showSocialButton(strategy.provider)
 *   else -> showGenericFactor()
 * }
 * ```
 *
 * Strategies the SDK does not know about decode to [Unknown] and keep their raw wire value, so a
 * new server-side strategy never breaks decoding and round-trips unchanged. Two strategies are
 * equal when their [value]s are equal. Use [Strategy.from] to convert a raw string.
 *
 * @property value The raw strategy string sent to and received from the Clerk API.
 */
@Serializable(with = StrategySerializer::class)
public sealed class Strategy(public val value: String) {

  /** A one-time code sent by SMS. */
  public object PhoneCode : Strategy("phone_code")

  /** A one-time code sent by email. */
  public object EmailCode : Strategy("email_code")

  /** A magic link sent by email. */
  public object EmailLink : Strategy("email_link")

  /** A time-based one-time password from an authenticator app. */
  public object Totp : Strategy("totp")

  /** A single-use backup code. */
  public object BackupCode : Strategy("backup_code")

  /** The user's password. */
  public object Password : Strategy("password")

  /** A WebAuthn passkey. */
  public object Passkey : Strategy("passkey")

  /** A password-reset code sent by email. */
  public object ResetPasswordEmailCode : Strategy("reset_password_email_code")

  /** A password-reset code sent by SMS. */
  public object ResetPasswordPhoneCode : Strategy("reset_password_phone_code")

  /** A sign-in or sign-up ticket, such as an invitation or sign-in token. */
  public object Ticket : Strategy("ticket")

  /** A transfer between a sign-in and a sign-up attempt. */
  public object Transfer : Strategy("transfer")

  /** Enterprise SSO (SAML or OIDC) through an enterprise connection. */
  public object EnterpriseSso : Strategy("enterprise_sso")

  /** Legacy SAML SSO. */
  public object Saml : Strategy("saml")

  /** A biometric credential stored on this device. */
  public object TrustedDevice : Strategy("trusted_device")

  /** A Google One Tap / Credential Manager ID token. */
  public object GoogleOneTap : Strategy("google_one_tap")

  /**
   * A redirect-based OAuth strategy, `oauth_<provider>` (for example `oauth_google` or
   * `oauth_custom_acme`).
   */
  public class OAuth internal constructor(value: String) : Strategy(value) {
    /** The OAuth provider this strategy authenticates with. */
    public val provider: OAuthProvider
      get() = OAuthProvider.fromStrategy(value)
  }

  /** A native OAuth token exchange strategy, `oauth_token_<provider>`. */
  public class OAuthToken internal constructor(value: String) : Strategy(value)

  /**
   * A strategy this version of the SDK does not recognize.
   *
   * @property raw The raw strategy string returned by the Clerk API.
   */
  public class Unknown internal constructor(raw: String) : Strategy(raw) {
    public val raw: String
      get() = value
  }

  final override fun equals(other: Any?): Boolean = other is Strategy && other.value == value

  final override fun hashCode(): Int = value.hashCode()

  /** Returns the raw wire [value], so a [Strategy] can be interpolated into requests and logs. */
  final override fun toString(): String = value

  public companion object {
    private const val OAUTH_PREFIX = "oauth_"
    private const val OAUTH_TOKEN_PREFIX = "oauth_token_"

    // Lazy so that initializing a nested object (which initializes this class first) never reads
    // a not-yet-constructed sibling instance.
    private val known: Map<String, Strategy> by lazy {
      listOf(
          PhoneCode,
          EmailCode,
          EmailLink,
          Totp,
          BackupCode,
          Password,
          Passkey,
          ResetPasswordEmailCode,
          ResetPasswordPhoneCode,
          Ticket,
          Transfer,
          EnterpriseSso,
          Saml,
          TrustedDevice,
          GoogleOneTap,
        )
        .associateBy { it.value }
    }

    /**
     * Converts a raw strategy string from the Clerk API into a [Strategy].
     *
     * Never fails: unrecognized values become [Unknown] and keep [value].
     */
    @JvmStatic
    public fun from(value: String): Strategy =
      known[value]
        ?: when {
          value.startsWith(OAUTH_TOKEN_PREFIX) -> OAuthToken(value)
          value.startsWith(OAUTH_PREFIX) -> OAuth(value)
          else -> Unknown(value)
        }
  }
}

internal object StrategySerializer : KSerializer<Strategy> {
  override val descriptor: SerialDescriptor =
    PrimitiveSerialDescriptor("com.clerk.api.auth.types.Strategy", PrimitiveKind.STRING)

  override fun serialize(encoder: Encoder, value: Strategy) = encoder.encodeString(value.value)

  override fun deserialize(decoder: Decoder): Strategy = Strategy.from(decoder.decodeString())
}
