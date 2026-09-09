package com.clerk.api

import kotlinx.serialization.json.*

internal val Undefined: JsonElement = buildJsonObject { put("\$undefined", true) }
internal fun JsonElement.requireString(): String = (this as? JsonPrimitive)?.takeIf { it.isString }?.content ?: throw CoreException("invalid_value")
internal fun JsonElement.requireDouble(): Double = (this as? JsonPrimitive)?.takeIf { !it.isString }?.doubleOrNull?.takeIf { it.isFinite() } ?: throw CoreException("invalid_value")
internal fun JsonElement.requireBoolean(): Boolean = (this as? JsonPrimitive)?.takeIf { !it.isString }?.booleanOrNull ?: throw CoreException("invalid_value")
internal fun JsonElement.requireLiteral(expected: JsonElement): JsonElement { if (this != expected) throw CoreException("invalid_value"); return this }
internal inline fun <T> JsonElement.decodeOptional(decode: (JsonElement) -> T): T? = if (this == JsonNull || this == Undefined) null else decode(this)
internal fun JsonObjectBuilder.putPresent(name: String, value: JsonElement) { if (value != Undefined) put(name, value) }

public sealed interface Field<out T> {
  public data object Omitted : Field<Nothing>
  public data object Null : Field<Nothing>
  public data class Value<T>(public val value: T) : Field<T>
  public fun toJson(encode: (@UnsafeVariance T) -> JsonElement): JsonElement = when (this) {
    Omitted -> Undefined
    Null -> JsonNull
    is Value -> encode(value)
  }
  public companion object {
    public fun <T> fromJson(value: JsonElement, decode: (JsonElement) -> T): Field<T> = when (value) {
      Undefined -> Omitted
      JsonNull -> Null
      else -> Value(decode(value))
    }
  }
}

public class UploadFile(public val name: String, public val contentType: String, data: ByteArray) {
  public val data: ByteArray = data.copyOf()
  public fun toJson(): JsonElement = buildJsonObject {
    put("name", name); put("contentType", contentType); put("base64", java.util.Base64.getEncoder().encodeToString(data))
  }
  public companion object {
    public fun fromJson(value: JsonElement): UploadFile {
      val v = value.jsonObject
      return UploadFile(v.getValue("name").requireString(), v.getValue("contentType").requireString(), java.util.Base64.getDecoder().decode(v.getValue("base64").requireString()))
    }
  }
}
