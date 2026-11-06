package com.clerk.api.network.middleware.outgoing

import android.content.Context
import com.clerk.api.Clerk
import com.clerk.api.configuration.ConfiguredClerkForTest
import com.clerk.api.configuration.DeviceBinding
import com.clerk.api.configuration.DeviceBindingState
import com.clerk.api.devicekey.DeviceKeyHolder
import com.clerk.api.devicekey.JvmDeviceKey
import com.clerk.api.dpop.ServerClock
import com.clerk.api.network.ClerkApi
import com.clerk.api.network.model.client.Client
import com.clerk.api.network.model.environment.Environment
import com.clerk.api.network.serialization.ClerkResult
import com.clerk.api.storage.StorageHelper
import com.clerk.api.storage.StorageKey
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockkObject
import io.mockk.unmockkAll
import java.io.IOException
import java.util.Base64
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.tls.HandshakeCertificates
import okhttp3.tls.HeldCertificate
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class DeviceBoundTokenFallbackTest {
  private val certificate = HeldCertificate.Builder().addSubjectAlternativeName("localhost").build()
  private val server = MockWebServer()
  private val holder = DeviceKeyHolder { JvmDeviceKey.getOrCreate() }
  private val context: Context = RuntimeEnvironment.getApplication()
  private val releaseRefresh = CompletableDeferred<Unit>()

  @Before
  fun setUp() {
    StorageHelper.reset(context)
    Clerk.updateClient(Client())
    ConfiguredClerkForTest.setConfigured(context, true)
    mockkObject(Client.Companion, Environment.Companion)
    coEvery { Client.getSkippingClientId() } coAnswers
      {
        releaseRefresh.await()
        ClerkResult.success(Client(id = "client_new"))
      }
    coEvery { Environment.get() } returns ClerkResult.unknownFailure(IllegalStateException())
    server.useHttps(
      HandshakeCertificates.Builder().heldCertificate(certificate).build().sslSocketFactory()
    )
    server.start()
  }

  @After
  fun tearDown() {
    releaseRefresh.complete(Unit)
    ConfiguredClerkForTest.setConfigured(context, false)
    unmockkAll()
    StorageHelper.reset(context)
    server.close()
    Clerk.updateClient(Client())
    Clerk.clearSessionAndUserState()
    Clerk.stateStore.reset()
  }

  @Test
  fun `a key still loading at cold start fails a bound token's request, keeps it, then signs`() {
    StorageHelper.saveValue(StorageKey.DEVICE_TOKEN, BOUND_DEVICE_TOKEN)
    val binding = CompletableDeferred<DeviceBinding>()
    val client = client(binding, deviceKeyWait = 100.milliseconds)

    assertFailsWith<IOException> { execute(client).close() }

    assertEquals(0, server.requestCount)
    assertEquals(BOUND_DEVICE_TOKEN, StorageHelper.loadValue(StorageKey.DEVICE_TOKEN))
    coVerify(exactly = 0) { Client.getSkippingClientId() }

    binding.complete(DeviceBinding.Signing(holder))
    server.enqueue(MockResponse())
    execute(client).use { assertEquals(200, it.code) }

    val recorded = server.takeRequest()
    assertEquals(BOUND_DEVICE_TOKEN, recorded.headers["Authorization"])
    assertEquals(1, recorded.headers.values(DPOP_HEADER).size)
  }

  @Test
  fun `a bound token with no usable key goes out unsigned and proof_missing drops it once`() {
    StorageHelper.saveValue(StorageKey.DEVICE_TOKEN, BOUND_DEVICE_TOKEN)
    server.enqueue(proofMissing())

    execute(client(keyUnavailable())).use { assertEquals(401, it.code) }

    val recorded = server.takeRequest()
    assertEquals(BOUND_DEVICE_TOKEN, recorded.headers["Authorization"])
    assertNull(recorded.headers[DPOP_HEADER])
    assertNull(StorageHelper.loadValue(StorageKey.DEVICE_TOKEN))
    coVerify(timeout = 5_000, exactly = 1) { Client.getSkippingClientId() }
    assertEquals(1, server.requestCount)
  }

  @Test
  fun `an unbound token with no usable key goes out unsigned, is served, and is kept`() {
    StorageHelper.saveValue(StorageKey.DEVICE_TOKEN, UNBOUND_DEVICE_TOKEN)
    server.enqueue(MockResponse())

    execute(client(keyUnavailable())).use { assertEquals(200, it.code) }

    val recorded = server.takeRequest()
    assertEquals(UNBOUND_DEVICE_TOKEN, recorded.headers["Authorization"])
    assertNull(recorded.headers[DPOP_HEADER])
    assertEquals(UNBOUND_DEVICE_TOKEN, StorageHelper.loadValue(StorageKey.DEVICE_TOKEN))
    coVerify(exactly = 0) { Client.getSkippingClientId() }
  }

  private fun keyUnavailable(): Deferred<DeviceBinding> =
    CompletableDeferred(DeviceBinding.NotSigning(DeviceBinding.Reason.KEY_UNAVAILABLE))

  private fun proofMissing(): MockResponse =
    MockResponse.Builder()
      .code(401)
      .addHeader("Content-Type", "application/json")
      .body("""{"errors":[{"code":"device_binding_proof_missing","message":"rejected"}]}""")
      .build()

  private fun client(
    binding: Deferred<DeviceBinding>,
    deviceKeyWait: Duration = 5_000.milliseconds,
  ): OkHttpClient {
    val trusted =
      HandshakeCertificates.Builder().addTrustedCertificate(certificate.certificate).build()
    return ClerkApi.buildHttpClient(
        customHeaders = emptyMap(),
        deviceBinding = DeviceBindingState(binding),
        serverClock = ServerClock(Clock.System),
        deviceKeyWait = deviceKeyWait,
      )
      .newBuilder()
      .sslSocketFactory(trusted.sslSocketFactory(), trusted.trustManager)
      .build()
  }

  private fun execute(client: OkHttpClient): Response =
    client.newCall(Request(server.url("/v1/client"))).execute()

  private companion object {
    val BOUND_DEVICE_TOKEN = jwt("""{"id":"client_bound","cnf":{"jkt":"0ZcOCORZNYy"}}""")
    val UNBOUND_DEVICE_TOKEN = jwt("""{"id":"client_unbound"}""")

    fun jwt(payload: String): String =
      listOf("""{"alg":"RS256"}""", payload).joinToString(".", postfix = ".signature") {
        Base64.getUrlEncoder().withoutPadding().encodeToString(it.toByteArray())
      }
  }
}
