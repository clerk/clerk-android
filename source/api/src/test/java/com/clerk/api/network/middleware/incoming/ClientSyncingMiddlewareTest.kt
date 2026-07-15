package com.clerk.api.network.middleware.incoming

import com.clerk.api.Clerk
import com.clerk.api.auth.AuthEvent
import com.clerk.api.network.ClerkApi
import com.clerk.api.network.model.client.Client
import com.clerk.api.session.Session
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
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
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ClientSyncingMiddlewareTest {

  @After
  fun tearDown() {
    unmockkAll()
    Clerk.updateClient(Client())
    Clerk.clearSessionAndUserState()
  }

  @Test
  fun `intercept emits SignUpCompleted when sign up response is complete`() = runTest {
    val middleware = ClientSyncingMiddleware(json = ClerkApi.json)

    val request =
      Request.Builder()
        .url("https://api.clerk.com/v1/client/sign_ups/su_123/attempt_verification")
        .post("".toRequestBody("application/x-www-form-urlencoded".toMediaType()))
        .build()

    val responseBody =
      """
      {
        "response": {
          "id": "su_123",
          "status": "complete",
          "required_fields": [],
          "optional_fields": [],
          "missing_fields": [],
          "unverified_fields": [],
          "verifications": {},
          "password_enabled": false,
          "created_at": 0,
          "updated_at": 0
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

    val completedEvents = mutableListOf<AuthEvent.SignUpCompleted>()
    val collectionJob =
      launch(start = CoroutineStart.UNDISPATCHED) {
        Clerk.auth.events
          .filterIsInstance<AuthEvent.SignUpCompleted>()
          .take(1)
          .toList(completedEvents)
      }

    middleware.intercept(chain)

    withTimeout(1_000) { collectionJob.join() }

    assertEquals(1, completedEvents.size)
    assertEquals("su_123", completedEvents.single().signUp.id)
  }

  @Test
  fun `intercept holds registered auth flow before syncing completed sign in client`() {
    val middleware = ClientSyncingMiddleware(json = ClerkApi.json)
    val registration = requireNotNull(Clerk.registerAuthFlow())
    try {
      val request =
        Request.Builder()
          .url("https://api.clerk.com/v1/client/sign_ins/si_123/attempt_first_factor")
          .post("".toRequestBody("application/x-www-form-urlencoded".toMediaType()))
          .build()
      val response =
        Response.Builder()
          .request(request)
          .protocol(Protocol.HTTP_1_1)
          .code(200)
          .message("OK")
          .body(completedSignInResponseBody())
          .build()
      val chain = mockk<Interceptor.Chain>()
      every { chain.request() } returns request
      every { chain.proceed(request) } returns response

      middleware.intercept(chain)

      assertEquals(Session.SessionStatus.ACTIVE, Clerk.session?.status)
      assertFalse(Clerk.isAuthFlowComplete)
      assertNotNull(Clerk.pendingAuthFlowCompletion)
    } finally {
      registration.close()
    }
  }

  @Test
  fun `intercept clears Clerk client when response client is explicit null`() {
    val middleware = ClientSyncingMiddleware(json = ClerkApi.json)
    val session = testSession("sess_123")
    Clerk.updateClient(
      Client(id = "client_123", sessions = listOf(session), lastActiveSessionId = session.id)
    )

    val request = Request.Builder().url("https://api.clerk.com/v1/client/sessions").build()

    val responseBody =
      """
      {
        "client": null
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

    middleware.intercept(chain)

    assertEquals(Client(), Clerk.client)
    assertEquals(emptyList<Session>(), Clerk.sessionsFlow.value)
  }

  @Test
  fun `intercept does not clear Clerk client when null client piggyback accompanies response`() {
    val middleware = ClientSyncingMiddleware(json = ClerkApi.json)
    val session = testSession("sess_123")
    val client =
      Client(id = "client_123", sessions = listOf(session), lastActiveSessionId = session.id)
    Clerk.updateClient(client)

    val request = Request.Builder().url("https://api.clerk.com/v1/client").build()

    val responseBody =
      """
      {
        "response": {
          "object": "client",
          "id": "client_123",
          "sessions": [],
          "last_active_session_id": null
        },
        "client": null
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

    middleware.intercept(chain)

    assertEquals(client, Clerk.client)
    assertEquals(listOf(session), Clerk.sessionsFlow.value)
  }

  private fun testSession(id: String): Session =
    Session(
      id = id,
      status = Session.SessionStatus.ACTIVE,
      expireAt = 10_000,
      lastActiveAt = 1_000,
      createdAt = 1_000,
      updatedAt = 1_000,
    )

  private fun completedSignInResponseBody() =
    """
    {
      "response": {
        "object": "sign_in_attempt",
        "id": "si_123",
        "status": "complete",
        "created_session_id": "sess_123"
      },
      "client": {
        "id": "client_123",
        "sessions": [{
          "id": "sess_123",
          "status": "active",
          "expire_at": 10000,
          "last_active_at": 1000,
          "user": {
            "has_image": false,
            "id": "user_123",
            "image_url": "https://example.com/avatar.png",
            "passkeys": [],
            "password_enabled": false,
            "phone_numbers": [],
            "totp_enabled": false,
            "two_factor_enabled": false,
            "updated_at": 1000
          },
          "created_at": 1000,
          "updated_at": 1000
        }],
        "last_active_session_id": "sess_123"
      }
    }
    """
      .trimIndent()
      .toResponseBody("application/json".toMediaType())
}
