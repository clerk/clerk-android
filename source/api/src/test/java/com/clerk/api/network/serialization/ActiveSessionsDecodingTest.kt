package com.clerk.api.network.serialization

import com.clerk.api.network.ClerkApi
import com.clerk.api.network.api.UserApi
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

class ActiveSessionsDecodingTest {
  private val activeSessionsBody =
    """
    [{"object":"session","id":"sess_1","status":"active","expire_at":2081458542453,
    "last_active_at":1766098542453,"created_at":1766098542453,"updated_at":1766098542453,
    "latest_activity":{"object":"session_activity","id":"sess_activity_1","is_mobile":true}}]
    """
      .trimIndent()

  private fun userApiRespondingWith(body: String): UserApi {
    val client =
      OkHttpClient.Builder()
        .addInterceptor { chain ->
          Response.Builder()
            .request(chain.request())
            .protocol(Protocol.HTTP_1_1)
            .code(200)
            .message("OK")
            .body(body.toResponseBody("application/json".toMediaType()))
            .build()
        }
        .build()
    return Retrofit.Builder()
      .baseUrl("https://example.com/v1/")
      .client(client)
      .addCallAdapterFactory(ClerkApiResultCallAdapterFactory)
      .addConverterFactory(ClerkApiResultConverterFactory)
      .addConverterFactory(
        ClerkApi.json.asConverterFactory("application/json; charset=utf-8".toMediaType())
      )
      .build()
      .create(UserApi::class.java)
  }

  @Test
  fun `active sessions decode from the plain JSON array the API returns`(): Unit = runBlocking {
    val result = userApiRespondingWith(activeSessionsBody).getActiveSessions(sessionId = "sess_1")

    assertTrue("expected success but got $result", result is ClerkResult.Success)
    val sessions = (result as ClerkResult.Success).value
    assertEquals(listOf("sess_1"), sessions.map { it.id })
  }
}
