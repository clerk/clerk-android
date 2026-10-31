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
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.longOrNull

internal fun toUnsafeMetadataJsonString(metadata: Map<String, Any>): String =
  Json.encodeToString(metadata.toJsonObject())

internal object UnsafeMetadataSerializer : KSerializer<Map<String, Any>> {
  override val descriptor: SerialDescriptor = JsonObject.serializer().descriptor

  override fun serialize(encoder: Encoder, value: Map<String, Any>) {
    encoder.encodeSerializableValue(JsonObject.serializer(), value.toJsonObject())
  }

  override fun deserialize(decoder: Decoder): Map<String, Any> =
    decoder.decodeSerializableValue(JsonObject.serializer()).toPlainMap().filterNotNullValues()
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

private fun JsonObject.toPlainMap(): Map<String, Any?> = mapValues { (_, value) ->
  value.toPlainValue()
}

private fun JsonElement.toPlainValue(): Any? =
  when (this) {
    JsonNull -> null
    is JsonObject -> toPlainMap()
    is JsonArray -> map { it.toPlainValue() }
    is JsonPrimitive ->
      if (isString) content else booleanOrNull ?: longOrNull ?: doubleOrNull ?: content
  }

private fun Map<String, Any?>.filterNotNullValues(): Map<String, Any> =
  entries.mapNotNull { (key, value) -> value?.let { key to it } }.toMap()
