package com.clerk.api.biometriccredential

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * A server challenge for biometric-credential enrollment or sign-in.
 *
 * The [clientData] string must be signed with the local biometric-credential private key and
 * returned to the server exactly as received.
 *
 * @property challenge The challenge value.
 * @property challengeId The unique identifier of the challenge.
 * @property biometricCredentialId The biometric credential ID for sign-in challenges.
 * @property clientData The exact client data string that must be signed.
 * @property expiresAt The time when the challenge expires, in milliseconds since epoch.
 * @property algorithm The signature algorithm required for the challenge.
 */
@Serializable
data class BiometricCredentialChallenge(
  /** The challenge value. */
  val challenge: String,

  /** The unique identifier of the challenge. */
  @SerialName("challenge_id") val challengeId: String,

  /** The biometric credential ID for sign-in challenges. */
  @SerialName("trusted_device_id") val biometricCredentialId: String? = null,

  /** The exact client data string that must be signed. */
  @SerialName("client_data") val clientData: String,

  /** The time when the challenge expires, in milliseconds since epoch. */
  @SerialName("expires_at") val expiresAt: Long,

  /** The signature algorithm required for the challenge. */
  val algorithm: String = BiometricCredential.ES256_ALGORITHM,
)
