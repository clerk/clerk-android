package com.clerk.network.serialization

import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import kotlin.reflect.KClass
import okhttp3.Request
import retrofit2.Call
import retrofit2.CallAdapter
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit

/**
 * A custom [CallAdapter.Factory] for [ClerkResult] calls. This creates a delegating adapter for
 * suspend function calls that return [ClerkResult]. This facilitates returning all error types
 * through a single [ClerkResult.Failure] type.
 */
object ClerkApiResultCallAdapterFactory : CallAdapter.Factory() {
  @Suppress("ReturnCount")
  override fun get(
    returnType: Type,
    annotations: Array<Annotation>,
    retrofit: Retrofit,
  ): CallAdapter<*, *>? {
    if (getRawType(returnType) != Call::class.java) {
      return null
    }
    val apiResultType = getParameterUpperBound(0, returnType as ParameterizedType)
    if (apiResultType !is ParameterizedType || apiResultType.rawType != ClerkResult::class.java) {
      return null
    }

    return ClerkApiResultCallAdapter(retrofit, apiResultType, annotations)
  }

  private class ClerkApiResultCallAdapter(
    private val retrofit: Retrofit,
    private val apiResultType: ParameterizedType,
    private val annotations: Array<Annotation>,
  ) : CallAdapter<ClerkResult<*, *>, Call<ClerkResult<*, *>>> {

    private companion object {
      private const val HTTP_NO_CONTENT = 204
      private const val HTTP_RESET_CONTENT = 205
    }

    override fun adapt(call: Call<ClerkResult<*, *>>): Call<ClerkResult<*, *>> {
      return object : Call<ClerkResult<*, *>> by call {
        @Suppress("LongMethod")
        override fun enqueue(callback: Callback<ClerkResult<*, *>>) {
          call.enqueue(
            object : Callback<ClerkResult<*, *>> {
              override fun onFailure(call: Call<ClerkResult<*, *>>, t: Throwable) {
                when (t) {
                  is ApiException -> {
                    callback.onResponse(
                      call,
                      Response.success(
                        ClerkResult.Failure(
                          error = t.error,
                          throwable = t,
                          errorType = ClerkResult.Failure.ErrorType.API,
                          tags = mapOf(Request::class to call.request()),
                        )
                      ),
                    )
                  }
                  else -> {
                    callback.onResponse(
                      call,
                      Response.success(
                        ClerkResult.Failure(
                          error = null,
                          throwable = t,
                          errorType = ClerkResult.Failure.ErrorType.UNKNOWN,
                          tags = mapOf(Request::class to call.request()),
                        )
                      ),
                    )
                  }
                }
              }

              override fun onResponse(
                call: Call<ClerkResult<*, *>>,
                response: Response<ClerkResult<*, *>>,
              ) {
                if (response.isSuccessful) {
                  // Repackage the initial result with new tags with this call's request + response
                  val tags = mapOf(okhttp3.Response::class to response.raw())
                  val withTag =
                    when (val result = response.body()) {
                      is ClerkResult.Success -> result.withTags(result.tags + tags)
                      null -> {
                        val responseCode = response.code()
                        if (
                          (responseCode == HTTP_NO_CONTENT || responseCode == HTTP_RESET_CONTENT) &&
                            apiResultType.actualTypeArguments[0] == Unit::class.java
                        ) {
                          @Suppress("UNCHECKED_CAST")
                          ClerkResult.success(Unit).withTags(tags as Map<KClass<*>, Any>)
                        } else {
                          null
                        }
                      }
                      else -> null
                    }
                  callback.onResponse(call, Response.success(withTag))
                } else {
                  var errorBody: Any? = null
                  response.errorBody()?.let { responseBody ->
                    // Don't try to decode empty bodies
                    // Unknown length bodies (i.e. -1L) are fine
                    if (responseBody.contentLength() == 0L) return@let
                    val errorType = apiResultType.actualTypeArguments[1]
                    val statusCode = createStatusCode(response.code())
                    val nextAnnotations = annotations + statusCode
                    @Suppress("TooGenericExceptionCaught")
                    errorBody =
                      try {
                        retrofit
                          .responseBodyConverter<Any>(errorType, nextAnnotations)
                          .convert(responseBody)
                      } catch (e: Throwable) {
                        @Suppress("UNCHECKED_CAST")
                        callback.onResponse(
                          call,
                          Response.success(
                            ClerkResult.Failure(
                              error = null,
                              throwable = e,
                              errorType = ClerkResult.Failure.ErrorType.UNKNOWN,
                              tags = mapOf(okhttp3.Response::class to response.raw()),
                            )
                          ),
                        )
                        return
                      }
                  }
                  @Suppress("UNCHECKED_CAST")
                  callback.onResponse(
                    call,
                    Response.success(
                      ClerkResult.Failure(
                        error = errorBody,
                        code = response.code(),
                        errorType = ClerkResult.Failure.ErrorType.HTTP,
                        tags = mapOf(okhttp3.Response::class to response.raw()),
                      )
                    ),
                  )
                }
              }
            }
          )
        }
      }
    }

    override fun responseType(): Type = apiResultType
  }
}
