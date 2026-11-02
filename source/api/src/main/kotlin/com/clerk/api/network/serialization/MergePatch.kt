package com.clerk.api.network.serialization

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject

internal fun computeMergePatch(current: JsonElement, desired: JsonElement): JsonElement {
  if (current !is JsonObject || desired !is JsonObject) {
    return desired
  }

  val changes = desired.mapNotNull { (key, desiredValue) ->
    val valuePatch = patchValue(current[key], desiredValue)
    valuePatch?.let { key to it }
  }
  val deletions = (current.keys - desired.keys).map { it to JsonNull }

  return JsonObject((changes + deletions).toMap())
}

private fun patchValue(current: JsonElement?, desired: JsonElement): JsonElement? {
  return when {
    current == null -> desired
    current == desired -> null
    current is JsonObject && desired is JsonObject ->
      computeMergePatch(current, desired).takeUnless { it is JsonObject && it.isEmpty() }
    else -> desired
  }
}
