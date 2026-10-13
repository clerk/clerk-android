package com.clerk.api.network.api

import com.clerk.api.network.ClerkApi
import com.clerk.api.network.serialization.ClerkApiResultCallAdapterFactory
import com.clerk.api.network.serialization.ClerkApiResultConverterFactory
import com.clerk.api.network.serialization.ClerkResult
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import kotlinx.coroutines.test.runTest
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

class ProtectCheckApiTest {

  @Test
  fun `sign in protect check patches proof token`() = runTest {
    val interceptor = CapturingInterceptor(wrapped(SIGN_IN_RESPONSE))
    val api = retrofit(interceptor).create(SignInApi::class.java)

    val result = api.submitProtectCheck(id = "sia_123", proofToken = "proof/+ token")

    assertTrue(result is ClerkResult.Success)
    assertEquals("PATCH", interceptor.method)
    assertEquals("/v1/client/sign_ins/sia_123/protect_check", interceptor.path)
    assertEquals("proof/+ token", interceptor.formBody["proof_token"])
  }

  @Test
  fun `sign up protect check patches proof token`() = runTest {
    val interceptor = CapturingInterceptor(wrapped(SIGN_UP_RESPONSE))
    val api = retrofit(interceptor).create(SignUpApi::class.java)

    val result = api.submitProtectCheck(id = "sua_123", proofToken = "proof-token")

    assertTrue(result is ClerkResult.Success)
    assertEquals("PATCH", interceptor.method)
    assertEquals("/v1/client/sign_ups/sua_123/protect_check", interceptor.path)
    assertEquals("proof-token", interceptor.formBody["proof_token"])
  }

  private fun retrofit(interceptor: CapturingInterceptor): Retrofit {
    return Retrofit.Builder()
      .baseUrl("https://example.com/v1/")
      .client(OkHttpClient.Builder().addInterceptor(interceptor).build())
      .addCallAdapterFactory(ClerkApiResultCallAdapterFactory)
      .addConverterFactory(ClerkApiResultConverterFactory)
      .addConverterFactory(
        ClerkApi.json.asConverterFactory("application/json; charset=utf-8".toMediaType())
      )
      .build()
  }

  private class CapturingInterceptor(private val responseBody: String) : Interceptor {
    lateinit var method: String
    lateinit var path: String
    lateinit var formBody: Map<String, String>

    override fun intercept(chain: Interceptor.Chain): Response {
      val request = chain.request()
      method = request.method
      path = request.url.encodedPath
      formBody = request.body.readFormBody()

      return Response.Builder()
        .request(request)
        .protocol(Protocol.HTTP_1_1)
        .code(200)
        .message("OK")
        .body(responseBody.toResponseBody("application/json".toMediaType()))
        .build()
    }

    private fun okhttp3.RequestBody?.readFormBody(): Map<String, String> {
      if (this == null) return emptyMap()
      val buffer = Buffer()
      writeTo(buffer)
      return buffer
        .readUtf8()
        .split("&")
        .filter { it.isNotEmpty() }
        .associate { pair ->
          val (key, value) = pair.split("=", limit = 2) + listOf("")
          URLDecoder.decode(key, StandardCharsets.UTF_8.name()) to
            URLDecoder.decode(value, StandardCharsets.UTF_8.name())
        }
    }
  }

  private companion object {
    fun wrapped(response: String): String = """{"response": $response, "client": null}"""

    const val SIGN_IN_RESPONSE = """{"id":"sia_123","status":"needs_first_factor"}"""

    const val SIGN_UP_RESPONSE =
      """
      {
        "id": "sua_123",
        "status": "complete",
        "required_fields": [],
        "optional_fields": [],
        "missing_fields": [],
        "unverified_fields": [],
        "verifications": {},
        "password_enabled": false
      }
      """
  }
}
