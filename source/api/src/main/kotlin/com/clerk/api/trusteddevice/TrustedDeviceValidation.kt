package com.clerk.api.trusteddevice

import kotlinx.serialization.Serializable

/** The server-side validation result for a local trusted-device sign-in credential. */
@Serializable
internal data class TrustedDeviceValidation(
  /** Whether the trusted-device credential can be used for sign-in. */
  val valid: Boolean
)

/** The outcome of validating a local trusted-device credential against the server. */
sealed interface TrustedDeviceValidationResult {
  /** The local credential is valid on the server. */
  data object Valid : TrustedDeviceValidationResult

  /** The local credential is invalid; stale local state has been cleaned up. */
  data class Invalid(val reason: TrustedDeviceAvailability.UnavailableReason) :
    TrustedDeviceValidationResult

  /** Validation could not be completed; the local credential is kept. */
  data object Inconclusive : TrustedDeviceValidationResult
}
