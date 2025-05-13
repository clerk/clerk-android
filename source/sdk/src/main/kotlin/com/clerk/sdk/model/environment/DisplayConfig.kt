package com.clerk.sdk.model.environment

import kotlinx.serialization.Serializable

/** Configuration for display settings in the Clerk environment. */
@Serializable
data class DisplayConfig(
  val instanceEnvironmentType: InstanceEnvironmentType,
  val applicationName: String,
  val preferredSignInStrategy: PreferredSignInStrategy,
  val branded: Boolean,
  val logoImageUrl: String,
  val homeUrl: String,
  val privacyPolicyUrl: String? = null,
  val termsUrl: String? = null,
) {
  /** Enum representing the preferred sign-in strategy. */
  @Serializable
  enum class PreferredSignInStrategy {
    PASSWORD,
    OTP,
    UNKNOWN,
  }
}
