package com.clerk.api.session

import com.clerk.api.Clerk
import com.clerk.api.network.ClerkApi
import com.clerk.api.network.api.SessionApi
import com.clerk.api.network.model.client.Client
import com.clerk.api.network.serialization.ClerkApiResultCallAdapterFactory
import com.clerk.api.network.serialization.ClerkApiResultConverterFactory
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkAll
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

class SessionRevokeTest {
  private val requests = mutableListOf<Request>()

  @Before
  fun setup() {
    val client =
      OkHttpClient.Builder()
        .addInterceptor { chain ->
          requests += chain.request()
          Response.Builder()
            .request(chain.request())
            .protocol(Protocol.HTTP_1_1)
            .code(200)
            .message("OK")
            .body("{}".toResponseBody(JSON))
            .build()
        }
        .build()
    val sessionApi =
      Retrofit.Builder()
        .baseUrl("https://clerk.example.com/v1/")
        .client(client)
        .addCallAdapterFactory(ClerkApiResultCallAdapterFactory)
        .addConverterFactory(ClerkApiResultConverterFactory)
        .addConverterFactory(ClerkApi.json.asConverterFactory(JSON))
        .build()
        .create(SessionApi::class.java)
    mockkObject(ClerkApi)
    every { ClerkApi.session } returns sessionApi
  }

  @After
  fun tearDown() {
    Clerk.updateClient(Client())
    unmockkAll()
  }

  @Test
  fun `revoke authenticates the request as the active session`() = runTest {
    val active = session("sess_active")
    val other = session("sess_other")
    Clerk.updateClient(Client(sessions = listOf(active, other), lastActiveSessionId = active.id))

    other.revoke()

    val url = requests.single().url
    assertEquals("/v1/me/sessions/sess_other/revoke", url.encodedPath)
    assertEquals("sess_active", url.queryParameter("_clerk_session_id"))
  }

  private fun session(id: String): Session =
    Session(
      id = id,
      status = Session.SessionStatus.ACTIVE,
      expireAt = 0L,
      lastActiveAt = 0L,
      createdAt = 0L,
      updatedAt = 0L,
    )

  private companion object {
    val JSON = "application/json; charset=utf-8".toMediaType()
  }
}
