package com.clerk.sdk.network.encoding

import java.lang.reflect.Type
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import okhttp3.FormBody
import okhttp3.RequestBody
import retrofit2.Converter
import retrofit2.Retrofit
import retrofit2.http.FormUrlEncoded

/**
 * A [Converter.Factory] that converts a Kotlin object to a [RequestBody] using the form URL encoded
 * format. This is used because the Clerk API does not support JSON in the request body.
 *
 * This factory is used when the request body is annotated with [FormEncoded].
 *
 * @see FormEncoded
 */
class FormUrlEncodedConverterFactory : Converter.Factory() {
  override fun requestBodyConverter(
    type: Type,
    parameterAnnotations: Array<Annotation>,
    methodAnnotations: Array<Annotation>,
    retrofit: Retrofit,
  ): Converter<*, RequestBody>? {
    if (parameterAnnotations.none { it is FormUrlEncoded }) return null

    return Converter<Any, RequestBody> { value ->
      val fieldMap = value.toFormMap()
      FormBody.Builder().apply { fieldMap.forEach { (key, value) -> add(key, value) } }.build()
    }
  }
}

fun Any.toFormMap(): Map<String, String> {
  val json = Json { encodeDefaults = false }
  val jsonString = json.encodeToString(this)
  val map = json.decodeFromString<Map<String, JsonElement>>(jsonString)

  return map.mapValues { (_, value) ->
    when (value) {
      is JsonPrimitive -> value.toString().removeSurrounding("\"")
      else -> value.toString()
    }
  }
}
