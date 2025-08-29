package com.clerk.api.network.serialization

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/**
 * A custom enum serializer that provides fallback behavior for unknown enum values.
 * 
 * When deserializing an enum value that doesn't match any known enum constants,
 * this serializer will fall back to the "UNKNOWN" enum value instead of throwing
 * a SerializationException.
 * 
 * This is particularly useful for API responses where new enum values might be
 * introduced server-side before the client code is updated.
 *
 * @param T The enum type to serialize/deserialize
 * @param serialName The name used for serialization
 * @param unknownValue The fallback value to use for unknown enum constants
 */
class FallbackEnumSerializer<T : Enum<T>>(
  private val serialName: String,
  private val unknownValue: T,
  private val enumValues: Array<T>
) : KSerializer<T> {

  override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor(serialName, PrimitiveKind.STRING)

  override fun serialize(encoder: Encoder, value: T) {
    // For serialization, we need to use the actual @SerialName values or enum name
    val serialValue = getSerialName(value) ?: value.name.lowercase()
    encoder.encodeString(serialValue)
  }

  override fun deserialize(decoder: Decoder): T {
    val enumString = decoder.decodeString()
    return try {
      // Try to find a matching enum value by @SerialName first, then by name
      enumValues.find { 
        val serialName = getSerialName(it)
        serialName == enumString || 
        it.name.equals(enumString, ignoreCase = true)
      } ?: unknownValue
    } catch (e: Exception) {
      // If anything goes wrong, return the fallback
      unknownValue
    }
  }

  /**
   * Get the @SerialName value for an enum constant if it exists.
   */
  private fun getSerialName(enumValue: T): String? {
    return try {
      val field = enumValue.javaClass.getField(enumValue.name)
      val annotation = field.getAnnotation(kotlinx.serialization.SerialName::class.java)
      annotation?.value
    } catch (e: Exception) {
      null
    }
  }
}

/**
 * Creates a fallback enum serializer for the given enum class.
 * 
 * This function automatically detects the "UNKNOWN" enum value and uses it
 * as the fallback for unknown values during deserialization.
 * 
 * @param T The enum type
 * @param serialName The name to use for serialization
 * @param enumValues All enum values
 * @return A FallbackEnumSerializer configured with UNKNOWN as the fallback
 * @throws IllegalArgumentException if no UNKNOWN enum value is found
 */
inline fun <reified T : Enum<T>> createFallbackEnumSerializer(
  serialName: String,
  enumValues: Array<T>
): FallbackEnumSerializer<T> {
  val unknownValue = enumValues.find { it.name == "UNKNOWN" }
    ?: throw IllegalArgumentException("Enum $serialName must have an UNKNOWN value for fallback behavior")
  
  return FallbackEnumSerializer(serialName, unknownValue, enumValues)
}