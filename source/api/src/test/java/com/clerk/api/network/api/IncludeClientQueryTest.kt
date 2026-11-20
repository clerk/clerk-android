package com.clerk.api.network.api

import com.clerk.api.network.ApiParams
import com.clerk.api.network.ClerkApi
import com.clerk.api.network.serialization.ClerkApiResultCallAdapterFactory
import com.clerk.api.network.serialization.ClerkApiResultConverterFactory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlinx.coroutines.test.runTest
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.http.Query

class IncludeClientQueryTest {

  @Test
  fun `fetchSignIn sends include client once`() = runTest {
    val interceptor = CapturingInterceptor()

    api<SignInApi>(interceptor).fetchSignIn(id = "sia_1")

    assertEquals("/v1/client/sign_ins/sia_1", interceptor.path)
    assertEquals(listOf("true"), interceptor.values("_include_client"))
  }

  @Test
  fun `fetchSignIn with a rotating token nonce keeps the nonce and sends include client`() =
    runTest {
      val interceptor = CapturingInterceptor()

      api<SignInApi>(interceptor).fetchSignIn(id = "sia_1", rotatingTokenNonce = "nonce")

      assertEquals(listOf("nonce"), interceptor.values("rotating_token_nonce"))
      assertEquals(listOf("true"), interceptor.values("_include_client"))
    }

  @Test
  fun `fetchSignUp with a rotating token nonce keeps the nonce and sends include client`() =
    runTest {
      val interceptor = CapturingInterceptor()

      api<SignUpApi>(interceptor).fetchSignUp(id = "sua_1", rotatingTokenNonce = "nonce")

      assertEquals("/v1/client/sign_ups/sua_1", interceptor.path)
      assertEquals(listOf("nonce"), interceptor.values("rotating_token_nonce"))
      assertEquals(listOf("true"), interceptor.values("_include_client"))
    }

  @Test
  fun `getUser keeps the session id and sends include client`() = runTest {
    val interceptor = CapturingInterceptor()

    api<UserApi>(interceptor).getUser(sessionId = "sess_1")

    assertEquals("/v1/me", interceptor.path)
    assertEquals(listOf("sess_1"), interceptor.values(ApiParams.CLERK_SESSION_ID))
    assertEquals(listOf("true"), interceptor.values("_include_client"))
  }

  @Test
  fun `the client fetch does not ask for the client`() {
    ClientApi::class
      .java
      .methods
      .filter { it.name == "get" || it.name == "getSkippingClientId" }
      .forEach {
        assertFalse(it.hasQuery("_include_client"), "${it.name} asks for the client")
      }
  }

  private inline fun <reified T> api(interceptor: CapturingInterceptor): T =
    Retrofit.Builder()
      .baseUrl("https://example.com/v1/")
      .client(OkHttpClient.Builder().addInterceptor(interceptor).build())
      .addCallAdapterFactory(ClerkApiResultCallAdapterFactory)
      .addConverterFactory(ClerkApiResultConverterFactory)
      .addConverterFactory(
        ClerkApi.json.asConverterFactory("application/json; charset=utf-8".toMediaType())
      )
      .build()
      .create(T::class.java)

  private fun java.lang.reflect.Method.hasQuery(value: String): Boolean =
    parameterAnnotations.any { annotations ->
      annotations.filterIsInstance<Query>().any { it.value == value }
    }

  private class CapturingInterceptor : Interceptor {
    lateinit var path: String
    private lateinit var url: okhttp3.HttpUrl

    fun values(name: String): List<String?> = url.queryParameterValues(name)

    override fun intercept(chain: Interceptor.Chain): Response {
      val request = chain.request()
      url = request.url
      path = request.url.encodedPath

      return Response.Builder()
        .request(request)
        .protocol(Protocol.HTTP_1_1)
        .code(200)
        .message("OK")
        .body("{}".toResponseBody("application/json".toMediaType()))
        .build()
    }
  }
}
