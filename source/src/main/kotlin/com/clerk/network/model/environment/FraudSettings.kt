package com.clerk.network.model.environment

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Settings for fraud prevention in the Clerk environment. */
@Serializable
data class FraudSettings(val native: Native) {
  /** Native platform specific fraud prevention settings. */
  @Serializable
  data class Native(
    @SerialName("device_attestation_mode") val deviceAttestationMode: DeviceAttestationMode
  )

  /** Enum representing the device attestation mode. */
  @Serializable
  enum class DeviceAttestationMode {
    @SerialName("disabled") DISABLED,
    @SerialName("onboarding") ONBOARDING,
    @SerialName("enforced") ENFORCED,
    @SerialName("unknown") UNKNOWN,
  }
}
