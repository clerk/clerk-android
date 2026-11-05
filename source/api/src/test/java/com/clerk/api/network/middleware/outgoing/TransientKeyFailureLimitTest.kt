package com.clerk.api.network.middleware.outgoing

import com.clerk.api.configuration.DeviceBinding
import com.clerk.api.configuration.DeviceBindingState
import com.clerk.api.devicekey.DeviceKey
import com.clerk.api.devicekey.DeviceKeyHolder
import com.clerk.api.devicekey.JvmDeviceKey
import com.clerk.api.dpop.ServerClock
import java.io.IOException
import java.security.ProviderException
import java.util.Base64
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TimeSource
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.OkHttpClient
import okhttp3.Request
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class TransientKeyFailureLimitTest {
  private val server = MockWebServer()
  private val wait = 500.milliseconds

  @Before
  fun setUp() {
    server.start()
  }

  @After
  fun tearDown() {
    server.close()
  }

  @Test
  fun `three timeouts in a row stop signing, and the next request goes out unsigned at once`() {
    val state = DeviceBindingState(CompletableDeferred())
    val client = client(state)

    repeat(3) {
      val error = assertFailsWith<IOException> { execute(client, BOUND_DEVICE_TOKEN) }
      assertTrue("KEY_NOT_READY" in error.message.orEmpty(), error.message)
    }
    server.enqueue(MockResponse())
    val started = TimeSource.Monotonic.markNow()
    execute(client, BOUND_DEVICE_TOKEN)

    assertTrue(started.elapsedNow() < wait, "the request waited ${started.elapsedNow()}")
    val recorded = server.takeRequest()
    assertEquals(BOUND_DEVICE_TOKEN, recorded.headers["Authorization"])
    assertNull(recorded.headers[DPOP_HEADER])
    assertEquals(unsigned(), runBlocking { state.current(wait) })
  }

  @Test
  fun `a successful sign resets the count, so two failures after it still fail the request`() {
    val keyError = ProviderException("INVALID_KEY_BLOB")
    val key = ScriptedKey(keyError, keyError, null, keyError, keyError)
    val state =
      DeviceBindingState(CompletableDeferred(DeviceBinding.Signing(DeviceKeyHolder { key })))
    val client = client(state)
    server.enqueue(MockResponse())

    repeat(2) { assertFailsWith<IOException> { execute(client, BOUND_DEVICE_TOKEN) } }
    execute(client, BOUND_DEVICE_TOKEN)
    repeat(2) {
      val error = assertFailsWith<IOException> { execute(client, BOUND_DEVICE_TOKEN) }
      assertTrue("KEY_ERROR" in error.message.orEmpty(), error.message)
    }

    assertEquals(1, server.requestCount)
    assertEquals(1, server.takeRequest().headers.values(DPOP_HEADER).size)
    assertEquals(5, key.signs)
  }

  @Test
  fun `the count runs across tokens`() {
    val state = DeviceBindingState(CompletableDeferred())
    val client = client(state)

    assertFailsWith<IOException> { execute(client, BOUND_DEVICE_TOKEN) }
    assertFailsWith<IOException> { execute(client, OTHER_BOUND_DEVICE_TOKEN) }
    assertFailsWith<IOException> { execute(client, BOUND_DEVICE_TOKEN) }
    server.enqueue(MockResponse())
    execute(client, OTHER_BOUND_DEVICE_TOKEN)

    val recorded = server.takeRequest()
    assertEquals(OTHER_BOUND_DEVICE_TOKEN, recorded.headers["Authorization"])
    assertNull(recorded.headers[DPOP_HEADER])
  }

  private class ScriptedKey(
    vararg failures: Exception?,
    private val key: DeviceKey = JvmDeviceKey.getOrCreate(),
  ) : DeviceKey by key {
    private val script = ArrayDeque(failures.toList())
    var signs = 0

    override fun sign(data: ByteArray): ByteArray {
      signs += 1
      script.removeFirst()?.let { throw it }
      return key.sign(data)
    }
  }

  private fun unsigned(): DeviceBinding =
    DeviceBinding.NotSigning(DeviceBinding.Reason.KEY_UNAVAILABLE)

  private fun client(state: DeviceBindingState, deviceKeyWait: Duration = wait): OkHttpClient =
    OkHttpClient.Builder()
      .addNetworkInterceptor(DpopMiddleware(state, ServerClock(Clock.System), deviceKeyWait))
      .build()

  private fun execute(client: OkHttpClient, token: String) {
    client
      .newCall(
        Request(server.url("/v1/client")).newBuilder().header("Authorization", token).build()
      )
      .execute()
      .close()
  }

  private companion object {
    val BOUND_DEVICE_TOKEN = jwt("""{"id":"client_bound","cnf":{"jkt":"0ZcOCORZNYy"}}""")
    val OTHER_BOUND_DEVICE_TOKEN = jwt("""{"id":"client_other","cnf":{"jkt":"9YbNBNQYMXx"}}""")

    fun jwt(payload: String): String =
      listOf("""{"alg":"RS256"}""", payload).joinToString(".", postfix = ".signature") {
        Base64.getUrlEncoder().withoutPadding().encodeToString(it.toByteArray())
      }
  }
}
