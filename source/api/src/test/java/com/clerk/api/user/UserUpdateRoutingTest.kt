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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

@Suppress("DEPRECATION") // exercises the deprecated unsafeMetadata parameter on UpdateParams
class UserUpdateRoutingTest {

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
  fun `no metadata routes to a single PATCH me`() = runTest {
    val user = newUser(unsafeMetadata = null)

    val result = user.update(User.UpdateParams(firstName = "Jane"))

    assertTrue(result is ClerkResult.Success)
    assertEquals(1, interceptor.calls.size)
    val first = interceptor.calls[0]
    assertEquals("PATCH", first.method)
    assertEquals("/v1/me", first.path)
    assertEquals("Jane", first.body["first_name"])
    assertNull(
      "unsafe_metadata must not be included when params.unsafeMetadata is null",
      first.body["unsafe_metadata"],
    )
  }

  @Test
  fun `only metadata routes to a single PATCH me_metadata with the computed patch`() = runTest {
    val user =
      newUser(
        unsafeMetadata =
          buildJsonObject {
            put("theme", "dark")
            put("layout", "compact")
          }
      )

    val result = user.update(User.UpdateParams(unsafeMetadata = """{"theme":"light"}"""))

    assertTrue(result is ClerkResult.Success)
    assertEquals(1, interceptor.calls.size)
    val call = interceptor.calls[0]
    assertEquals("PATCH", call.method)
    assertEquals("/v1/me/metadata", call.path)
    // The patch null-deletes `layout` (absent from desired) and overwrites `theme`.
    assertEquals("""{"theme":"light","layout":null}""", call.body["unsafe_metadata"])
  }

  @Test
  fun `mixed metadata and non-metadata fields issue PATCH me then PATCH me_metadata in order`() =
    runTest {
      val user = newUser(unsafeMetadata = buildJsonObject { put("foo", "old") })

      val result =
        user.update(
          User.UpdateParams(firstName = "Jane", unsafeMetadata = """{"foo":"new","bar":"added"}""")
        )

      assertTrue(result is ClerkResult.Success)
      assertEquals(2, interceptor.calls.size)

      val patchMe = interceptor.calls[0]
      assertEquals("PATCH", patchMe.method)
      assertEquals("/v1/me", patchMe.path)
      assertEquals("Jane", patchMe.body["first_name"])
      assertNull(patchMe.body["unsafe_metadata"])

      val patchMetadata = interceptor.calls[1]
      assertEquals("PATCH", patchMetadata.method)
      assertEquals("/v1/me/metadata", patchMetadata.path)
      // Patch keeps both keys: `foo` because the value changed, `bar` because it was added.
      assertEquals("""{"foo":"new","bar":"added"}""", patchMetadata.body["unsafe_metadata"])
    }

  @Test
  fun `malformed metadata returns ClerkResult Failure and issues no network calls`() = runTest {
    val user = newUser(unsafeMetadata = buildJsonObject { put("foo", "old") })

    val result = user.update(User.UpdateParams(unsafeMetadata = "{not-valid"))

    assertTrue(result is ClerkResult.Failure)
    assertEquals(ClerkResult.Failure.ErrorType.UNKNOWN, (result as ClerkResult.Failure).errorType)
    assertTrue("No network calls must be issued before validation", interceptor.calls.isEmpty())
  }

  @Test
  fun `metadata identical to current short-circuits without a metadata call`() = runTest {
    val user = newUser(unsafeMetadata = buildJsonObject { put("theme", "dark") })

    val result = user.update(User.UpdateParams(unsafeMetadata = """{"theme":"dark"}"""))

    assertTrue(result is ClerkResult.Success)
    assertTrue("Identical metadata must not issue any network call", interceptor.calls.isEmpty())
  }

  @Test
  fun `identical metadata with non-metadata fields returns the PATCH me response`() = runTest {
    // Receiver has the stale firstName; the server response (USER_RESPONSE) has the fresh one.
    // The bug was that the empty-patch short-circuit returned `this` (stale) instead of the
    // fresh updateUser response.
    val user =
      newUser(firstName = "Stale", unsafeMetadata = buildJsonObject { put("theme", "dark") })

    val result =
      user.update(
        User.UpdateParams(
          firstName = "Fresh",
          unsafeMetadata = """{"theme":"dark"}""", // identical to current
        )
      )

    assertTrue(result is ClerkResult.Success)
    assertEquals(1, interceptor.calls.size)
    val patchMe = interceptor.calls[0]
    assertEquals("PATCH", patchMe.method)
    assertEquals("/v1/me", patchMe.path)
    assertEquals("Fresh", patchMe.body["first_name"])

    val returned = (result as ClerkResult.Success).value
    assertEquals(
      "Returned user must be the PATCH /me response, not the stale receiver",
      "Fresh",
      returned.firstName,
    )
  }

  private fun newUser(unsafeMetadata: JsonObject?, firstName: String? = null): User =
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
      firstName = firstName,
      unsafeMetadata = unsafeMetadata,
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
          "first_name": "Fresh",
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
