package com.clerk.api.trusteddevice

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * A server challenge for trusted-device enrollment or sign-in.
 *
 * The [clientData] string must be signed with the local trusted-device private key and returned to
 * the server exactly as received.
 *
 * @property challenge The challenge value.
 * @property challengeId The unique identifier of the challenge.
 * @property trustedDeviceId The trusted-device credential ID for sign-in challenges.
 * @property clientData The exact client data string that must be signed.
 * @property expiresAt The time when the challenge expires, in milliseconds since epoch.
 * @property algorithm The signature algorithm required for the challenge.
 */
@Serializable
data class TrustedDeviceChallenge(
  /** The challenge value. */
  val challenge: String,

  /** The unique identifier of the challenge. */
  @SerialName("challenge_id") val challengeId: String,

  /** The trusted-device credential ID for sign-in challenges. */
  @SerialName("trusted_device_id") val trustedDeviceId: String? = null,

  /** The exact client data string that must be signed. */
  @SerialName("client_data") val clientData: String,

  /** The time when the challenge expires, in milliseconds since epoch. */
  @SerialName("expires_at") val expiresAt: Long,

  /** The signature algorithm required for the challenge. */
  val algorithm: String = TrustedDevice.ES256_ALGORITHM,
)
