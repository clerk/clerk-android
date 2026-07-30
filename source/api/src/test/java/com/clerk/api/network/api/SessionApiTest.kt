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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

class SessionApiTest {

  @Test
  fun `tokens posts no body at all`() = runTest {
    val interceptor = CapturingInterceptor()
    val api = sessionApi(interceptor)

    val result = api.tokens(sessionId = "sess_123")

    assertTrue(result is ClerkResult.Success)
    assertEquals("header.payload.signature", (result as ClerkResult.Success).value.jwt)
    assertEquals("POST", interceptor.method)
    assertEquals("/v1/client/sessions/sess_123/tokens", interceptor.path)
    assertNull(interceptor.contentType)
    assertEquals(0L, interceptor.contentLength)
  }

  @Test
  fun `templated tokens posts no body at all`() = runTest {
    val interceptor = CapturingInterceptor()
    val api = sessionApi(interceptor)

    val result = api.tokens(userId = "sess_123", templateType = "custom_template")

    assertTrue(result is ClerkResult.Success)
    assertEquals("/v1/client/sessions/sess_123/tokens/custom_template", interceptor.path)
    assertNull(interceptor.contentType)
    assertEquals(0L, interceptor.contentLength)
  }

  @Test
  fun `mintTokens sends the previous session token and force origin when both are set`() = runTest {
    val interceptor = CapturingInterceptor()
    val api = sessionApi(interceptor)

    val result =
      api.mintTokens(
        sessionId = "sess_123",
        previousSessionToken = "header.payload.signature",
        forceOrigin = true,
      )

    assertTrue(result is ClerkResult.Success)
    assertEquals("POST", interceptor.method)
    assertEquals("/v1/client/sessions/sess_123/tokens", interceptor.path)
    assertEquals("application/x-www-form-urlencoded", interceptor.contentType)
    assertEquals(
      mapOf("token" to "header.payload.signature", "force_origin" to "true"),
      interceptor.formBody,
    )
  }

  @Test
  fun `mintTokens omits force origin when only the seed is set`() = runTest {
    val interceptor = CapturingInterceptor()
    val api = sessionApi(interceptor)

    api.mintTokens(sessionId = "sess_123", previousSessionToken = "header.payload.signature")

    assertEquals(mapOf("token" to "header.payload.signature"), interceptor.formBody)
    assertFalse(interceptor.formBody.containsKey("force_origin"))
  }

  @Test
  fun `mintTokens omits the seed when only force origin is set`() = runTest {
    val interceptor = CapturingInterceptor()
    val api = sessionApi(interceptor)

    api.mintTokens(sessionId = "sess_123", forceOrigin = true)

    assertEquals(mapOf("force_origin" to "true"), interceptor.formBody)
    assertFalse(interceptor.formBody.containsKey("token"))
  }

  @Test
  fun `mintTokens is tagged sensitive so debug logging never prints the seed`() = runTest {
    val interceptor = CapturingInterceptor()
    val api = sessionApi(interceptor)

    api.mintTokens(sessionId = "sess_123", previousSessionToken = "header.payload.signature")

    assertTrue(interceptor.sensitiveRequest)
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

  private class CapturingInterceptor(private val responseBody: String = TOKEN_RESPONSE) :
    Interceptor {
    lateinit var method: String
    lateinit var path: String
    lateinit var formBody: Map<String, String>
    var contentType: String? = null
    var contentLength: Long = -1L
    var sensitiveRequest: Boolean = false

    override fun intercept(chain: Interceptor.Chain): Response {
      val request = chain.request()
      method = request.method
      path = request.url.encodedPath
      contentType = request.body?.contentType()?.let { "${it.type}/${it.subtype}" }
      contentLength = request.body?.contentLength() ?: -1L
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
    // TokenResource is excluded from client piggyback wrapping, so the body is the bare resource.
    const val TOKEN_RESPONSE = """{"jwt":"header.payload.signature"}"""
  }
}
