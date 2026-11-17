package com.clerk.api.network.api

import com.clerk.api.network.ClerkApi
import com.clerk.api.network.middleware.SensitiveRequest
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

class SessionApiTest {

  @Test
  fun `createSessionTransfer posts redirect url and presentation mode as sensitive form fields`() =
    runTest {
      val interceptor = CapturingInterceptor(SESSION_TRANSFER_RESPONSE)
      val api = sessionApi(interceptor)

      val result =
        api.createSessionTransfer(
          sessionId = "sess_123",
          redirectUrl = "https://example.com/disputes/new?transaction=streamly",
          presentationMode = "browser",
        )

      assertTrue(result is ClerkResult.Success)
      val transfer = (result as ClerkResult.Success).value
      assertEquals("session_transfer", transfer.objectType)
      assertEquals("https://clerk.example.com/v1/session_transfers/redeem?token=abc", transfer.url)
      assertEquals(1_700_000_060_000L, transfer.expiresAt)
      assertEquals("POST", interceptor.method)
      assertEquals("/v1/client/sessions/sess_123/session_transfers", interceptor.path)
      assertTrue(interceptor.sensitiveRequest)
      assertEquals(
        mapOf(
          "redirect_url" to "https://example.com/disputes/new?transaction=streamly",
          "presentation_mode" to "browser",
        ),
        interceptor.formBody,
      )
    }

  private fun sessionApi(interceptor: CapturingInterceptor): SessionApi {
    return Retrofit.Builder()
      .baseUrl("https://example.com/v1/")
      .client(OkHttpClient.Builder().addInterceptor(interceptor).build())
      .addCallAdapterFactory(ClerkApiResultCallAdapterFactory)
      .addConverterFactory(ClerkApiResultConverterFactory)
      .addConverterFactory(
        ClerkApi.json.asConverterFactory("application/json; charset=utf-8".toMediaType())
      )
      .build()
      .create(SessionApi::class.java)
  }

  private class CapturingInterceptor(private val responseBody: String) : Interceptor {
    lateinit var method: String
    lateinit var path: String
    lateinit var formBody: Map<String, String>
    var sensitiveRequest: Boolean = false

    override fun intercept(chain: Interceptor.Chain): Response {
      val request = chain.request()
      method = request.method
      path = request.url.encodedPath
      sensitiveRequest = request.tag(SensitiveRequest::class.java) != null
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
          val parts = pair.split("=", limit = 2)
          val key = parts.first().urlDecode()
          val value = parts.getOrElse(1) { "" }.urlDecode()
          key to value
        }
    }

    private fun String.urlDecode(): String = URLDecoder.decode(this, StandardCharsets.UTF_8.name())
  }

  private companion object {
    const val SESSION_TRANSFER_RESPONSE =
      """
      {
        "response": {
          "object": "session_transfer",
          "url": "https://clerk.example.com/v1/session_transfers/redeem?token=abc",
          "expires_at": 1700000060000
        },
        "client": {
          "id": "client_123",
          "sessions": []
        }
      }
      """
  }
}
