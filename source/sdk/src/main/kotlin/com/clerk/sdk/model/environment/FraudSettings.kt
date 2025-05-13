package com.clerk.sdk.model.environment

import kotlinx.serialization.Serializable

/** Settings for fraud prevention in the Clerk environment. */
@Serializable
data class FraudSettings(val native: Native) {
  /** Native platform specific fraud prevention settings. */
  @Serializable data class Native(val deviceAttestationMode: DeviceAttestationMode)

  /** Enum representing the device attestation mode. */
  @Serializable
  enum class DeviceAttestationMode {
    DISABLED,
    ONBOARDING,
    ENFORCED,
    UNKNOWN,
  }
}
