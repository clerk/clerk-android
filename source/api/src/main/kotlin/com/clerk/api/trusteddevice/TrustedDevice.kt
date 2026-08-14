package com.clerk.api.trusteddevice

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/**
 * A biometric-gated trusted-device credential associated with a user.
 *
 * Trusted devices allow users to sign in with device biometrics instead of their regular first
 * factor. The private key used to sign server challenges never leaves the device.
 *
 * @property id The unique identifier of the trusted-device credential.
 * @property platform The platform this credential belongs to.
 * @property platformRawValue The platform value exactly as returned by the Clerk API.
 * @property appIdentifier The native app identifier this credential is bound to.
 * @property name The user-facing credential name.
 * @property algorithm The signature algorithm used by the credential.
 * @property status The credential status.
 * @property statusRawValue The status value exactly as returned by the Clerk API.
 * @property createdAt The time when the credential was created, in milliseconds since epoch.
 * @property updatedAt The time when the credential was last updated, in milliseconds since epoch.
 * @property lastUsedAt The time when the credential was last used, in milliseconds since epoch.
 * @property revokedAt The time when the credential was revoked, in milliseconds since epoch.
 */
@Serializable(with = TrustedDeviceSerializer::class)
@ConsistentCopyVisibility
data class TrustedDevice
private constructor(
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

  /** The platform value exactly as returned by the Clerk API. */
  val platformRawValue: String,

  /** The status value exactly as returned by the Clerk API. */
  val statusRawValue: String,
) {

  constructor(
    id: String,
    platform: Platform = Platform.UNKNOWN,
    appIdentifier: String,
    name: String? = null,
    algorithm: String = ES256_ALGORITHM,
    status: Status = Status.UNKNOWN,
    createdAt: Long,
    updatedAt: Long,
    lastUsedAt: Long? = null,
    revokedAt: Long? = null,
  ) : this(
    id = id,
    platform = platform,
    appIdentifier = appIdentifier,
    name = name,
    algorithm = algorithm,
    status = status,
    createdAt = createdAt,
    updatedAt = updatedAt,
    lastUsedAt = lastUsedAt,
    revokedAt = revokedAt,
    platformRawValue = platform.serializedValue,
    statusRawValue = status.serializedValue,
  )

  fun copy(
    id: String = this.id,
    platform: Platform = this.platform,
    appIdentifier: String = this.appIdentifier,
    name: String? = this.name,
    algorithm: String = this.algorithm,
    status: Status = this.status,
    createdAt: Long = this.createdAt,
    updatedAt: Long = this.updatedAt,
    lastUsedAt: Long? = this.lastUsedAt,
    revokedAt: Long? = this.revokedAt,
  ): TrustedDevice =
    TrustedDevice(
      id = id,
      platform = platform,
      appIdentifier = appIdentifier,
      name = name,
      algorithm = algorithm,
      status = status,
      createdAt = createdAt,
      updatedAt = updatedAt,
      lastUsedAt = lastUsedAt,
      revokedAt = revokedAt,
      platformRawValue =
        if (platform == this.platform) platformRawValue else platform.serializedValue,
      statusRawValue = if (status == this.status) statusRawValue else status.serializedValue,
    )

  /** The platform a trusted-device credential belongs to. */
  @Serializable
  enum class Platform {
    @SerialName("ios") IOS,
    @SerialName("android") ANDROID,
    UNKNOWN;

    internal val serializedValue: String
      get() =
        when (this) {
          IOS -> "ios"
          ANDROID -> "android"
          UNKNOWN -> "unknown"
        }

    internal companion object {
      fun fromSerializedValue(value: String): Platform =
        when (value) {
          "ios" -> IOS
          "android" -> ANDROID
          else -> UNKNOWN
        }
    }
  }

  /** The server-side trusted-device credential status. */
  @Serializable
  enum class Status {
    @SerialName("active") ACTIVE,
    @SerialName("revoked") REVOKED,
    UNKNOWN;

    internal val serializedValue: String
      get() =
        when (this) {
          ACTIVE -> "active"
          REVOKED -> "revoked"
          UNKNOWN -> "unknown"
        }

    internal companion object {
      fun fromSerializedValue(value: String): Status =
        when (value) {
          "active" -> ACTIVE
          "revoked" -> REVOKED
          else -> UNKNOWN
        }
    }
  }

  companion object {
    /** The signature algorithm used by trusted-device credentials on Android. */
    const val ES256_ALGORITHM: String = "ES256"

    internal fun fromPayload(payload: TrustedDevicePayload): TrustedDevice =
      TrustedDevice(
        id = payload.id,
        platform = Platform.fromSerializedValue(payload.platform),
        appIdentifier = payload.appIdentifier,
        name = payload.name,
        algorithm = payload.algorithm,
        status = Status.fromSerializedValue(payload.status),
        createdAt = payload.createdAt,
        updatedAt = payload.updatedAt,
        lastUsedAt = payload.lastUsedAt,
        revokedAt = payload.revokedAt,
        platformRawValue = payload.platform,
        statusRawValue = payload.status,
      )
  }
}

@Serializable
internal data class TrustedDevicePayload(
  val id: String,
  val platform: String = "unknown",
  @SerialName("app_identifier") val appIdentifier: String,
  val name: String? = null,
  val algorithm: String = TrustedDevice.ES256_ALGORITHM,
  val status: String = "unknown",
  @SerialName("created_at") val createdAt: Long,
  @SerialName("updated_at") val updatedAt: Long,
  @SerialName("last_used_at") val lastUsedAt: Long? = null,
  @SerialName("revoked_at") val revokedAt: Long? = null,
)

internal object TrustedDeviceSerializer : KSerializer<TrustedDevice> {
  override val descriptor: SerialDescriptor =
    SerialDescriptor(
      "com.clerk.api.trusteddevice.TrustedDevice",
      TrustedDevicePayload.serializer().descriptor,
    )

  override fun serialize(encoder: Encoder, value: TrustedDevice) {
    encoder.encodeSerializableValue(
      TrustedDevicePayload.serializer(),
      TrustedDevicePayload(
        id = value.id,
        platform = value.platformRawValue,
        appIdentifier = value.appIdentifier,
        name = value.name,
        algorithm = value.algorithm,
        status = value.statusRawValue,
        createdAt = value.createdAt,
        updatedAt = value.updatedAt,
        lastUsedAt = value.lastUsedAt,
        revokedAt = value.revokedAt,
      ),
    )
  }

  override fun deserialize(decoder: Decoder): TrustedDevice {
    val payload = decoder.decodeSerializableValue(TrustedDevicePayload.serializer())
    return TrustedDevice.fromPayload(payload)
  }
}
