package com.clerk.api.trusteddevice

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * A biometric-gated trusted-device credential associated with a user.
 *
 * Trusted devices allow users to sign in with device biometrics instead of their regular first
 * factor. The private key used to sign server challenges never leaves the device.
 *
 * @property id The unique identifier of the trusted-device credential.
 * @property platform The platform this credential belongs to.
 * @property appIdentifier The native app identifier this credential is bound to.
 * @property name The user-facing credential name.
 * @property algorithm The signature algorithm used by the credential.
 * @property status The credential status.
 * @property createdAt The time when the credential was created, in milliseconds since epoch.
 * @property updatedAt The time when the credential was last updated, in milliseconds since epoch.
 * @property lastUsedAt The time when the credential was last used, in milliseconds since epoch.
 * @property revokedAt The time when the credential was revoked, in milliseconds since epoch.
 */
@Serializable
data class TrustedDevice(
  /** The unique identifier of the trusted-device credential. */
  val id: String,

  /** The platform this credential belongs to. */
  val platform: Platform = Platform.UNKNOWN,

  /** The native app identifier this credential is bound to. */
  @SerialName("app_identifier") val appIdentifier: String,

  /** The user-facing credential name. */
  val name: String? = null,

  /** The signature algorithm used by the credential. */
  val algorithm: String = ES256_ALGORITHM,

  /** The credential status. */
  val status: Status = Status.UNKNOWN,

  /** The time when the credential was created, in milliseconds since epoch. */
  @SerialName("created_at") val createdAt: Long,

  /** The time when the credential was last updated, in milliseconds since epoch. */
  @SerialName("updated_at") val updatedAt: Long,

  /** The time when the credential was last used, in milliseconds since epoch. */
  @SerialName("last_used_at") val lastUsedAt: Long? = null,

  /** The time when the credential was revoked, in milliseconds since epoch. */
  @SerialName("revoked_at") val revokedAt: Long? = null,
) {

  /** The platform a trusted-device credential belongs to. */
  @Serializable
  enum class Platform {
    @SerialName("ios") IOS,
    @SerialName("android") ANDROID,
    UNKNOWN,
  }

  /** The server-side trusted-device credential status. */
  @Serializable
  enum class Status {
    @SerialName("active") ACTIVE,
    @SerialName("revoked") REVOKED,
    UNKNOWN,
  }

  companion object {
    /** The signature algorithm used by trusted-device credentials on Android. */
    const val ES256_ALGORITHM: String = "ES256"
  }
}
