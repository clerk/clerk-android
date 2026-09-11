package com.clerk.api.protect

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonObject

/**
 * Opaque Protect challenge data returned by the Clerk API.
 *
 * The payload is intentionally preserved without interpreting its fields so newer Protect
 * challenges remain compatible with older Clerk Android SDK versions.
 */
@Serializable(with = ProtectCheckResourceSerializer::class)
class ProtectCheckResource internal constructor(internal val raw: JsonObject) {
  override fun equals(other: Any?): Boolean =
    this === other || (other is ProtectCheckResource && raw == other.raw)

  override fun hashCode(): Int = raw.hashCode()

  override fun toString(): String = "ProtectCheckResource([REDACTED])"
}

internal object ProtectCheckResourceSerializer : KSerializer<ProtectCheckResource> {
  override val descriptor: SerialDescriptor =
    SerialDescriptor(
      "com.clerk.api.protect.ProtectCheckResource",
      JsonObject.serializer().descriptor,
    )

  override fun serialize(encoder: Encoder, value: ProtectCheckResource) {
    encoder.encodeSerializableValue(JsonObject.serializer(), value.raw)
  }

  override fun deserialize(decoder: Decoder): ProtectCheckResource =
    ProtectCheckResource(decoder.decodeSerializableValue(JsonObject.serializer()))
}
