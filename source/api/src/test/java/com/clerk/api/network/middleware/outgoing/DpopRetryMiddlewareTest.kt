package com.clerk.api.network.middleware.outgoing

import com.clerk.api.Clerk
import com.clerk.api.configuration.DeviceBinding
import com.clerk.api.devicekey.DeviceKeyHolder
import com.clerk.api.devicekey.JvmDeviceKey
import com.clerk.api.dpop.ServerClock
import com.clerk.api.network.ClerkApi
import com.clerk.api.network.model.client.Client
import com.nimbusds.jwt.SignedJWT
import kotlin.test.assertEquals
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import mockwebserver3.RecordedRequest
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

/** Runs requests through the SDK's own OkHttp client, so the order of its interceptors counts. */
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

  @Before
  fun setUp() {
    server.useHttps(
      HandshakeCertificates.Builder().heldCertificate(certificate).build().sslSocketFactory()
    )
    server.start()
  }

  @After
  fun tearDown() {
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

  private fun signing(): Deferred<DeviceBinding> =
    CompletableDeferred(DeviceBinding.Signing(holder))

  private fun execute(binding: Deferred<DeviceBinding>): Response {
    val trusted =
      HandshakeCertificates.Builder().addTrustedCertificate(certificate.certificate).build()
    val client: OkHttpClient =
      ClerkApi.buildHttpClient(
          customHeaders = emptyMap(),
          deviceBinding = binding,
          serverClock = serverClock,
        )
        .newBuilder()
        .sslSocketFactory(trusted.sslSocketFactory(), trusted.trustManager)
        .build()
    return client.newCall(Request(server.url("/v1/client"))).execute()
  }

  private fun claims(recorded: RecordedRequest) =
    SignedJWT.parse(recorded.headers[DPOP_HEADER]).jwtClaimsSet
}
