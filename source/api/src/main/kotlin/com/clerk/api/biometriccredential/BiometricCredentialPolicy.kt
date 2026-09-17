package com.clerk.api.biometriccredential

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** The local authentication policy used to protect a biometric-credential private key. */
@Serializable
enum class BiometricCredentialPolicy {
  /**
   * Require a Class 3 (strong) biometric from the currently enrolled set.
   *
   * Adding a new biometric or removing all biometrics invalidates the private key. This is the
   * default enrollment policy and the only policy supported for session reverification.
   */
  @SerialName("biometry_current_set") BIOMETRY_CURRENT_SET,

  /**
   * Require Class 3 (strong) biometric authentication, but allow enrollment changes.
   *
   * Supported for sign-in only; session reverification requires [BIOMETRY_CURRENT_SET].
   */
  @SerialName("biometry_any") BIOMETRY_ANY,

  /**
   * Require biometric availability, then allow biometric or device credential (PIN, pattern, or
   * password) authentication.
   *
   * On devices running Android 10 (API 29) and below, key access is biometric-only because
   * device-credential fallback for key operations requires Android 11 (API 30). Supported for
   * sign-in only; session reverification requires [BIOMETRY_CURRENT_SET].
   */
  @SerialName("biometry_or_device_passcode") BIOMETRY_OR_DEVICE_PASSCODE,
}
