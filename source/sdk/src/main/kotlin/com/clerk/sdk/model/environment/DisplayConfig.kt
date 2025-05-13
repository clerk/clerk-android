package com.clerk.sdk.model.environment

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DisplayConfig(
  @SerialName("instance_environment_type") val instanceEnvironmentType: InstanceEnvironmentType,
  @SerialName("application_name") val applicationName: String,
  @SerialName("preferred_sign_in_strategy") val preferredSignInStrategy: PreferredSignInStrategy,
  @SerialName("branded") val branded: Boolean,
  @SerialName("logo_image_url") val logoImageUrl: String,
  @SerialName("home_url") val homeUrl: String,
  @SerialName("privacy_policy_url") val privacyPolicyUrl: String?,
  @SerialName("terms_url") val termsUrl: String?,
)

@Serializable
enum class PreferredSignInStrategy {
  @SerialName("password") PASSWORD,
  @SerialName("otp") OTP,
  @SerialName("unknown") UNKNOWN,
}
