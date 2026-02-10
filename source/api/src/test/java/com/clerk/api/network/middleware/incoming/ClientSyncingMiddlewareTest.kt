package com.clerk.api.network.middleware.incoming

import android.content.Context
import com.clerk.api.Clerk
import com.clerk.api.log.ClerkLog
import com.clerk.api.network.model.environment.DisplayConfig
import com.clerk.api.network.model.environment.Environment
import com.clerk.api.network.model.environment.UserSettings
import com.clerk.api.storage.StorageHelper
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class ClientSyncingMiddlewareTest {
  private val testDispatcher = StandardTestDispatcher()
  private lateinit var context: Context
  private lateinit var mockEnvironment: Environment
  private lateinit var middleware: ClientSyncingMiddleware
  private val json = Json { ignoreUnknownKeys = true }

  @OptIn(ExperimentalCoroutinesApi::class)
  @Before
  fun setup() {
    Dispatchers.setMain(testDispatcher)
    context = RuntimeEnvironment.getApplication()

    // Initialize StorageHelper with real context
    StorageHelper.initialize(context)
    StorageHelper.reset(context)

    // Create middleware
    middleware = ClientSyncingMiddleware(json)

    // Setup environment mock
    mockEnvironment = mockk()
    val mockDisplayConfig = mockk<DisplayConfig>()
    val mockUserSettings = mockk<UserSettings>()
    every { mockEnvironment.displayConfig } returns mockDisplayConfig
    every { mockEnvironment.userSettings } returns mockUserSettings
    every { mockDisplayConfig.logoImageUrl } returns ""
    every { mockDisplayConfig.applicationName } returns ""
    every { mockUserSettings.social } returns emptyMap()
    every { mockEnvironment.passkeyIsEnabled } returns false
    every { mockEnvironment.mfaIsEnabled } returns false
    every { mockEnvironment.mfaAuthenticatorAppIsEnabled } returns false
    every { mockEnvironment.passwordIsEnabled } returns false
    every { mockEnvironment.usernameIsEnabled } returns false
    every { mockEnvironment.firstNameIsEnabled } returns false
    every { mockEnvironment.lastNameIsEnabled } returns false
    every { mockUserSettings.actions } returns mockk { every { deleteSelf } returns false }

    Clerk.environment = mockEnvironment

    // Mock ClerkLog
    mockkObject(ClerkLog)
    every { ClerkLog.d(any()) } returns 0
    every { ClerkLog.w(any()) } returns 0
    every { ClerkLog.e(any()) } returns 0
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  @After
  fun tearDown() {
    Dispatchers.resetMain()
    unmockkAll()
    StorageHelper.reset(context)
  }

  private fun createMockChain(responseBody: String): Interceptor.Chain {
    val request = Request.Builder().url("https://api.clerk.com/v1/test").build()

    val response =
      Response.Builder()
        .request(request)
        .protocol(Protocol.HTTP_1_1)
        .code(200)
        .message("OK")
        .body(responseBody.toResponseBody("application/json".toMediaType()))
        .build()

    val chain = mockk<Interceptor.Chain>()
    every { chain.request() } returns request
    every { chain.proceed(any()) } returns response

    return chain
  }

  @Test
  fun `logs warning when sign-in is complete but session is pending`() {
    // Given - sign-in complete with pending session
    val clientJson =
      """
      {
        "client": {
          "id": "client_123",
          "sign_in": {
            "id": "signin_123",
            "status": "complete",
            "created_session_id": "sess_123"
          },
          "sessions": [
            {
              "id": "sess_123",
              "status": "pending",
              "expire_at": ${System.currentTimeMillis() + 3600000},
              "last_active_at": ${System.currentTimeMillis()},
              "created_at": ${System.currentTimeMillis()},
              "updated_at": ${System.currentTimeMillis()}
            }
          ]
        }
      }
      """
        .trimIndent()

    val chain = createMockChain(clientJson)

    // When
    middleware.intercept(chain)

    // Then
    verify { ClerkLog.w(match { it.contains("Sign-in completed but the session is pending") }) }
  }

  @Test
  fun `logs warning when sign-up is complete but session is pending`() {
    // Given - sign-up complete with pending session
    val clientJson =
      """
      {
        "client": {
          "id": "client_123",
          "sign_up": {
            "id": "signup_123",
            "status": "complete",
            "required_fields": [],
            "optional_fields": [],
            "missing_fields": [],
            "unverified_fields": [],
            "verifications": {},
            "password_enabled": false,
            "created_session_id": "sess_123"
          },
          "sessions": [
            {
              "id": "sess_123",
              "status": "pending",
              "expire_at": ${System.currentTimeMillis() + 3600000},
              "last_active_at": ${System.currentTimeMillis()},
              "created_at": ${System.currentTimeMillis()},
              "updated_at": ${System.currentTimeMillis()}
            }
          ]
        }
      }
      """
        .trimIndent()

    val chain = createMockChain(clientJson)

    // When
    middleware.intercept(chain)

    // Then
    verify { ClerkLog.w(match { it.contains("Sign-up completed but the session is pending") }) }
  }

  @Test
  fun `does not log warning when sign-in is complete and session is active`() {
    // Given - sign-in complete with active session
    val clientJson =
      """
      {
        "client": {
          "id": "client_123",
          "sign_in": {
            "id": "signin_123",
            "status": "complete",
            "created_session_id": "sess_123"
          },
          "sessions": [
            {
              "id": "sess_123",
              "status": "active",
              "expire_at": ${System.currentTimeMillis() + 3600000},
              "last_active_at": ${System.currentTimeMillis()},
              "created_at": ${System.currentTimeMillis()},
              "updated_at": ${System.currentTimeMillis()}
            }
          ]
        }
      }
      """
        .trimIndent()

    val chain = createMockChain(clientJson)

    // When
    middleware.intercept(chain)

    // Then - should not log any warning about pending session
    verify(exactly = 0) { ClerkLog.w(match { it.contains("pending") }) }
  }

  @Test
  fun `does not log warning when sign-in is not complete`() {
    // Given - sign-in needs first factor
    val clientJson =
      """
      {
        "client": {
          "id": "client_123",
          "sign_in": {
            "id": "signin_123",
            "status": "needs_first_factor"
          },
          "sessions": []
        }
      }
      """
        .trimIndent()

    val chain = createMockChain(clientJson)

    // When
    middleware.intercept(chain)

    // Then - should not log any warning
    verify(exactly = 0) { ClerkLog.w(match { it.contains("pending") }) }
  }

  @Test
  fun `does not log warning when sign-up is not complete`() {
    // Given - sign-up missing requirements
    val clientJson =
      """
      {
        "client": {
          "id": "client_123",
          "sign_up": {
            "id": "signup_123",
            "status": "missing_requirements",
            "required_fields": ["email_address"],
            "optional_fields": [],
            "missing_fields": ["email_address"],
            "unverified_fields": [],
            "verifications": {},
            "password_enabled": false
          },
          "sessions": []
        }
      }
      """
        .trimIndent()

    val chain = createMockChain(clientJson)

    // When
    middleware.intercept(chain)

    // Then - should not log any warning
    verify(exactly = 0) { ClerkLog.w(match { it.contains("pending") }) }
  }

  @Test
  fun `does not log warning when no sign-in or sign-up present`() {
    // Given - client with no sign-in or sign-up
    val clientJson =
      """
      {
        "client": {
          "id": "client_123",
          "sessions": [
            {
              "id": "sess_123",
              "status": "active",
              "expire_at": ${System.currentTimeMillis() + 3600000},
              "last_active_at": ${System.currentTimeMillis()},
              "created_at": ${System.currentTimeMillis()},
              "updated_at": ${System.currentTimeMillis()}
            }
          ]
        }
      }
      """
        .trimIndent()

    val chain = createMockChain(clientJson)

    // When
    middleware.intercept(chain)

    // Then - should not log any warning
    verify(exactly = 0) { ClerkLog.w(match { it.contains("pending") }) }
  }
}
