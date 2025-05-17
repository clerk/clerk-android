package com.clerk.sdk.network.converter

import com.clerk.sdk.model.error.ClerkErrorResponse
import com.clerk.sdk.model.error.Error
import com.clerk.sdk.model.error.Meta
import com.clerk.sdk.model.response.ClerkResponse
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody
import retrofit2.Converter
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

/** Retrofit converter factory that handles Clerk API responses. */
class ClerkConverterFactory private constructor(private val json: Json) : Converter.Factory() {

  @OptIn(ExperimentalSerializationApi::class)
  override fun responseBodyConverter(
    type: Type,
    annotations: Array<out Annotation>,
    retrofit: Retrofit,
  ): Converter<ResponseBody, *>? {
    // Check if the type is ClerkResponse<T>
    if (type is ParameterizedType && type.rawType == ClerkResponse::class.java) {

      // Get the success type (T in ClerkResponse<T>)
      val successType = type.actualTypeArguments[0]

      // Use non-generic constructor to avoid type inference issues
      return ClerkResponseConverter(json, successType)
    }

    // For other types, delegate to the kotlinx.serialization converter
    return json
      .asConverterFactory("application/json".toMediaType())
      .responseBodyConverter(type, annotations, retrofit)
  }

  companion object {
    fun create(json: Json = defaultJson): ClerkConverterFactory = ClerkConverterFactory(json)

    private val defaultJson = Json {
      ignoreUnknownKeys = true
      isLenient = true
      coerceInputValues = true
    }
  }
}

/**
 * Converter that handles the parsing of Clerk API responses into ClerkResponse<T>. Made non-generic
 * to avoid type inference issues.
 */
@Suppress("TooGenericExceptionCaught")
@OptIn(ExperimentalSerializationApi::class)
class ClerkResponseConverter(private val json: Json, private val successType: Type) :
  Converter<ResponseBody, ClerkResponse<*>> {

  override fun convert(value: ResponseBody): ClerkResponse<*> {
    val jsonString = value.string()

    // First try to parse as a regular response
    return try {
      // Get serializer for the success type
      val serializer = json.serializersModule.serializer(successType)
      val data = json.decodeFromString(serializer, jsonString)
      ClerkResponse.Success(data)
    } catch (e: Exception) {
      // If parsing as success fails, try as error
      try {
        val errorResponse = json.decodeFromString<ClerkErrorResponse>(jsonString)
        ClerkResponse.Error(errorResponse)
      } catch (e: Exception) {
        // If all parsing fails, create a generic error
        val genericError = createGenericError(e.message ?: "Unknown error")
        ClerkResponse.Error(genericError)
      }
    }
  }

  private fun createGenericError(message: String): ClerkErrorResponse {
    return ClerkErrorResponse(
      errors =
        listOf(
          Error(message = "Failed to parse response", longMessage = message, code = "parsing_error")
        ),
      meta = Meta(client = null),
      clerkTraceId = "",
    )
  }
}
