package com.cerk.clerkserializer

import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import okhttp3.ResponseBody
import retrofit2.Converter
import retrofit2.Retrofit

public object ApiResultConverterFactory : Converter.Factory() {
  override fun responseBodyConverter(
    type: Type,
    annotations: Array<Annotation>,
    retrofit: Retrofit,
  ): Converter<ResponseBody, *>? {
    if (getRawType(type) != ClerkApiResult::class.java) return null

    val successType = (type as ParameterizedType).actualTypeArguments[0]
    val errorType = type.actualTypeArguments[1]
    val errorResultType: Annotation = createResultType(errorType)
    val nextAnnotations = annotations + errorResultType
    val delegateConverter =
      retrofit.nextResponseBodyConverter<Any>(this, successType, nextAnnotations)
    return ApiResultConverter(delegateConverter)
  }

  private class ApiResultConverter(private val delegate: Converter<ResponseBody, Any>) :
    Converter<ResponseBody, ClerkApiResult<*, *>> {
    override fun convert(value: ResponseBody): ClerkApiResult<*, *>? {
      return delegate.convert(value)?.let(ClerkApiResult.Companion::success)
    }
  }
}
