package com.clerk.api.trusteddevice

/** Local availability state for biometric trusted-device sign-in. */
sealed interface TrustedDeviceAvailability {

  /** Trusted-device sign-in can be started with a local credential on this device. */
  data object Available : TrustedDeviceAvailability

  /** Trusted-device sign-in is unavailable for the given [reason]. */
  data class Unavailable(val reason: UnavailableReason) : TrustedDeviceAvailability

  /** Whether the SDK has a local credential and key that can be used for trusted-device sign-in. */
  val isAvailable: Boolean
    get() = this is Available

  /** The reason trusted-device sign-in is unavailable, if any. */
  val unavailableReason: UnavailableReason?
    get() = (this as? Unavailable)?.reason

  /** The reason trusted-device sign-in is unavailable. */
  enum class UnavailableReason {
    /** The Clerk environment has not been loaded yet. */
    ENVIRONMENT_UNAVAILABLE,

    /** The Clerk Native API is disabled for this instance. */
    NATIVE_API_DISABLED,

    /** Trusted-device sign-in is disabled for this instance. */
    FEATURE_DISABLED,

    /** The device or OS version does not support trusted-device sign-in. */
    UNSUPPORTED_PLATFORM,

    /** Biometric authentication is not available or not enrolled on this device. */
    BIOMETRIC_AUTHENTICATION_UNAVAILABLE,

    /** No trusted-device credential is stored on this device. */
    NO_LOCAL_CREDENTIAL,

    /** The local private key backing the stored credential is missing. */
    LOCAL_KEY_MISSING,

    /** The server no longer has a matching trusted-device credential. */
    SERVER_CREDENTIAL_MISSING,

    /** The server-side trusted-device credential has been revoked. */
    SERVER_CREDENTIAL_REVOKED,
  }
}
