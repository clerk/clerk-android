package com.clerk.api.network.middleware.outgoing

import com.clerk.api.configuration.DeviceBinding
import com.clerk.api.configuration.DeviceBindingState
import com.clerk.api.configuration.KEY_LOAD_GIVE_UP
import com.clerk.api.configuration.TRANSIENT_KEY_FAILURES_BEFORE_UNSIGNED
import com.clerk.api.devicekey.DeviceKey
import com.clerk.api.devicekey.DeviceKeyHolder
import com.clerk.api.devicekey.JvmDeviceKey
import com.clerk.api.dpop.ServerClock
import java.io.IOException
import java.security.ProviderException
import java.util.Base64
import java.util.concurrent.Callable
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TestTimeSource
import kotlin.time.TimeSource
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.asCoroutineDispatcher
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
  fun `three parallel waiters on one slow load count once, and a request after the load completes signs`() {
    val binding = CompletableDeferred<DeviceBinding>()
    val client = client(DeviceBindingState(binding))

    val errors =
      inParallel(3) { assertFailsWith<IOException> { execute(client, BOUND_DEVICE_TOKEN) } }
    errors.forEach { assertTrue("KEY_NOT_READY" in it.message.orEmpty(), it.message) }
    binding.complete(DeviceBinding.Signing(DeviceKeyHolder { JvmDeviceKey.getOrCreate() }))
    server.enqueue(MockResponse())
    execute(client, BOUND_DEVICE_TOKEN)

    assertEquals(1, server.requestCount)
    assertEquals(1, server.takeRequest().headers.values(DPOP_HEADER).size)
  }

  @Test
  fun `requests that find the load already timed out return at once without counting`() {
    val state = DeviceBindingState(CompletableDeferred())
    val client = client(state)
    assertFailsWith<IOException> { execute(client, BOUND_DEVICE_TOKEN) }

    repeat(TRANSIENT_KEY_FAILURES_BEFORE_UNSIGNED + 1) {
      val started = TimeSource.Monotonic.markNow()
      val error = assertFailsWith<IOException> { execute(client, BOUND_DEVICE_TOKEN) }
      assertTrue(started.elapsedNow() < AT_ONCE, "the request waited ${started.elapsedNow()}")
      assertTrue("KEY_NOT_READY" in error.message.orEmpty(), error.message)
    }
    server.enqueue(MockResponse())
    val started = TimeSource.Monotonic.markNow()
    execute(client, UNBOUND_DEVICE_TOKEN)

    assertTrue(started.elapsedNow() < AT_ONCE, "the request waited ${started.elapsedNow()}")
    assertNull(server.takeRequest().headers[DPOP_HEADER])
    assertEquals(timedOut(), runBlocking { state.current(wait) })
  }

  @Test
  fun `a load that completes at six seconds after one timeout resets the count and signs`() {
    val clock = TestTimeSource()
    val binding = CompletableDeferred<DeviceBinding>()
    val keyError = ProviderException("INVALID_KEY_BLOB")
    val key = ScriptedKey(keyError, keyError, null)
    val client = client(DeviceBindingState(binding, clock))

    assertFailsWith<IOException> { execute(client, BOUND_DEVICE_TOKEN) }
    clock += 6.seconds
    binding.complete(DeviceBinding.Signing(DeviceKeyHolder { key }))
    repeat(2) {
      val error = assertFailsWith<IOException> { execute(client, BOUND_DEVICE_TOKEN) }
      assertTrue("KEY_ERROR" in error.message.orEmpty(), error.message)
    }
    server.enqueue(MockResponse())
    execute(client, BOUND_DEVICE_TOKEN)

    assertEquals(1, server.takeRequest().headers.values(DPOP_HEADER).size)
    assertEquals(3, key.signs)
  }

  @Test
  fun `a load still pending past KEY_LOAD_GIVE_UP stops signing`() {
    val clock = TestTimeSource()
    val state = DeviceBindingState(CompletableDeferred(), clock)
    val client = client(state)

    assertFailsWith<IOException> { execute(client, BOUND_DEVICE_TOKEN) }
    clock += KEY_LOAD_GIVE_UP
    server.enqueue(MockResponse())
    execute(client, BOUND_DEVICE_TOKEN)

    val recorded = server.takeRequest()
    assertEquals(BOUND_DEVICE_TOKEN, recorded.headers["Authorization"])
    assertNull(recorded.headers[DPOP_HEADER])
    assertEquals(unsigned(), runBlocking { state.current(wait) })
  }

  @Test
  fun `a load that completes after KEY_LOAD_GIVE_UP resumes signing`() {
    val clock = TestTimeSource()
    val binding = CompletableDeferred<DeviceBinding>()
    val state = DeviceBindingState(binding, clock)
    val client = client(state)
    assertFailsWith<IOException> { execute(client, BOUND_DEVICE_TOKEN) }
    clock += KEY_LOAD_GIVE_UP
    server.enqueue(MockResponse())
    execute(client, BOUND_DEVICE_TOKEN)
    assertNull(server.takeRequest().headers[DPOP_HEADER])

    binding.complete(DeviceBinding.Signing(DeviceKeyHolder { JvmDeviceKey.getOrCreate() }))
    server.enqueue(MockResponse())
    execute(client, BOUND_DEVICE_TOKEN)

    assertEquals(1, server.takeRequest().headers.values(DPOP_HEADER).size)
  }

  @Test
  fun `a load that completes after KEY_LOAD_GIVE_UP with no request in between signs`() {
    val clock = TestTimeSource()
    val binding = CompletableDeferred<DeviceBinding>()
    val state = DeviceBindingState(binding, clock)

    clock += KEY_LOAD_GIVE_UP + 5.seconds
    binding.complete(DeviceBinding.Signing(DeviceKeyHolder { JvmDeviceKey.getOrCreate() }))
    server.enqueue(MockResponse())
    execute(client(state), BOUND_DEVICE_TOKEN)

    assertEquals(1, server.takeRequest().headers.values(DPOP_HEADER).size)
  }

  @Test
  fun `a request-time load that completes after KEY_LOAD_GIVE_UP resumes signing`() {
    val clock = TestTimeSource()
    val loadReleased = CountDownLatch(1)
    val holder = DeviceKeyHolder {
      loadReleased.await(HUNG_LOAD_SECONDS, TimeUnit.SECONDS)
      JvmDeviceKey.getOrCreate()
    }
    val state = DeviceBindingState(CompletableDeferred(DeviceBinding.Signing(holder)), clock)
    val client = client(state)
    assertFailsWith<IOException> { execute(client, BOUND_DEVICE_TOKEN) }
    clock += KEY_LOAD_GIVE_UP
    assertFailsWith<IOException> { execute(client, BOUND_DEVICE_TOKEN) }
    assertEquals(unsigned(), runBlocking { state.current(wait) })

    loadReleased.countDown()
    awaitBinding(state, DeviceBinding.Signing(holder))
    server.enqueue(MockResponse())
    execute(client, BOUND_DEVICE_TOKEN)

    assertEquals(1, server.takeRequest().headers.values(DPOP_HEADER).size)
  }

  @Test
  fun `a request-time load that completes between requests resets the count`() {
    val loadReleased = CountDownLatch(1)
    val keyError = ProviderException("INVALID_KEY_BLOB")
    val key = ScriptedKey(keyError, keyError, null)
    val holder = DeviceKeyHolder {
      loadReleased.await(HUNG_LOAD_SECONDS, TimeUnit.SECONDS)
      key
    }
    val loadThread = Executors.newSingleThreadExecutor()
    val client =
      client(
        DeviceBindingState(
          CompletableDeferred(DeviceBinding.Signing(holder)),
          ioDispatcher = loadThread.asCoroutineDispatcher(),
        )
      )

    val first = assertFailsWith<IOException> { execute(client, BOUND_DEVICE_TOKEN) }
    assertTrue("KEY_NOT_READY" in first.message.orEmpty(), first.message)
    loadReleased.countDown()
    awaitIdle(loadThread)
    repeat(2) {
      val error = assertFailsWith<IOException> { execute(client, BOUND_DEVICE_TOKEN) }
      assertTrue("KEY_ERROR" in error.message.orEmpty(), error.message)
    }
    server.enqueue(MockResponse())
    execute(client, BOUND_DEVICE_TOKEN)

    assertEquals(1, server.takeRequest().headers.values(DPOP_HEADER).size)
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
  fun `three sign failures in a row stop signing, counted across tokens`() {
    val keyError = ProviderException("INVALID_KEY_BLOB")
    val key = ScriptedKey(keyError, keyError, keyError)
    val client =
      client(
        DeviceBindingState(CompletableDeferred(DeviceBinding.Signing(DeviceKeyHolder { key })))
      )

    assertFailsWith<IOException> { execute(client, BOUND_DEVICE_TOKEN) }
    assertFailsWith<IOException> { execute(client, OTHER_BOUND_DEVICE_TOKEN) }
    assertFailsWith<IOException> { execute(client, BOUND_DEVICE_TOKEN) }
    server.enqueue(MockResponse())
    execute(client, OTHER_BOUND_DEVICE_TOKEN)

    val recorded = server.takeRequest()
    assertEquals(OTHER_BOUND_DEVICE_TOKEN, recorded.headers["Authorization"])
    assertNull(recorded.headers[DPOP_HEADER])
    assertEquals(3, key.signs)
  }

  @Test
  fun `a request-time key load that does not finish is waited on once, counted once, then signs`() {
    val loadReleased = CountDownLatch(1)
    val holder = DeviceKeyHolder {
      loadReleased.await(HUNG_LOAD_SECONDS, TimeUnit.SECONDS)
      JvmDeviceKey.getOrCreate()
    }
    val client = client(DeviceBindingState(CompletableDeferred(DeviceBinding.Signing(holder))))
    server.enqueue(MockResponse())

    try {
      repeat(TRANSIENT_KEY_FAILURES_BEFORE_UNSIGNED) { attempt ->
        val started = TimeSource.Monotonic.markNow()
        val error = assertFailsWith<IOException> { execute(client, BOUND_DEVICE_TOKEN) }
        val limit = if (attempt == 0) wait * 2 else AT_ONCE
        assertTrue(started.elapsedNow() < limit, "the request waited ${started.elapsedNow()}")
        assertTrue("KEY_NOT_READY" in error.message.orEmpty(), error.message)
      }
    } finally {
      loadReleased.countDown()
    }
    awaitLoaded(holder)
    execute(client, BOUND_DEVICE_TOKEN)

    assertEquals(1, server.requestCount)
    assertEquals(1, server.takeRequest().headers.values(DPOP_HEADER).size)
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

  private fun awaitLoaded(holder: DeviceKeyHolder) {
    val started = TimeSource.Monotonic.markNow()
    while (holder.loadedState() == null) {
      assertTrue(started.elapsedNow() < LOAD_DEADLINE, "the key did not load in $LOAD_DEADLINE")
      Thread.sleep(10)
    }
  }

  private fun awaitBinding(state: DeviceBindingState, expected: DeviceBinding) {
    val started = TimeSource.Monotonic.markNow()
    var seen = runBlocking { state.current(wait) }
    while (seen != expected) {
      assertTrue(started.elapsedNow() < LOAD_DEADLINE, "the binding was $seen after $LOAD_DEADLINE")
      Thread.sleep(10)
      seen = runBlocking { state.current(wait) }
    }
  }

  private fun awaitIdle(loadThread: ExecutorService) {
    val idle = CountDownLatch(1)
    loadThread.execute(idle::countDown)
    loadThread.shutdown()
    assertTrue(
      idle.await(LOAD_DEADLINE.inWholeMilliseconds, TimeUnit.MILLISECONDS),
      "the key load thread was still busy after $LOAD_DEADLINE",
    )
  }

  private fun unsigned(): DeviceBinding =
    DeviceBinding.NotSigning(DeviceBinding.Reason.KEY_UNAVAILABLE)

  private fun timedOut(): DeviceBinding = DeviceBinding.NotSigning(DeviceBinding.Reason.TIMEOUT)

  private fun <T> inParallel(count: Int, block: () -> T): List<T> {
    val pool = Executors.newFixedThreadPool(count)
    try {
      return List(count) { pool.submit(Callable(block)) }.map { it.get() }
    } finally {
      pool.shutdown()
    }
  }

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
    const val HUNG_LOAD_SECONDS = 3L
    val LOAD_DEADLINE = 5.seconds
    val AT_ONCE = 100.milliseconds
    val UNBOUND_DEVICE_TOKEN = jwt("""{"id":"client_unbound"}""")
    val OTHER_BOUND_DEVICE_TOKEN = jwt("""{"id":"client_other","cnf":{"jkt":"9YbNBNQYMXx"}}""")

    fun jwt(payload: String): String =
      listOf("""{"alg":"RS256"}""", payload).joinToString(".", postfix = ".signature") {
        Base64.getUrlEncoder().withoutPadding().encodeToString(it.toByteArray())
      }
  }
}
