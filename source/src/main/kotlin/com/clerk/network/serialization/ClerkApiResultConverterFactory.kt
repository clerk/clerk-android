package com.clerk.network.serialization

import com.clerk.log.ClerkLog
import com.clerk.network.model.response.ClientPiggybackedResponse
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import okhttp3.ResponseBody
import retrofit2.Converter
import retrofit2.Retrofit

private const val ENVIRONMENT_TYPE = "com.clerk.network.model.environment.Environment"
private const val TOKEN_TYPE = "com.clerk.network.model.token.TokenResource"

/**  */
internal object ClerkApiResultConverterFactory : Converter.Factory() {
  override fun responseBodyConverter(
    type: Type,
    annotations: Array<out Annotation>,
    retrofit: Retrofit,
  ): Converter<ResponseBody, *>? {

    if (getRawType(type) != ClerkResult::class.java) {
      return null
    }

    val successType = (type as ParameterizedType).actualTypeArguments[0]
    val errorType = type.actualTypeArguments[1]

    val errorResultType: Annotation = createResultType(errorType)
    val nextAnnotations = annotations.toList() + errorResultType

    // For List<Session>, don't wrap in ClientPiggybackedResponse - it comes as plain JSON array
    val rawSuccessType = getRawType(successType)

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

        if (shouldWrap) {
          createParameterizedType(ClientPiggybackedResponse::class.java, successType)
        } else {
          successType
        }
      }

    val delegateConverter =
      retrofit.nextResponseBodyConverter<Any>(
        this,
        actualSuccessType,
        nextAnnotations.toTypedArray(),
      )

    return ClerkApiResultConverter(delegateConverter)
  }

  private fun shouldWrapInClientPiggybackedResponse(successType: Type): Boolean {
    val rawType = getRawType(successType)

    // Don't wrap the Environment type
    if (rawType.name == ENVIRONMENT_TYPE || rawType.name == TOKEN_TYPE) {
      return false
    }

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

      return delegate.convert(value)?.let { result ->
        // If the result is a ClientPiggybackedResponse, unwrap it
        val unwrappedResult =
          if (result is ClientPiggybackedResponse<*>) {
            result.response
          } else {
            result
          }

        @Suppress("UNCHECKED_CAST") ClerkResult.success(unwrappedResult as Any)
      }
        ?: run {
          ClerkLog.e("Delegate converter returned null!")
          null
        }
    }
  }
}
