package com.clerk.sdk.model.environment

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Settings for fraud prevention in the Clerk environment. */
@Serializable
data class FraudSettings(val native: Native) {
  /** Native platform specific fraud prevention settings. */
  @Serializable data class Native(val deviceAttestationMode: DeviceAttestationMode)

  /** Enum representing the device attestation mode. */
  @Serializable
  enum class DeviceAttestationMode {
    @SerialName("disabled") DISABLED,
    @SerialName("onboarindg") ONBOARDING,
    @SerialName("enforced") ENFORCED,
    @SerialName("unknown") UNKNOWN,
  }
}
