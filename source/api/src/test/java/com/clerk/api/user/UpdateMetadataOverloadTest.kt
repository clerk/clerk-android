package com.clerk.api.user

import com.clerk.api.network.ClerkApi
import com.clerk.api.network.api.UserApi
import com.clerk.api.network.serialization.ClerkApiResultCallAdapterFactory
import com.clerk.api.network.serialization.ClerkApiResultConverterFactory
import com.clerk.api.network.serialization.ClerkResult
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

class UpdateMetadataOverloadTest {

  private lateinit var interceptor: CapturingInterceptor
  private var originalUserApi: Any? = null

  @Before
  fun setUp() {
    interceptor = CapturingInterceptor()
    val api =
      Retrofit.Builder()
        .baseUrl("https://example.com/v1/")
        .client(OkHttpClient.Builder().addInterceptor(interceptor).build())
        .addCallAdapterFactory(ClerkApiResultCallAdapterFactory)
        .addConverterFactory(ClerkApiResultConverterFactory)
        .addConverterFactory(
          ClerkApi.json.asConverterFactory("application/json; charset=utf-8".toMediaType())
        )
        .build()
        .create(UserApi::class.java)

    originalUserApi = getField("_user")
    setField("_user", api)
  }

  @After
  fun tearDown() {
    setField("_user", originalUserApi)
  }

  @Test
  fun `JsonObject overload issues a single PATCH me_metadata with stringified body`() = runTest {
    val user = newUser()
    val payload = buildJsonObject { put("theme", "dark") }

    val result = user.updateMetadata(payload)

    assertTrue(result is ClerkResult.Success)
    assertEquals(1, interceptor.calls.size)
    val call = interceptor.calls[0]
    assertEquals("PATCH", call.method)
    assertEquals("/v1/me/metadata", call.path)
    assertEquals(payload.toString(), call.body["unsafe_metadata"])
  }

  @Test
  fun `empty JsonObject overload sends empty-object payload (no special handling)`() = runTest {
    val user = newUser()

    val result = user.updateMetadata(JsonObject(emptyMap()))

    assertTrue(result is ClerkResult.Success)
    assertEquals(1, interceptor.calls.size)
    assertEquals("{}", interceptor.calls[0].body["unsafe_metadata"])
  }

  private fun newUser(): User =
    User(
      id = "user_123",
      imageUrl = "",
      hasImage = false,
      passkeys = emptyList(),
      passwordEnabled = false,
      phoneNumbers = emptyList(),
      totpEnabled = false,
      twoFactorEnabled = false,
      updatedAt = 0L,
    )

  private fun getField(name: String): Any? {
    val field = ClerkApi::class.java.getDeclaredField(name)
    field.isAccessible = true
    return field.get(ClerkApi)
  }

  private fun setField(name: String, value: Any?) {
    val field = ClerkApi::class.java.getDeclaredField(name)
    field.isAccessible = true
    field.set(ClerkApi, value)
  }

  private data class CapturedCall(
    val method: String,
    val path: String,
    val body: Map<String, String>,
  )

  private class CapturingInterceptor : Interceptor {
    val calls: MutableList<CapturedCall> = mutableListOf()

    override fun intercept(chain: Interceptor.Chain): Response {
      val request = chain.request()
      calls.add(
        CapturedCall(
          method = request.method,
          path = request.url.encodedPath,
          body = request.body.readFormBody(),
        )
      )

      return Response.Builder()
        .request(request)
        .protocol(Protocol.HTTP_1_1)
        .code(200)
        .message("OK")
        .body(USER_RESPONSE.toResponseBody("application/json".toMediaType()))
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
    const val USER_RESPONSE =
      """
      {
        "response": {
          "id": "user_123",
          "image_url": "",
          "has_image": false,
          "passkeys": [],
          "password_enabled": false,
          "phone_numbers": [],
          "totp_enabled": false,
          "two_factor_enabled": false,
          "updated_at": 0
        },
        "client": null
      }
      """
  }
}
