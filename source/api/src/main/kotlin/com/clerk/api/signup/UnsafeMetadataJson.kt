package com.clerk.api.signup

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

internal fun Map<String, Any>.toUnsafeMetadataJsonString(): String =
  Json.encodeToString(toJsonObject())

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
