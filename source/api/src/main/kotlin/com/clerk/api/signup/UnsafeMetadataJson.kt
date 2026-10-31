package com.clerk.api.signup

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encodeToString
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

internal fun toUnsafeMetadataJsonString(metadata: Map<String, Any>): String =
  Json.encodeToString(metadata.toJsonObject())

internal object UnsafeMetadataSerializer : KSerializer<Map<String, Any>> {
  override val descriptor: SerialDescriptor = JsonObject.serializer().descriptor

  override fun serialize(encoder: Encoder, value: Map<String, Any>) {
    encoder.encodeSerializableValue(JsonObject.serializer(), value.toJsonObject())
  }

  override fun deserialize(decoder: Decoder): Map<String, Any> =
    decoder.decodeSerializableValue(JsonObject.serializer())
}

private fun Map<*, *>.toJsonObject(): JsonObject =
  JsonObject(entries.associate { entry -> entry.key.toString() to entry.value.toJsonElement() })

private fun Any?.toJsonElement(): JsonElement =
  when (this) {
    null -> JsonNull
    is JsonElement -> this
    is String -> JsonPrimitive(this)
    is Boolean -> JsonPrimitive(this)
    is Number -> JsonPrimitive(this)
    is Map<*, *> -> toJsonObject()
    is Iterable<*> -> JsonArray(map { it.toJsonElement() })
    is Array<*> -> JsonArray(map { it.toJsonElement() })
    else -> JsonPrimitive(toString())
  }
