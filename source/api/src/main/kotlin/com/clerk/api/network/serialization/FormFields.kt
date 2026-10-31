package com.clerk.api.network.serialization

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.json.ClassDiscriminatorMode
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.serializer

@OptIn(ExperimentalSerializationApi::class)
private val formJson = Json {
  encodeDefaults = true
  explicitNulls = false
  classDiscriminatorMode = ClassDiscriminatorMode.NONE
}

internal inline fun <reified T> T.toFormFields(): Map<String, String> = toFormFields(serializer())

// Form endpoints reject bracket-encoded nested params (unsafe_metadata[key]=value), so nested
// values are sent as JSON strings.
internal fun <T> T.toFormFields(serializer: SerializationStrategy<T>): Map<String, String> =
  formJson.encodeToJsonElement(serializer, this).jsonObject.mapValues { (_, value) ->
    if (value is JsonPrimitive) value.content else value.toString()
  }
