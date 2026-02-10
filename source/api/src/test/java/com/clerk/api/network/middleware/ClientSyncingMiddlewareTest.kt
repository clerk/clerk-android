package com.clerk.api.network.middleware

import android.util.Log
import com.clerk.api.Clerk
import com.clerk.api.network.middleware.incoming.ClientSyncingMiddleware
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ClientSyncingMiddlewareTest {

  private val json = Json {
    ignoreUnknownKeys = true
    isLenient = true
  }

  private lateinit var middleware: ClientSyncingMiddleware

  @Before
  fun setup() {
    middleware = ClientSyncingMiddleware(json)
    mockkStatic(Log::class)
    every { Log.w(any(), any<String>()) } returns 0
    every { Log.d(any(), any<String>()) } returns 0
    every { Log.e(any(), any<String>()) } returns 0
    every { Log.i(any(), any<String>()) } returns 0
    every { Log.v(any(), any<String>()) } returns 0
    mockkObject(Clerk)
    every { Clerk.updateClient(any()) } returns Unit
  }

  @After
  fun tearDown() {
    unmockkAll()
  }

  @Test
  fun `logs warning when sign-in is complete but session is pending`() {
    // Given
    val sessionId = "session_123"
    val responseJson =
      """
      {
        "client": {
          "id": "client_123",
          "sign_in": {
            "id": "sign_in_123",
            "status": "complete",
            "created_session_id": "$sessionId"
          },
          "sessions": [
            {
              "id": "$sessionId",
              "status": "pending",
              "expire_at": 1234567890,
              "last_active_at": 1234567890,
              "created_at": 1234567890,
              "updated_at": 1234567890
            }
          ]
        }
      }
      """
        .trimIndent()

    val chain = createMockChain(responseJson)

    // When
    middleware.intercept(chain)

    // Then
    val warningSlot = slot<String>()
    verify { Log.w("ClerkLog", capture(warningSlot)) }
    assertTrue(warningSlot.captured.contains("Sign-in completed but session is pending"))
  }

  @Test
  fun `logs warning when sign-up is complete but session is pending`() {
    // Given
    val sessionId = "session_456"
    val responseJson =
      """
      {
        "client": {
          "id": "client_123",
          "sign_up": {
            "id": "sign_up_123",
            "status": "complete",
            "created_session_id": "$sessionId",
            "required_fields": [],
            "optional_fields": [],
            "missing_fields": [],
            "unverified_fields": [],
            "verifications": {},
            "password_enabled": false
          },
          "sessions": [
            {
              "id": "$sessionId",
              "status": "pending",
              "expire_at": 1234567890,
              "last_active_at": 1234567890,
              "created_at": 1234567890,
              "updated_at": 1234567890
            }
          ]
        }
      }
      """
        .trimIndent()

    val chain = createMockChain(responseJson)

    // When
    middleware.intercept(chain)

    // Then
    val warningSlot = slot<String>()
    verify { Log.w("ClerkLog", capture(warningSlot)) }
    assertTrue(warningSlot.captured.contains("Sign-up completed but session is pending"))
  }

  @Test
  fun `does not log warning when sign-in is complete and session is active`() {
    // Given
    val sessionId = "session_123"
    val responseJson =
      """
      {
        "client": {
          "id": "client_123",
          "sign_in": {
            "id": "sign_in_123",
            "status": "complete",
            "created_session_id": "$sessionId"
          },
          "sessions": [
            {
              "id": "$sessionId",
              "status": "active",
              "expire_at": 1234567890,
              "last_active_at": 1234567890,
              "created_at": 1234567890,
              "updated_at": 1234567890
            }
          ]
        }
      }
      """
        .trimIndent()

    val chain = createMockChain(responseJson)

    // When
    middleware.intercept(chain)

    // Then - verify no warning was logged (only debug log for client synced)
    verify(exactly = 0) {
      Log.w("ClerkLog", match<String> { it.contains("Sign-in completed but session is pending") })
    }
  }

  @Test
  fun `does not log warning when sign-in is not complete`() {
    // Given
    val responseJson =
      """
      {
        "client": {
          "id": "client_123",
          "sign_in": {
            "id": "sign_in_123",
            "status": "needs_first_factor"
          },
          "sessions": []
        }
      }
      """
        .trimIndent()

    val chain = createMockChain(responseJson)

    // When
    middleware.intercept(chain)

    // Then - verify no warning was logged
    verify(exactly = 0) {
      Log.w("ClerkLog", match<String> { it.contains("Sign-in completed but session is pending") })
    }
  }

  @Test
  fun `does not log warning when no client in response`() {
    // Given
    val responseJson =
      """
      {
        "data": "some other response"
      }
      """
        .trimIndent()

    val chain = createMockChain(responseJson)

    // When
    middleware.intercept(chain)

    // Then - verify no warning was logged
    verify(exactly = 0) { Log.w(any(), any<String>()) }
  }

  private fun createMockChain(responseJson: String): Interceptor.Chain {
    val request = Request.Builder().url("https://api.clerk.dev/test").build()

    val responseBody = responseJson.toResponseBody("application/json".toMediaType())

    val response =
      Response.Builder()
        .request(request)
        .protocol(Protocol.HTTP_1_1)
        .code(200)
        .message("OK")
        .body(responseBody)
        .build()

    return object : Interceptor.Chain {
      override fun request(): Request = request

      override fun proceed(request: Request): Response = response

      override fun connection() = null

      override fun call() = throw UnsupportedOperationException()

      override fun connectTimeoutMillis() = 0

      override fun withConnectTimeout(timeout: Int, unit: java.util.concurrent.TimeUnit) = this

      override fun readTimeoutMillis() = 0

      override fun withReadTimeout(timeout: Int, unit: java.util.concurrent.TimeUnit) = this

      override fun writeTimeoutMillis() = 0

      override fun withWriteTimeout(timeout: Int, unit: java.util.concurrent.TimeUnit) = this
    }
  }
}
