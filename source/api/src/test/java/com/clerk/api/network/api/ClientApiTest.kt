package com.clerk.api.network.api

import com.clerk.api.network.ClerkApi
import com.clerk.api.network.middleware.ManualClientSyncRequest
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
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

class ClientApiTest {

  @Test
  fun `createHostedAuth posts PKCE state redirect and mode as form fields`() = runTest {
    val interceptor = CapturingInterceptor(HOSTED_AUTH_RESPONSE)
    val api = clientApi(interceptor)

    val result =
      api.createHostedAuth(
        redirectUrl = "clerk://com.example.app.callback",
        codeChallenge = "challenge_123",
        state = "state_123",
        mode = "sign-up",
      )

    assertTrue(result is ClerkResult.Success)
    val hostedAuth = (result as ClerkResult.Success).value
    assertEquals("hosted_auth", hostedAuth.objectType)
    assertEquals("https://example.accounts.dev/sign-in", hostedAuth.url)
    assertEquals("POST", interceptor.method)
    assertEquals("/v1/client/hosted_auth", interceptor.path)
    assertTrue(interceptor.sensitiveRequest)
    assertEquals(
      mapOf(
        "redirect_url" to "clerk://com.example.app.callback",
        "code_challenge" to "challenge_123",
        "state" to "state_123",
        "mode" to "sign-up",
      ),
      interceptor.formBody,
    )
  }

  @Test
  fun `createHostedAuth omits optional mode`() = runTest {
    val interceptor = CapturingInterceptor(HOSTED_AUTH_RESPONSE)
    val api = clientApi(interceptor)

    val result =
      api.createHostedAuth(
        redirectUrl = "clerk://com.example.app.callback",
        codeChallenge = "challenge_123",
        state = "state_123",
      )

    assertTrue(result is ClerkResult.Success)
    assertFalse(interceptor.formBody.containsKey("mode"))
    assertFalse(interceptor.formBody.containsKey("initial_page"))
  }

  @Test
  fun `redeemHostedAuth posts verifier and nonce in form body`() = runTest {
    val interceptor = CapturingInterceptor(CLIENT_RESPONSE)
    val api = clientApi(interceptor)

    val result =
      api.redeemHostedAuth(
        rotatingTokenNonce = "nonce_123",
        codeVerifier = "verifier_123",
      )

    assertTrue(result is ClerkResult.Success)
    assertEquals("client_123", (result as ClerkResult.Success).value.id)
    assertEquals("POST", interceptor.method)
    assertEquals("/v1/client", interceptor.path)
    assertEquals(null, interceptor.query)
    assertTrue(interceptor.manualClientSyncRequest)
    assertTrue(interceptor.sensitiveRequest)
    assertEquals(
      mapOf(
        "_method" to "GET",
        "rotating_token_nonce" to "nonce_123",
        "code_verifier" to "verifier_123",
      ),
      interceptor.formBody,
    )
  }

  @Test
  fun `setActive request includes select org intent and organization id`() = runTest {
    val interceptor = CapturingInterceptor()
    val api = clientApi(interceptor)

    val result =
      api.setActive(
        sessionId = "sess_123",
        organizationId = "org_123",
        intent = SET_ACTIVE_INTENT_SELECT_ORG,
      )

    assertTrue(result is ClerkResult.Success)
    assertEquals("POST", interceptor.method)
    assertEquals("/v1/client/sessions/sess_123/touch", interceptor.path)
    assertEquals("org_123", interceptor.formBody["active_organization_id"])
    assertEquals(SET_ACTIVE_INTENT_SELECT_ORG, interceptor.formBody["intent"])
  }

  @Test
  fun `setActive request sends empty organization id when clearing active organization`() =
    runTest {
      val interceptor = CapturingInterceptor()
      val api = clientApi(interceptor)

      val result =
        api.setActive(
          sessionId = "sess_123",
          organizationId = "",
          intent = SET_ACTIVE_INTENT_SELECT_ORG,
        )

      assertTrue(result is ClerkResult.Success)
      assertEquals("", interceptor.formBody["active_organization_id"])
      assertEquals(SET_ACTIVE_INTENT_SELECT_ORG, interceptor.formBody["intent"])
    }

  private fun clientApi(interceptor: CapturingInterceptor): ClientApi {
    return Retrofit.Builder()
      .baseUrl("https://example.com/v1/")
      .client(OkHttpClient.Builder().addInterceptor(interceptor).build())
      .addCallAdapterFactory(ClerkApiResultCallAdapterFactory)
      .addConverterFactory(ClerkApiResultConverterFactory)
      .addConverterFactory(
        ClerkApi.json.asConverterFactory("application/json; charset=utf-8".toMediaType())
      )
      .build()
      .create(ClientApi::class.java)
  }

  private class CapturingInterceptor(private val responseBody: String = SESSION_RESPONSE) :
    Interceptor {
    lateinit var method: String
    lateinit var path: String
    lateinit var formBody: Map<String, String>
    var query: String? = null
    var manualClientSyncRequest: Boolean = false
    var sensitiveRequest: Boolean = false

    override fun intercept(chain: Interceptor.Chain): Response {
      val request = chain.request()
      method = request.method
      path = request.url.encodedPath
      query = request.url.encodedQuery
      manualClientSyncRequest = request.tag(ManualClientSyncRequest::class.java) != null
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
    const val HOSTED_AUTH_RESPONSE =
      """
      {
        "response": {
          "object": "hosted_auth",
          "url": "https://example.accounts.dev/sign-in"
        },
        "client": {
          "id": "client_123",
          "sessions": []
        }
      }
      """

    const val CLIENT_RESPONSE =
      """
      {
        "response": {
          "id": "client_123",
          "sessions": []
        },
        "client": null
      }
      """

    const val SESSION_RESPONSE =
      """
      {
        "response": {
          "id": "sess_123",
          "status": "active",
          "expire_at": 10000,
          "last_active_at": 1000,
          "created_at": 1000,
          "updated_at": 1000
        },
        "client": null
      }
      """
  }
}
