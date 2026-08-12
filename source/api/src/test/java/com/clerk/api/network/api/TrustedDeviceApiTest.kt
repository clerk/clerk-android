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

class TrustedDeviceApiTest {

  @Test
  fun `list requests trusted devices for the current user`() = runTest {
    val interceptor = CapturingInterceptor(TRUSTED_DEVICE_LIST_RESPONSE)
    val api = trustedDeviceApi(interceptor)

    val result = api.list(sessionId = "sess_123")

    assertTrue(result is ClerkResult.Success)
    assertEquals("GET", interceptor.method)
    assertEquals("/v1/me/trusted_devices", interceptor.path)
    assertEquals("sess_123", interceptor.queryParams["_clerk_session_id"])
  }

  @Test
  fun `prepareEnrollment sends platform and public key jwk`() = runTest {
    val interceptor = CapturingInterceptor(wrapped(TRUSTED_DEVICE_CHALLENGE_RESPONSE))
    val api = trustedDeviceApi(interceptor)

    val result =
      api.prepareEnrollment(
        appIdentifier = "com.example.app",
        name = "Pixel 9",
        publicKeyJwk = """{"kty":"EC"}""",
        sessionId = "sess_123",
      )

    assertTrue(result is ClerkResult.Success)
    assertEquals("POST", interceptor.method)
    assertEquals("/v1/me/trusted_devices/prepare", interceptor.path)
    assertEquals("android", interceptor.formBody["platform"])
    assertEquals("com.example.app", interceptor.formBody["app_identifier"])
    assertEquals("Pixel 9", interceptor.formBody["name"])
    assertEquals("ES256", interceptor.formBody["algorithm"])
    assertEquals("""{"kty":"EC"}""", interceptor.formBody["public_key_jwk"])
  }

  @Test
  fun `attemptEnrollment sends signed challenge`() = runTest {
    val interceptor = CapturingInterceptor(wrapped(TRUSTED_DEVICE_RESPONSE))
    val api = trustedDeviceApi(interceptor)

    val result =
      api.attemptEnrollment(
        appIdentifier = "com.example.app",
        publicKeyJwk = """{"kty":"EC"}""",
        clientData = "client-data",
        signature = "base64url-signature",
        sessionId = "sess_123",
      )

    assertTrue(result is ClerkResult.Success)
    assertEquals("POST", interceptor.method)
    assertEquals("/v1/me/trusted_devices/attempt", interceptor.path)
    assertEquals("client-data", interceptor.formBody["client_data"])
    assertEquals("base64url-signature", interceptor.formBody["signature"])
    assertEquals("android", interceptor.formBody["platform"])
  }

  @Test
  fun `validateSignInCredential posts against the client`() = runTest {
    val interceptor = CapturingInterceptor(wrapped("""{"valid": true}"""))
    val api = trustedDeviceApi(interceptor)

    val result = api.validateSignInCredential(trustedDeviceId = "td_123")

    assertTrue(result is ClerkResult.Success)
    assertEquals("POST", interceptor.method)
    assertEquals("/v1/client/trusted_devices/validate", interceptor.path)
    assertEquals("td_123", interceptor.formBody["trusted_device_id"])
  }

  @Test
  fun `revoke deletes the trusted device`() = runTest {
    val interceptor = CapturingInterceptor(wrapped(TRUSTED_DEVICE_RESPONSE))
    val api = trustedDeviceApi(interceptor)

    val result = api.revoke(trustedDeviceId = "td_123", sessionId = "sess_123")

    assertTrue(result is ClerkResult.Success)
    assertEquals("DELETE", interceptor.method)
    assertEquals("/v1/me/trusted_devices/td_123", interceptor.path)
    assertEquals("sess_123", interceptor.queryParams["_clerk_session_id"])
  }

  private fun trustedDeviceApi(interceptor: CapturingInterceptor): TrustedDeviceApi {
    return Retrofit.Builder()
      .baseUrl("https://example.com/v1/")
      .client(OkHttpClient.Builder().addInterceptor(interceptor).build())
      .addCallAdapterFactory(ClerkApiResultCallAdapterFactory)
      .addConverterFactory(ClerkApiResultConverterFactory)
      .addConverterFactory(
        ClerkApi.json.asConverterFactory("application/json; charset=utf-8".toMediaType())
      )
      .build()
      .create(TrustedDeviceApi::class.java)
  }

  private class CapturingInterceptor(private val responseBody: String) : Interceptor {
    lateinit var method: String
    lateinit var path: String
    lateinit var formBody: Map<String, String>
    lateinit var queryParams: Map<String, String>

    override fun intercept(chain: Interceptor.Chain): Response {
      val request = chain.request()
      method = request.method
      path = request.url.encodedPath
      formBody = request.body.readFormBody()
      queryParams =
        request.url.queryParameterNames.associateWith { request.url.queryParameter(it).orEmpty() }

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
    /** Mutation responses arrive wrapped in the client piggyback envelope. */
    fun wrapped(response: String): String = """{"response": $response, "client": null}"""

    const val TRUSTED_DEVICE_RESPONSE =
      """
      {
        "id": "td_123",
        "object": "trusted_device",
        "platform": "android",
        "app_identifier": "com.example.app",
        "name": "Pixel 9",
        "algorithm": "ES256",
        "status": "active",
        "created_at": 1720000000000,
        "updated_at": 1720000001000
      }
      """

    const val TRUSTED_DEVICE_LIST_RESPONSE = "[$TRUSTED_DEVICE_RESPONSE]"

    const val TRUSTED_DEVICE_CHALLENGE_RESPONSE =
      """
      {
        "object": "trusted_device_challenge",
        "challenge": "challenge-value",
        "challenge_id": "tdc_123",
        "client_data": "client-data",
        "expires_at": 1720000005000,
        "algorithm": "ES256"
      }
      """
  }
}
