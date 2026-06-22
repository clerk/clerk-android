package com.clerk.api.user

import com.clerk.api.network.ClerkApi
import com.clerk.api.network.api.UserApi
import com.clerk.api.network.serialization.ClerkApiResultCallAdapterFactory
import com.clerk.api.network.serialization.ClerkApiResultConverterFactory
import com.clerk.api.network.serialization.ClerkResult
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
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
  fun `only metadata reloads then issues a PATCH me_metadata with the computed patch`() = runTest {
    // Server reflects what the receiver has cached locally.
    interceptor.serverUnsafeMetadata = buildJsonObject {
      put("theme", "dark")
      put("layout", "compact")
    }
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
    // Two calls now: a GET /me reload to refresh the diff baseline, then the PATCH /me/metadata.
    assertEquals(2, interceptor.calls.size)
    assertEquals("GET", interceptor.calls[0].method)
    assertEquals("/v1/me", interceptor.calls[0].path)
    val patchMetadata = interceptor.calls[1]
    assertEquals("PATCH", patchMetadata.method)
    assertEquals("/v1/me/metadata", patchMetadata.path)
    // The patch null-deletes `layout` (absent from desired) and overwrites `theme`.
    assertEquals("""{"theme":"light","layout":null}""", patchMetadata.body["unsafe_metadata"])
  }

  @Test
  fun `mixed metadata and non-metadata fields issue PATCH me then PATCH me_metadata in order`() =
    runTest {
      interceptor.serverUnsafeMetadata = buildJsonObject { put("foo", "old") }
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
  fun `identical metadata skips the metadata PATCH`() = runTest {
    interceptor.serverUnsafeMetadata = buildJsonObject { put("theme", "dark") }
    val user = newUser(unsafeMetadata = buildJsonObject { put("theme", "dark") })

    val result = user.update(User.UpdateParams(unsafeMetadata = """{"theme":"dark"}"""))

    assertTrue(result is ClerkResult.Success)
    // The pre-diff reload always runs, but the patch is empty so no PATCH /me/metadata.
    assertEquals(1, interceptor.calls.size)
    assertEquals("GET", interceptor.calls[0].method)
    assertEquals("/v1/me", interceptor.calls[0].path)
  }

  @Test
  fun `identical metadata with non-metadata fields returns the PATCH me response`() = runTest {
    // Receiver has the stale firstName; the server response (USER_RESPONSE) has the fresh one.
    // The bug was that the empty-patch short-circuit returned `this` (stale) instead of the
    // fresh updateUser response.
    interceptor.serverUnsafeMetadata = buildJsonObject { put("theme", "dark") }
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

  @Test
  fun `reloads before diffing so server-side mutations are not lost`() = runTest {
    // The local cache thinks unsafeMetadata is { position: "goalie" }, but the server has
    // drifted to { position: "goalie", adminAdded: "yes" }. 
    // Without the pre-diff reload the SDK would compute
    // mergePatch({position:goalie}, {city:Toronto}) = {position:null, city:Toronto},
    // and `adminAdded` would survive on the server — silently violating replace semantics.
    interceptor.serverUnsafeMetadata = buildJsonObject {
      put("position", "goalie")
      put("adminAdded", "yes")
    }
    val user = newUser(unsafeMetadata = buildJsonObject { put("position", "goalie") })

    val result = user.update(User.UpdateParams(unsafeMetadata = """{"city":"Toronto"}"""))

    assertTrue(result is ClerkResult.Success)
    assertEquals(2, interceptor.calls.size)
    assertEquals("GET", interceptor.calls[0].method)
    assertEquals("/v1/me", interceptor.calls[0].path)

    val patchMetadata = interceptor.calls[1]
    assertEquals("PATCH", patchMetadata.method)
    assertEquals("/v1/me/metadata", patchMetadata.path)
    // The patch null-deletes BOTH server-side keys because the reload surfaced them; without
    // the freshness fix `adminAdded` would not appear here.
    assertEquals(
      """{"city":"Toronto","position":null,"adminAdded":null}""",
      patchMetadata.body["unsafe_metadata"],
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

    /**
     * When non-null, the mock embeds this value as `unsafe_metadata` in every successful
     * response, simulating the server's view of the user. Tests set this to either match or
     * diverge from the receiver's locally cached metadata depending on what behavior they
     * want to exercise.
     */
    var serverUnsafeMetadata: JsonObject? = null

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
        .body(buildUserResponseJson(serverUnsafeMetadata).toResponseBody("application/json".toMediaType()))
        .build()
    }

    private fun buildUserResponseJson(metadata: JsonObject?): String =
      buildJsonObject {
          putJsonObject("response") {
            put("id", "user_123")
            put("image_url", "")
            put("has_image", false)
            put("first_name", "Fresh")
            putJsonArray("passkeys") {}
            put("password_enabled", false)
            putJsonArray("phone_numbers") {}
            put("totp_enabled", false)
            put("two_factor_enabled", false)
            put("updated_at", 0)
            metadata?.let { put("unsafe_metadata", it) }
          }
          put("client", JsonNull)
        }
        .toString()

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
}
