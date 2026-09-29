package com.clerk.api.network.serialization

import com.clerk.api.network.model.error.ClerkErrorResponse
import io.mockk.every
import io.mockk.mockk
import java.lang.reflect.Type
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Call
import retrofit2.CallAdapter
import retrofit2.Callback
import retrofit2.Converter
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

class ClerkApiResultCallAdapterFactoryTest {

  @Test
  fun `undecodable error body becomes an unknown failure`() {
    val retrofit = retrofit(Json.asConverterFactory(JSON))

    val result = enqueueErrorResponse(retrofit, "not json")

    assertTrue(result is ClerkResult.Failure)
    val failure = result as ClerkResult.Failure
    assertEquals(ClerkResult.Failure.ErrorType.UNKNOWN, failure.errorType)
    assertTrue(failure.throwable is SerializationException)
  }

  @Test
  fun `error thrown while decoding error body propagates`() {
    val error = OutOfMemoryError("decode")
    val retrofit = retrofit(ThrowingConverterFactory(error))

    val thrown = assertThrows(OutOfMemoryError::class.java) { enqueueErrorResponse(retrofit, "{}") }

    assertEquals(error, thrown)
  }

  private fun retrofit(converterFactory: Converter.Factory): Retrofit =
    Retrofit.Builder()
      .baseUrl("https://example.com/")
      .addCallAdapterFactory(ClerkApiResultCallAdapterFactory)
      .addConverterFactory(converterFactory)
      .build()

  @Suppress("UNCHECKED_CAST")
  private fun enqueueErrorResponse(retrofit: Retrofit, body: String): ClerkResult<*, *>? {
    val returnType =
      ParameterizedTypeImpl(
        null,
        Call::class.java,
        ParameterizedTypeImpl(null, ClerkResult::class.java, Unit::class.java, ERROR_TYPE),
      )
    val adapter =
      retrofit.callAdapter(returnType, emptyArray())
        as CallAdapter<ClerkResult<*, *>, Call<ClerkResult<*, *>>>

    val call = mockk<Call<ClerkResult<*, *>>>()
    every { call.enqueue(any()) } answers
      {
        firstArg<Callback<ClerkResult<*, *>>>()
          .onResponse(call, Response.error(400, body.toResponseBody(JSON)))
      }

    var result: ClerkResult<*, *>? = null
    adapter
      .adapt(call)
      .enqueue(
        object : Callback<ClerkResult<*, *>> {
          override fun onResponse(
            call: Call<ClerkResult<*, *>>,
            response: Response<ClerkResult<*, *>>,
          ) {
            result = response.body()
          }

          override fun onFailure(call: Call<ClerkResult<*, *>>, t: Throwable) = Unit
        }
      )
    return result
  }

  private class ThrowingConverterFactory(private val throwable: Throwable) : Converter.Factory() {
    override fun responseBodyConverter(
      type: Type,
      annotations: Array<out Annotation>,
      retrofit: Retrofit,
    ): Converter<ResponseBody, *> = Converter<ResponseBody, Any> { throw throwable }
  }

  private companion object {
    val JSON = "application/json".toMediaType()
    val ERROR_TYPE: Type = ClerkErrorResponse::class.java
  }
}
