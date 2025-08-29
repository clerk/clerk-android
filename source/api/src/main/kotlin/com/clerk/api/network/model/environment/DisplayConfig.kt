package com.clerk.api.network.model.environment

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Internal data class representing display configuration from the Clerk environment.
 *
 * This class contains UI and branding configuration that controls how authentication interfaces are
 * displayed to users, including application branding, preferred sign-in methods, and various URL
 * configurations.
 *
 * @property instanceEnvironmentType The type of environment (development, staging, production)
 * @property applicationName The display name of the application
 * @property preferredSignInStrategy The preferred sign-in strategy for the application
 * @property branded Whether the application uses Clerk branding
 * @property logoImageUrl URL of the application's logo image
 * @property homeUrl The home URL of the application
 * @property privacyPolicyUrl URL of the privacy policy (optional)
 * @property termsUrl URL of the terms of service (optional)
 * @property googleOneTapClientId Google One Tap client ID for enhanced sign-in (optional)
 */
@Serializable
internal data class DisplayConfig(
  /** The type of environment (development, staging, production) */
  @SerialName("instance_environment_type") val instanceEnvironmentType: InstanceEnvironmentType,

  /** The display name of the application */
  @SerialName("application_name") val applicationName: String,

  /** The preferred sign-in strategy for the application */
  @SerialName("preferred_sign_in_strategy") val preferredSignInStrategy: PreferredSignInStrategy,

  /** Whether the application uses Clerk branding */
  @SerialName("branded") val branded: Boolean,

  /** URL of the application's logo image */
  @SerialName("logo_image_url") val logoImageUrl: String,

  /** The home URL of the application */
  @SerialName("home_url") val homeUrl: String,

  /** URL of the privacy policy (optional) */
  @SerialName("privacy_policy_url") val privacyPolicyUrl: String?,

  /** URL of the terms of service (optional) */
  @SerialName("terms_url") val termsUrl: String?,

  /** Google One Tap client ID for enhanced sign-in (optional) */
  @SerialName("google_one_tap_client_id") val googleOneTapClientId: String?,
)

/**
 * Enumeration of preferred sign-in strategies.
 *
 * This enum defines the different sign-in methods that can be set as the preferred option for users
 * when they authenticate with the application.
 */
@Serializable(with = PreferredSignInStrategySerializer::class)
internal enum class PreferredSignInStrategy {
  /** Password-based authentication is preferred */
  @SerialName("password") PASSWORD,

  /** One-time password (OTP) authentication is preferred */
  @SerialName("otp") OTP,

  /** Unknown or unspecified strategy */
  @SerialName("unknown") UNKNOWN,
}

/**
 * Custom serializer for PreferredSignInStrategy that provides fallback to UNKNOWN.
 */
internal object PreferredSignInStrategySerializer : com.clerk.api.network.serialization.FallbackEnumSerializer<PreferredSignInStrategy>(
  "PreferredSignInStrategy",
  PreferredSignInStrategy.UNKNOWN,
  PreferredSignInStrategy.entries.toTypedArray()
)
