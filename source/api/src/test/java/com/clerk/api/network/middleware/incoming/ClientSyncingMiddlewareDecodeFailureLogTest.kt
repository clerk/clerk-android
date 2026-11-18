package com.clerk.api.network.middleware.incoming

import com.clerk.api.Clerk
import com.clerk.api.network.ClerkApi
import com.clerk.api.network.model.client.Client
import com.clerk.api.session.Session
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowLog

@RunWith(RobolectricTestRunner::class)
class ClientSyncingMiddlewareDecodeFailureLogTest {

  @After
  fun tearDown() {
    unmockkAll()
    Clerk.updateClient(Client())
    Clerk.clearSessionAndUserState()
    Clerk.stateStore.reset()
  }

  @Test
  fun `intercept keeps the session token out of the decode failure log`() {
    val middleware = ClientSyncingMiddleware(json = ClerkApi.json)
    val jwt = "eyJhbGciOiJSUzI1NiJ9.c2Vzc2lvbl9zZWNyZXQ.signature"

    val request =
      Request.Builder()
        .url("https://api.clerk.com/v1/client/hosted_auth")
        .post("".toRequestBody("application/x-www-form-urlencoded".toMediaType()))
        .build()
    val responseBody =
      """
      {
        "response": {
          "object": "hosted_auth",
          "url": "https://example.accounts.dev/sign-in"
        },
        "client": {
          "id": "client_123",
          "sessions": [{
            "id": "sess_123",
            "status": "active",
            "expire_at": "not-a-number",
            "last_active_at": 1000,
            "created_at": 1000,
            "updated_at": 1000,
            "last_active_token": { "jwt": "$jwt" }
          }],
          "last_active_session_id": "sess_123"
        }
      }
      """
        .trimIndent()
        .toResponseBody("application/json".toMediaType())
    val response =
      Response.Builder()
        .request(request)
        .protocol(Protocol.HTTP_1_1)
        .code(200)
        .message("OK")
        .body(responseBody)
        .build()
    val chain = mockk<Interceptor.Chain>()
    every { chain.request() } returns request
    every { chain.proceed(request) } returns response
    ShadowLog.clear()

    middleware.intercept(chain)

    val decodeFailures =
      ShadowLog.getLogs().filter { it.msg.contains("Error deserializing client") }
    assertEquals(1, decodeFailures.size)
    assertFalse(decodeFailures.single().msg.contains(jwt))
    assertFalse(decodeFailures.single().msg.contains("jwt"))
    assertEquals(emptyList<Session>(), Clerk.sessionsFlow.value)
  }
}
