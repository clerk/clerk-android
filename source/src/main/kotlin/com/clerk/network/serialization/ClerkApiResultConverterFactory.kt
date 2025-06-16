package com.clerk.network.serialization

import com.clerk.log.ClerkLog
import com.clerk.network.model.response.ClientPiggybackedResponse
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import okhttp3.ResponseBody
import retrofit2.Converter
import retrofit2.Retrofit

private const val ENVIRONMENT_TYPE = "com.clerk.network.model.environment.Environment"

/**  */
internal object ClerkApiResultConverterFactory : Converter.Factory() {
  override fun responseBodyConverter(
    type: Type,
    annotations: Array<out Annotation>,
    retrofit: Retrofit,
  ): Converter<ResponseBody, *>? {
    ClerkLog.d("ClerkApiResultConverterFactory.responseBodyConverter called with type: $type")

    if (getRawType(type) != ClerkResult::class.java) {
      ClerkLog.d("Type is not ClerkResult, returning null")
      return null
    }

    ClerkLog.d("Type is ClerkResult, proceeding with conversion")

    val successType = (type as ParameterizedType).actualTypeArguments[0]
    val errorType = type.actualTypeArguments[1]

    ClerkLog.d("Success type: $successType")
    ClerkLog.d("Error type: $errorType")

    val errorResultType: Annotation = createResultType(errorType)
    val nextAnnotations = annotations.toList() + errorResultType

    // For List<Session>, don't wrap in ClientPiggybackedResponse - it comes as plain JSON array
    val rawSuccessType = getRawType(successType)
    ClerkLog.d("Raw success type: $rawSuccessType")

    val isListType =
      rawSuccessType == List::class.java ||
        (successType.toString().contains("java.util.List") &&
          successType.toString().contains("Session"))

    val actualSuccessType =
      if (isListType) {
        ClerkLog.d(
          "This is a List<Session> type, using direct type (no ClientPiggybackedResponse wrapper)"
        )
        successType
      } else {
        // Check if the success type should be wrapped in ClientPiggybackedResponse
        val shouldWrap = shouldWrapInClientPiggybackedResponse(successType)
        ClerkLog.d("Should wrap in ClientPiggybackedResponse: $shouldWrap")

        if (shouldWrap) {
          createParameterizedType(ClientPiggybackedResponse::class.java, successType)
        } else {
          successType
        }
      }

    ClerkLog.d("Actual success type for conversion: $actualSuccessType")

    val delegateConverter =
      retrofit.nextResponseBodyConverter<Any>(
        this,
        actualSuccessType,
        nextAnnotations.toTypedArray(),
      )

    ClerkLog.d("Delegate converter: $delegateConverter")
    return ClerkApiResultConverter(delegateConverter)
  }

  private fun shouldWrapInClientPiggybackedResponse(successType: Type): Boolean {
    val rawType = getRawType(successType)

    // Don't wrap the Environment type
    if (rawType.name == ENVIRONMENT_TYPE) {
      ClerkLog.d("Not wrapping Environment type in ClientPiggybackedResponse")
      return false
    }

    ClerkLog.d("Wrapping type in ClientPiggybackedResponse")
    return true
  }

  private fun createParameterizedType(rawType: Class<*>, typeArgument: Type): ParameterizedType {
    return object : ParameterizedType {
      override fun getRawType(): Type = rawType

      override fun getActualTypeArguments(): Array<Type> = arrayOf(typeArgument)

      override fun getOwnerType(): Type? = null
    }
  }

  private class ClerkApiResultConverter(private val delegate: Converter<ResponseBody, Any>) :
    Converter<ResponseBody, ClerkResult<*, *>> {
    override fun convert(value: ResponseBody): ClerkResult<*, *>? {
      ClerkLog.d("ClerkApiResultConverter.convert called")

      return delegate.convert(value)?.let { result ->
        ClerkLog.d("Delegate converter returned: $result")

        // If the result is a ClientPiggybackedResponse, unwrap it
        val unwrappedResult =
          if (result is ClientPiggybackedResponse<*>) {
            ClerkLog.d("Unwrapping ClientPiggybackedResponse")
            result.response
          } else {
            ClerkLog.d("Result is not ClientPiggybackedResponse, using as-is")
            result
          }

        ClerkLog.d("Final unwrapped result: $unwrappedResult")
        @Suppress("UNCHECKED_CAST") ClerkResult.success(unwrappedResult as Any)
      }
        ?: run {
          ClerkLog.e("Delegate converter returned null!")
          null
        }
    }
  }
}
