package com.clerk.api.network.model.environment

import com.clerk.api.network.serialization.UnknownFallbackEnumSerializer
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KeepGeneratedSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class DisplayConfig(
  @SerialName("instance_environment_type")
  val instanceEnvironmentType: InstanceEnvironmentType = InstanceEnvironmentType.UNKNOWN,
  @SerialName("application_name") val applicationName: String,
  @SerialName("preferred_sign_in_strategy")
  val preferredSignInStrategy: PreferredSignInStrategy = PreferredSignInStrategy.UNKNOWN,
  @SerialName("support_email") val supportEmail: String? = null,
  @SerialName("show_devmode_warning") val showDevModeWarning: Boolean = false,
  @SerialName("branded") val branded: Boolean,
  @SerialName("logo_image_url") val logoImageUrl: String,
  @SerialName("home_url") val homeUrl: String,
  @SerialName("privacy_policy_url") val privacyPolicyUrl: String?,
  @SerialName("terms_url") val termsUrl: String?,
  @SerialName("google_one_tap_client_id") val googleOneTapClientId: String?,
)

@OptIn(ExperimentalSerializationApi::class)
@KeepGeneratedSerializer
@Serializable(with = PreferredSignInStrategy.Serializer::class)
internal enum class PreferredSignInStrategy {
  @SerialName("password") PASSWORD,
  @SerialName("otp") OTP,
  @SerialName("unknown") UNKNOWN;

  internal object Serializer :
    UnknownFallbackEnumSerializer<PreferredSignInStrategy>(generatedSerializer(), UNKNOWN)
}
