package com.clerk.api.network.middleware.outgoing

import android.content.Context
import com.clerk.api.Clerk
import com.clerk.api.configuration.DeviceBinding
import com.clerk.api.configuration.DeviceBindingState
import com.clerk.api.devicekey.DeviceKeyHolder
import com.clerk.api.devicekey.JvmDeviceKey
import com.clerk.api.dpop.ServerClock
import com.clerk.api.network.ClerkApi
import com.clerk.api.network.model.client.Client
import com.clerk.api.storage.StorageHelper
import com.nimbusds.jwt.SignedJWT
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import mockwebserver3.RecordedRequest
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import okhttp3.tls.HandshakeCertificates
import okhttp3.tls.HeldCertificate
import okio.BufferedSink
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class DpopRetryMiddlewareTest {
  private val certificate = HeldCertificate.Builder().addSubjectAlternativeName("localhost").build()
  private val server = MockWebServer()
  private val holder = DeviceKeyHolder { JvmDeviceKey.getOrCreate() }
  private val localClock =
    object : Clock {
      override fun now(): Instant = Instant.parse("2026-11-04T12:00:00Z")
    }
  private val serverClock = ServerClock(localClock)
  private val context: Context = RuntimeEnvironment.getApplication()

  @Before
  fun setUp() {
    StorageHelper.reset(context)
    server.useHttps(
      HandshakeCertificates.Builder().heldCertificate(certificate).build().sslSocketFactory()
    )
    server.start()
  }

  @After
  fun tearDown() {
    StorageHelper.reset(context)
    server.close()
    Clerk.updateClient(Client())
    Clerk.clearSessionAndUserState()
    Clerk.stateStore.reset()
  }

  @Test
  fun `the SDK client signs a request`() {
    server.enqueue(MockResponse())

    execute(signing()).close()

    assertEquals(1, server.takeRequest().headers.values(DPOP_HEADER).size)
  }

  @Test
  fun `a proof rejected as invalid is retried once with the rejection's Date`() {
    server.enqueue(
      rejection("device_binding_proof_invalid", date = "Wed, 04 Nov 2026 12:20:00 GMT")
    )
    server.enqueue(MockResponse())

    val response = execute(signing())

    assertEquals(200, response.code)
    response.close()
    val rejected = claims(server.takeRequest())
    val retried = claims(server.takeRequest())
    assertEquals(Instant.parse("2026-11-04T12:00:00Z").epochSeconds, rejected.issueTime.time / 1000)
    assertEquals(Instant.parse("2026-11-04T12:20:00Z").epochSeconds, retried.issueTime.time / 1000)
    assertNotEquals(rejected.jwtid, retried.jwtid)
  }

  @Test
  fun `a request with a one-shot body is not retried`() {
    server.enqueue(
      rejection("device_binding_proof_invalid", date = "Wed, 04 Nov 2026 12:20:00 GMT")
    )
    server.enqueue(MockResponse())
    val oneShotBody =
      object : RequestBody() {
        override fun contentType() = null

        override fun isOneShot() = true

        override fun writeTo(sink: BufferedSink) {
          sink.writeUtf8("once")
        }
      }

    execute(signing(), Request(server.url("/v1/client"), method = "POST", body = oneShotBody)).use {
      assertEquals(401, it.code)
    }

    assertEquals(1, server.requestCount)
  }

  @Test
  fun `a rejection without a Date is not retried`() {
    server.enqueue(rejection("device_binding_proof_invalid", date = null))

    execute(signing()).use { assertEquals(401, it.code) }

    assertEquals(1, server.requestCount)
  }

  @Test
  fun `a second invalid proof is not retried again`() {
    server.enqueue(
      rejection("device_binding_proof_invalid", date = "Wed, 04 Nov 2026 12:20:00 GMT")
    )
    server.enqueue(
      rejection("device_binding_proof_invalid", date = "Wed, 04 Nov 2026 12:20:01 GMT")
    )
    server.enqueue(MockResponse())

    execute(signing()).use { assertEquals(401, it.code) }

    assertEquals(2, server.requestCount)
  }

  @Test
  fun `an unsigned request is not retried`() {
    server.enqueue(
      rejection("device_binding_proof_invalid", date = "Wed, 04 Nov 2026 12:20:00 GMT")
    )
    server.enqueue(MockResponse())

    execute(CompletableDeferred(DeviceBinding.NotSigning(DeviceBinding.Reason.TIMEOUT))).use {
      assertEquals(401, it.code)
    }

    assertEquals(1, server.requestCount)
  }

  @Test
  fun `the other binding rejections are not retried`() {
    for (code in
      listOf(
        "device_binding_proof_missing",
        "device_binding_proof_replayed",
        "device_binding_key_mismatch",
      )) {
      server.enqueue(rejection(code, date = "Wed, 04 Nov 2026 12:20:00 GMT"))
      execute(signing()).use { assertEquals(401, it.code) }
    }

    assertEquals(3, server.requestCount)
  }

  private fun rejection(code: String, date: String?): MockResponse =
    MockResponse.Builder()
      .code(401)
      .addHeader("Content-Type", "application/json")
      .apply { if (date != null) addHeader("Date", date) else removeHeader("Date") }
      .body("""{"errors":[{"code":"$code","message":"rejected"}]}""")
      .build()

  private fun signing(): Deferred<DeviceBinding> =
    CompletableDeferred(DeviceBinding.Signing(holder))

  private fun execute(binding: Deferred<DeviceBinding>): Response =
    execute(binding, Request(server.url("/v1/client")))

  private fun execute(binding: Deferred<DeviceBinding>, request: Request): Response {
    val trusted =
      HandshakeCertificates.Builder().addTrustedCertificate(certificate.certificate).build()
    val client: OkHttpClient =
      ClerkApi.buildHttpClient(
          customHeaders = emptyMap(),
          deviceBinding = DeviceBindingState(binding),
          serverClock = serverClock,
        )
        .newBuilder()
        .sslSocketFactory(trusted.sslSocketFactory(), trusted.trustManager)
        .build()
    return client.newCall(request).execute()
  }

  private fun claims(recorded: RecordedRequest) =
    SignedJWT.parse(recorded.headers[DPOP_HEADER]).jwtClaimsSet
}
