package com.clerk.sdk.network.calladapter

import com.clerk.sdk.log.ClerkLog
import com.clerk.sdk.network.ClerkApiResult
import java.lang.reflect.Type
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.ResponseBody
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.IOException
import okio.Timeout
import retrofit2.Call
import retrofit2.CallAdapter
import retrofit2.Callback
import retrofit2.Converter
import retrofit2.Response

class ClerkApiResultCallAdapter<T, E>(
  private val successType: Type,
  private val errorBodyConverter: Converter<ResponseBody, E>,
) : CallAdapter<T, Call<ClerkApiResult<T, E?>>> {

  override fun responseType(): Type = successType

  override fun adapt(call: Call<T>): Call<ClerkApiResult<T, E?>> {
    return ClerkApiResultCall(call, errorBodyConverter)
  }
}

@Suppress("UNCHECKED_CAST", "TooGenericExceptionCaught")
class ClerkApiResultCall<T, E>(
  private val delegate: Call<T>,
  private val errorBodyConverter: Converter<ResponseBody, E>,
) : Call<ClerkApiResult<T, E?>> {

  override fun enqueue(callback: Callback<ClerkApiResult<T, E?>>) {
    delegate.enqueue(
      object : Callback<T> {
        override fun onResponse(call: Call<T>, response: Response<T>) {
          if (response.isSuccessful) {
            val body = response.body()
            val successResult =
              if (body != null) {
                ClerkApiResult.Success(body)
              } else {
                ClerkApiResult.Success(Unit as T)
              }
            // Success result type is correct
            callback.onResponse(this@ClerkApiResultCall, Response.success(successResult))
          } else {
            // Handle error response
            val errorBody = response.errorBody()
            val error =
              if (errorBody != null) {
                try {
                  errorBodyConverter.convert(errorBody)
                } catch (e: Exception) {
                  ClerkLog.e("Error converting error body: ${e.message}")
                  // If error conversion fails, create a default error
                  createDefaultError(response.code(), response.message())
                }
              } else {
                createDefaultError(response.code(), response.message())
              }

            // For error results, need to ensure correct type
            val errorResult: ClerkApiResult<T, E?> = ClerkApiResult.Error(error)
            callback.onResponse(this@ClerkApiResultCall, Response.success(errorResult))
          }
        }

        override fun onFailure(call: Call<T>, t: Throwable) {
          // Handle network and other errors
          val error = createErrorFromThrowable(t)
          // Explicit type to ensure compatibility
          val result: ClerkApiResult<T, E?> = ClerkApiResult.Error(error)
          callback.onResponse(this@ClerkApiResultCall, Response.success(result))
        }

        private fun createDefaultError(code: Int, message: String): E? {
          try {
            // Create a ResponseBody with our JSON
            val errorJson =
              """
              {
                "errors": [
                  {
                    "message": "HTTP Error $code",
                    "long_message": "$message",
                    "code": "http_error_$code"
                  }
                ],
                "meta": {},
                "clerk_trace_id": ""
              }
            """
                .trimIndent()

            // Use the same converter that Retrofit uses for error bodies
            val errorResponseBody = errorJson.toResponseBody("application/json".toMediaType())

            return errorBodyConverter.convert(errorResponseBody)
          } catch (e: Exception) {
            ClerkLog.e("Error converting error body: ${e.message}")
            return null
          }
        }

        private fun createErrorFromThrowable(t: Throwable): E? {
          try {
            // Create a ResponseBody with our JSON
            val errorJson =
              """
              {
                "errors": [
                  {
                    "message": "${t.message ?: "Unknown error"}",
                    "long_message": "${t.stackTraceToString()}",
                    "code": "${if (t is IOException) "network_error" else "unknown_error"}"
                  }
                ],
                "meta": {},
                "clerk_trace_id": ""
              }
            """
                .trimIndent()

            // Use the same converter that Retrofit uses for error bodies
            val errorResponseBody = errorJson.toResponseBody("application/json".toMediaType())

            return errorBodyConverter.convert(errorResponseBody)
          } catch (e: Exception) {
            ClerkLog.e("Error converting error body: ${e.message}")
            return null
          }
        }
      }
    )
  }

  override fun isExecuted(): Boolean = delegate.isExecuted

  override fun clone(): Call<ClerkApiResult<T, E?>> =
    ClerkApiResultCall(delegate.clone(), errorBodyConverter)

  override fun isCanceled(): Boolean = delegate.isCanceled

  override fun cancel() = delegate.cancel()

  override fun execute(): Response<ClerkApiResult<T, E?>> {
    val response = delegate.execute()

    if (response.isSuccessful) {
      val body = response.body()
      val successResult =
        if (body != null) {
          ClerkApiResult.Success(body)
        } else {
          ClerkApiResult.Success(Unit as T)
        }
      return Response.success(successResult)
    } else {
      // Handle error response
      val errorBody = response.errorBody()
      val error =
        if (errorBody != null) {
          try {
            errorBodyConverter.convert(errorBody)
          } catch (e: Exception) {
            ClerkLog.e("Error converting error body: ${e.message}")
            createDefaultError(response.code(), response.message())
          }
        } else {
          createDefaultError(response.code(), response.message())
        }

      // Explicit type for error result
      val errorResult: ClerkApiResult<T, E?> = ClerkApiResult.Error(error)
      return Response.success(errorResult)
    }
  }

  private fun createDefaultError(code: Int, message: String): E? {
    try {
      // Create a ResponseBody with our JSON
      val errorJson =
        """
        {
          "errors": [
            {
              "message": "HTTP Error $code",
              "long_message": "$message",
              "code": "http_error_$code"
            }
          ],
          "meta": {},
          "clerk_trace_id": ""
        }
      """
          .trimIndent()

      // Use the same converter that Retrofit uses for error bodies
      val errorResponseBody = errorJson.toResponseBody("application/json".toMediaType())

      return errorBodyConverter.convert(errorResponseBody)
    } catch (e: Exception) {
      ClerkLog.e("Error converting error body: ${e.message}")
      return null
    }
  }

  private fun createErrorFromThrowable(t: Throwable): E? {
    try {
      // Create a ResponseBody with our JSON
      val errorJson =
        """
        {
          "errors": [
            {
              "message": "${t.message ?: "Unknown error"}",
              "long_message": "${t.stackTraceToString()}",
              "code": "${if (t is IOException) "network_error" else "unknown_error"}"
            }
          ],
          "meta": {},
          "clerk_trace_id": ""
        }
      """
          .trimIndent()

      // Use the same converter that Retrofit uses for error bodies
      val errorResponseBody = errorJson.toResponseBody("application/json".toMediaType())

      return errorBodyConverter.convert(errorResponseBody)
    } catch (e: Exception) {
      ClerkLog.e("Error converting error body: ${e.message}")
      return null
    }
  }

  override fun request(): Request = delegate.request()

  override fun timeout(): Timeout = delegate.timeout()
}
