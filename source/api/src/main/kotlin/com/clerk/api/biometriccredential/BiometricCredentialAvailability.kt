package com.clerk.api.biometriccredential

/** Local availability state for biometric-credential sign-in. */
sealed interface BiometricCredentialAvailability {

  /** Biometric-credential sign-in can be started with a local credential on this device. */
  data object Available : BiometricCredentialAvailability

  /** Biometric-credential sign-in is unavailable for the given [reason]. */
  data class Unavailable(val reason: UnavailableReason) : BiometricCredentialAvailability

  /**
   * Whether the SDK has a local credential and key that can be used for biometric-credential
   * sign-in.
   */
  val isAvailable: Boolean
    get() = this is Available

  /** The reason biometric-credential sign-in is unavailable, if any. */
  val unavailableReason: UnavailableReason?
    get() = (this as? Unavailable)?.reason

  /** The reason biometric-credential sign-in is unavailable. */
  enum class UnavailableReason {
    /** The Clerk environment has not been loaded yet. */
    ENVIRONMENT_UNAVAILABLE,

    /** The Clerk Native API is disabled for this instance. */
    NATIVE_API_DISABLED,

    /** Biometric-credential sign-in is disabled for this instance. */
    FEATURE_DISABLED,

    /** Biometric authentication is not available or not enrolled on this device. */
    BIOMETRIC_AUTHENTICATION_UNAVAILABLE,

    /** No biometric credential is stored on this device. */
    NO_LOCAL_CREDENTIAL,

    /** The local private key backing the stored credential is missing. */
    LOCAL_KEY_MISSING,

    /** The server no longer has a matching biometric credential. */
    SERVER_CREDENTIAL_MISSING,

    /** The server-side biometric credential has been revoked. */
    SERVER_CREDENTIAL_REVOKED,
  }
}
