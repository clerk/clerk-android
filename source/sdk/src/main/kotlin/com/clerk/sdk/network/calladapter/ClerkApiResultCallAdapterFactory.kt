package com.clerk.sdk.network.calladapter

import com.clerk.sdk.network.ClerkApiResult
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import retrofit2.Call
import retrofit2.CallAdapter
import retrofit2.Retrofit

@Suppress("ReturnCount")
class ClerkApiResultCallAdapterFactory : CallAdapter.Factory() {
  override fun get(
    returnType: Type,
    annotations: Array<out Annotation>,
    retrofit: Retrofit,
  ): CallAdapter<*, *>? {
    // Check if the return type is Call
    if (getRawType(returnType) != Call::class.java) {
      return null
    }

    // Get the parameter type of Call<T>
    check(returnType is ParameterizedType) { "Call return type must be parameterized" }
    val responseType = getParameterUpperBound(0, returnType)

    // Check if the Call's parameter type is ClerkApiResult
    if (getRawType(responseType) != ClerkApiResult::class.java) {
      return null
    }

    // Get the parameter types of ClerkApiResult<T, E>
    check(responseType is ParameterizedType) {
      "Response type must be parameterized as ClerkApiResult<T, E>"
    }
    val successType = getParameterUpperBound(0, responseType)
    val errorType = getParameterUpperBound(1, responseType)

    // Get error converter
    val errorBodyConverter = retrofit.nextResponseBodyConverter<Any>(null, errorType, annotations)

    return ClerkApiResultCallAdapter<Any, Any>(successType, errorBodyConverter)
  }
}
