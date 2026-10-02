package com.clerk.api.network.serialization

import com.clerk.api.log.ClerkLog
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/**
 * The single policy for enums decoded from Clerk API responses: a value this SDK version does not
 * recognize decodes to the enum's `UNKNOWN` entry instead of failing the whole response.
 *
 * Every server enum is declared as
 *
 * ```kotlin
 * @OptIn(ExperimentalSerializationApi::class)
 * @KeepGeneratedSerializer
 * @Serializable(with = Status.Serializer::class)
 * enum class Status {
 *   @SerialName("active") ACTIVE,
 *   @SerialName("unknown") UNKNOWN;
 *
 *   internal object Serializer :
 *     UnknownFallbackEnumSerializer<Status>(generatedSerializer(), UNKNOWN)
 * }
 * ```
 *
 * The wire names come from the plugin-generated serializer, so `@SerialName` stays the only source
 * of truth for each entry. Unlike `coerceInputValues`, the fallback does not depend on the [Json]
 * configuration or on the property having a default, so it also covers nullable properties, lists
 * and maps of enums, and standalone decoding.
 *
 * @param generated The plugin-generated serializer of [E] (from `@KeepGeneratedSerializer`).
 * @param fallback The entry returned for unrecognized values.
 */
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
      ClerkLog.w("Unrecognized ${generated.descriptor.serialName} value '$raw'; using $fallback")
      return fallback
    }
    return entries[index]
  }
}
