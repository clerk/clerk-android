package com.clerk.api.network.middleware.incoming

import android.content.Context
import com.clerk.api.Clerk
import com.clerk.api.Constants.Http.AUTHORIZATION_HEADER
import com.clerk.api.configuration.ConfiguredClerkForTest
import com.clerk.api.dpop.ServerClock
import com.clerk.api.network.ClerkApi
import com.clerk.api.network.model.client.Client
import com.clerk.api.network.model.environment.Environment
import com.clerk.api.network.serialization.ClerkResult
import com.clerk.api.storage.StorageHelper
import com.clerk.api.storage.StorageKey
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.coroutines.CompletableDeferred
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
class ClientSyncingMiddlewareDeviceBindingTest {
  private val context: Context = RuntimeEnvironment.getApplication()
  private val localClock =
    object : Clock {
      override fun now(): Instant = Instant.parse("2026-11-04T12:00:00Z")
    }
  private val serverClock = ServerClock(localClock)
  private val middleware = ClientSyncingMiddleware(json = ClerkApi.json, serverClock = serverClock)
  private val releaseRefresh = CompletableDeferred<Unit>()

  @Before
  fun setUp() {
    StorageHelper.reset(context)
    mockkObject(Client.Companion, Environment.Companion)
    coEvery { Client.getSkippingClientId() } coAnswers
      {
        releaseRefresh.await()
        ClerkResult.success(Client(id = "client_new"))
      }
    coEvery { Environment.get() } returns ClerkResult.unknownFailure(IllegalStateException())
  }

  @After
  fun tearDown() {
    releaseRefresh.complete(Unit)
    ConfiguredClerkForTest.setConfigured(context, false)
    unmockkAll()
    StorageHelper.reset(context)
    Clerk.updateClient(Client())
    Clerk.clearSessionAndUserState()
    Clerk.stateStore.reset()
  }

  @Test
  fun `the response Date reaches the server clock`() {
    middleware.intercept(chainFor(response(code = 200, date = "Wed, 04 Nov 2026 12:20:00 GMT")))

    assertEquals(Instant.parse("2026-11-04T12:20:00Z"), serverClock.now())
  }

  @Test
  fun `a rejected request's Date reaches the server clock`() {
    middleware.intercept(chainFor(response(code = 401, date = "Wed, 04 Nov 2026 11:40:00 GMT")))

    assertEquals(Instant.parse("2026-11-04T11:40:00Z"), serverClock.now())
  }

  @Test
  fun `an obsolete RFC 850 Date reaches the server clock`() {
    middleware.intercept(chainFor(response(code = 200, date = "Wednesday, 04-Nov-26 12:20:00 GMT")))

    assertEquals(Instant.parse("2026-11-04T12:20:00Z"), serverClock.now())
  }

  @Test
  fun `an unparsable Date leaves the server clock offset unchanged`() {
    middleware.intercept(chainFor(response(code = 200, date = "Wed, 04 Nov 2026 12:20:00 GMT")))
    middleware.intercept(chainFor(response(code = 200, date = "yesterday at noon")))

    assertEquals(Instant.parse("2026-11-04T12:20:00Z"), serverClock.now())
  }

  @Test
  fun `key_mismatch drops the token through the fence, signs out, and refreshes the client`() {
    assertRejectedTokenIsDropped("device_binding_key_mismatch")
  }

  @Test
  fun `proof_missing drops the token through the fence, signs out, and refreshes the client`() {
    assertRejectedTokenIsDropped("device_binding_proof_missing")
  }

  @Test
  fun `a rejection that carries a rotated token drops the rotated token`() {
    ConfiguredClerkForTest.setConfigured(context, true)
    StorageHelper.saveValue(StorageKey.DEVICE_TOKEN, "token_rotated")

    middleware.intercept(
      chainFor(
        rejection("device_binding_key_mismatch", "token_bound")
          .newBuilder()
          .header(AUTHORIZATION_HEADER, "token_rotated")
          .build()
      )
    )

    assertNull(StorageHelper.loadValue(StorageKey.DEVICE_TOKEN))
    coVerify(timeout = 5_000) { Client.getSkippingClientId() }
  }

  @Test
  fun `a rejection for a token that was already replaced leaves the new token alone`() {
    ConfiguredClerkForTest.setConfigured(context, true)
    StorageHelper.saveValue(StorageKey.DEVICE_TOKEN, "token_new")
    Clerk.updateClient(Client(id = "client_new"))
    val fence = ConfiguredClerkForTest.fenceGeneration()

    middleware.intercept(chainFor(rejection("device_binding_key_mismatch", "token_bound")))

    assertEquals("token_new", StorageHelper.loadValue(StorageKey.DEVICE_TOKEN))
    assertEquals(fence, ConfiguredClerkForTest.fenceGeneration())
    assertEquals("client_new", Clerk.clientFlow.value?.id)
    coVerify(exactly = 0) { Client.getSkippingClientId() }
  }

  @Test
  fun `proof_invalid keeps the token`() {
    ConfiguredClerkForTest.setConfigured(context, true)
    StorageHelper.saveValue(StorageKey.DEVICE_TOKEN, "token_bound")

    middleware.intercept(chainFor(rejection("device_binding_proof_invalid", "token_bound")))

    assertEquals("token_bound", StorageHelper.loadValue(StorageKey.DEVICE_TOKEN))
    coVerify(exactly = 0) { Client.getSkippingClientId() }
  }

  private fun assertRejectedTokenIsDropped(code: String) {
    ConfiguredClerkForTest.setConfigured(context, true)
    StorageHelper.saveValue(StorageKey.DEVICE_TOKEN, "token_bound")
    Clerk.updateClient(Client(id = "client_bound"))
    val fence = ConfiguredClerkForTest.fenceGeneration()

    middleware.intercept(chainFor(rejection(code, "token_bound")))

    assertNull(StorageHelper.loadValue(StorageKey.DEVICE_TOKEN))
    assertEquals(fence + 1, ConfiguredClerkForTest.fenceGeneration())
    assertNull(Clerk.clientFlow.value?.id)
    coVerify(timeout = 5_000) { Client.getSkippingClientId() }
  }

  private fun response(code: Int, date: String): Response =
    Response.Builder()
      .request(Request.Builder().url("https://api.clerk.com/v1/environment").build())
      .protocol(Protocol.HTTP_1_1)
      .code(code)
      .message("")
      .header("Date", date)
      .body("{}".toResponseBody("application/json".toMediaType()))
      .build()

  private fun rejection(code: String, requestToken: String): Response =
    Response.Builder()
      .request(
        Request.Builder()
          .url("https://api.clerk.com/v1/client/sessions/sess_1/tokens")
          .header(AUTHORIZATION_HEADER, requestToken)
          .build()
      )
      .protocol(Protocol.HTTP_1_1)
      .code(401)
      .message("")
      .body("""{"errors":[{"code":"$code"}]}""".toResponseBody("application/json".toMediaType()))
      .build()

  private fun chainFor(response: Response): Interceptor.Chain =
    mockk<Interceptor.Chain>().also {
      every { it.request() } returns response.request
      every { it.proceed(response.request) } returns response
    }
}
