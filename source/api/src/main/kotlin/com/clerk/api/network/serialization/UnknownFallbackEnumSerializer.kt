package com.clerk.api.network.serialization

import com.clerk.api.log.ClerkLogger
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

internal open class UnknownFallbackEnumSerializer<E : Enum<E>>(
  private val generated: KSerializer<E>,
  private val fallback: E,
) : KSerializer<E> {

  private val entries: List<E> = requireNotNull(fallback.declaringJavaClass.enumConstants).asList()

  override val descriptor: SerialDescriptor =
    PrimitiveSerialDescriptor(generated.descriptor.serialName, PrimitiveKind.STRING)

  override fun serialize(encoder: Encoder, value: E) {
    encoder.encodeString(generated.descriptor.getElementName(value.ordinal))
  }

  override fun deserialize(decoder: Decoder): E {
    val raw = decoder.decodeString()
    val index = generated.descriptor.getElementIndex(raw)
    if (index == CompositeDecoder.UNKNOWN_NAME) {
      ClerkLogger.w("Unrecognized ${generated.descriptor.serialName} value '$raw'; using $fallback")
      return fallback
    }
    return entries[index]
  }
}
