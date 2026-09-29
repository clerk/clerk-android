package com.clerk.api.biometriccredential

import kotlinx.serialization.Serializable

@Serializable
internal data class BiometricCredentialValidation(
  val valid: Boolean
)

/** The outcome of validating a local biometric credential against the server. */
sealed interface BiometricCredentialValidationResult {
  /** The local credential is valid on the server. */
  data object Valid : BiometricCredentialValidationResult

  /** The local credential is invalid; stale local state has been cleaned up. */
  data class Invalid(val reason: BiometricCredentialAvailability.UnavailableReason) :
    BiometricCredentialValidationResult

  /** Validation could not be completed; the local credential is kept. */
  data object Inconclusive : BiometricCredentialValidationResult
}
