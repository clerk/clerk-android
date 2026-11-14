package com.clerk.api.network.middleware.outgoing

import android.util.Log
import com.clerk.api.configuration.DEVICE_KEY_WAIT
import com.clerk.api.configuration.DeviceBinding
import com.clerk.api.configuration.DeviceBindingState
import com.clerk.api.devicekey.DeviceKey
import com.clerk.api.devicekey.DeviceKeyHolder
import com.clerk.api.devicekey.DeviceKeyLostException
import com.clerk.api.devicekey.JvmDeviceKey
import com.clerk.api.dpop.ServerClock
import com.nimbusds.jose.crypto.ECDSAVerifier
import com.nimbusds.jose.jwk.ECKey
import com.nimbusds.jwt.SignedJWT
import java.io.IOException
import java.security.MessageDigest
import java.security.ProviderException
import java.util.Base64
import java.util.UUID
import kotlin.concurrent.thread
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.runBlocking
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import mockwebserver3.RecordedRequest
import mockwebserver3.SocketEffect
import okhttp3.OkHttpClient
import okhttp3.Request
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowLog

@RunWith(RobolectricTestRunner::class)
class DpopMiddlewareTest {
  private val server = MockWebServer()
  private val holder = DeviceKeyHolder { JvmDeviceKey.getOrCreate() }
  private val localClock =
    object : Clock {
      override fun now(): Instant = Instant.parse("2026-11-04T12:00:00Z")
    }
  private val serverClock = ServerClock(localClock)

  @Before
  fun setUp() {
    server.start()
  }

  @After
  fun tearDown() {
    server.close()
  }

  @Test
  fun `a request carries exactly one DPoP header, signed by the holder's key`() {
    server.enqueue(MockResponse())

    execute(signing(), get("/v1/client").newBuilder().header(DPOP_HEADER, "stale").build())

    val recorded = server.takeRequest()
    assertEquals(1, recorded.headers.values(DPOP_HEADER).size)
    val proof = SignedJWT.parse(recorded.headers[DPOP_HEADER])
    val jwk = assertIs<ECKey>(proof.header.jwk)
    assertTrue(proof.verify(ECDSAVerifier(jwk)))
    assertEquals(holder.withKey { it.thumbprint }.getOrThrow(), jwk.computeThumbprint().toString())
  }

  @Test
  fun `OkHttp's retry after a reset pooled connection carries a new UUID jti`() {
    server.enqueue(MockResponse())
    server.enqueue(MockResponse.Builder().onResponseStart(SocketEffect.CloseSocket()).build())
    server.enqueue(MockResponse())
    val client = client(signing())

    execute(client, get("/v1/environment"))
    execute(client, get("/v1/client"))

    server.takeRequest()
    val first = jti(server.takeRequest())
    val second = jti(server.takeRequest())
    assertNotEquals(first, second)
    assertEquals(first, UUID.fromString(first).toString())
    assertEquals(second, UUID.fromString(second).toString())
  }

  @Test
  fun `a redirect carries one proof for the new URL`() {
    server.enqueue(
      MockResponse.Builder().code(307).addHeader("Location", server.url("/v1/environment")).build()
    )
    server.enqueue(MockResponse())

    execute(signing(), get("/v1/client?_is_native=true"))

    val first = server.takeRequest()
    val redirected = server.takeRequest()
    assertEquals(server.url("/v1/client").toString(), htu(first))
    assertEquals(1, redirected.headers.values(DPOP_HEADER).size)
    assertEquals(server.url("/v1/environment").toString(), htu(redirected))
    assertNotEquals(jti(first), jti(redirected))
  }

  @Test
  fun `no proof when the runtime does not sign`() {
    server.enqueue(MockResponse())

    execute(
      CompletableDeferred(DeviceBinding.NotSigning(DeviceBinding.Reason.KEY_UNAVAILABLE)),
      get("/v1/client"),
    )

    assertNull(server.takeRequest().headers[DPOP_HEADER])
  }

  @Test
  fun `ath is the hash of the Authorization the request carried`() {
    server.enqueue(MockResponse())

    execute(signing(), get("/v1/client").newBuilder().header("Authorization", "token_123").build())

    val expected =
      Base64.getUrlEncoder()
        .withoutPadding()
        .encodeToString(MessageDigest.getInstance("SHA-256").digest("token_123".toByteArray()))
    assertEquals(expected, claims(server.takeRequest()).getStringClaim("ath"))
  }

  @Test
  fun `iat follows the recorded server time, not the local clock`() {
    serverClock.record(Instant.parse("2026-11-04T12:20:00Z"))
    server.enqueue(MockResponse())

    execute(signing(), get("/v1/client"))

    assertEquals(
      Instant.parse("2026-11-04T12:20:00Z").epochSeconds,
      claims(server.takeRequest()).issueTime.time / 1000,
    )
  }

  @Test
  fun `a request waits for the binding to resolve`() {
    server.enqueue(MockResponse())
    val binding = CompletableDeferred<DeviceBinding>()
    thread {
      Thread.sleep(200)
      binding.complete(DeviceBinding.Signing(holder))
    }

    execute(binding, get("/v1/client"))

    assertEquals(1, server.takeRequest().headers.values(DPOP_HEADER).size)
  }

  @Test
  fun `a reset while the request waits fails it with an IOException`() {
    val binding = CompletableDeferred<DeviceBinding>().apply { cancel() }

    assertFailsWith<IOException> { execute(binding, get("/v1/client")) }
    assertEquals(0, server.requestCount)
  }

  @Test
  fun `an unbound token goes out unsigned after one sign attempt that fails without a loss`() {
    for (failure in
      listOf(
        ProviderException("INVALID_KEY_BLOB"),
        IllegalStateException("bug"),
        IOException("Keystore I/O"),
      )) {
      val failingKey = FailingKey(failure)
      server.enqueue(MockResponse())
      ShadowLog.clear()

      execute(signingWith(failingKey), get("/").withToken(UNBOUND_DEVICE_TOKEN))

      assertNull(server.takeRequest().headers[DPOP_HEADER])
      assertEquals(1, failingKey.signs)
      val warnings = ShadowLog.getLogsForTag("ClerkLog").filter { it.type == Log.WARN }
      assertEquals(
        listOf("Clerk warning: DPoP proof failed: ${failure.javaClass.name}"),
        warnings.map { it.msg },
      )
    }
  }

  @Test
  fun `a bound token's request fails when a sign attempt fails without a loss`() {
    for (failure in
      listOf(
        ProviderException("INVALID_KEY_BLOB"),
        IllegalStateException("bug"),
        IOException("Keystore I/O"),
      )) {
      val failingKey = FailingKey(failure)

      val error =
        assertFailsWith<IOException> {
          execute(signingWith(failingKey), get("/").withToken(BOUND_DEVICE_TOKEN))
        }

      assertEquals(failure, error.cause)
      assertTrue("KEY_ERROR" in error.message.orEmpty(), error.message)
      assertEquals(1, failingKey.signs)
    }
    assertEquals(0, server.requestCount)
  }

  @Test
  fun `a bound token goes out unsigned once the holder gives up a lost key`() {
    val lostKey = FailingKey(DeviceKeyLostException())
    server.enqueue(MockResponse())

    execute(signingWith(lostKey), get("/").withToken(BOUND_DEVICE_TOKEN))

    val recorded = server.takeRequest()
    assertEquals(BOUND_DEVICE_TOKEN, recorded.headers["Authorization"])
    assertNull(recorded.headers[DPOP_HEADER])
  }

  @Test
  fun `a request-time load that throws a non-Keystore exception sends bound requests unsigned`() {
    var loads = 0
    val holder = DeviceKeyHolder {
      loads += 1
      throw IllegalStateException("vendor Keystore bug")
    }
    val state = DeviceBindingState(CompletableDeferred(DeviceBinding.Signing(holder)))
    val client = client(state)
    repeat(2) { server.enqueue(MockResponse()) }

    repeat(2) { execute(client, get("/").withToken(BOUND_DEVICE_TOKEN)) }

    repeat(2) {
      val recorded = server.takeRequest()
      assertEquals(BOUND_DEVICE_TOKEN, recorded.headers["Authorization"])
      assertNull(recorded.headers[DPOP_HEADER])
    }
    assertEquals(
      DeviceBinding.NotSigning(DeviceBinding.Reason.KEY_UNAVAILABLE),
      runBlocking { state.current(DEVICE_KEY_WAIT) },
    )
    assertEquals(1, loads)
  }

  @Test
  fun `the cnf check follows the token a request carries`() {
    val client = client(signingWith(FailingKey(IllegalStateException("bug"))))
    server.enqueue(MockResponse())

    execute(client, get("/").withToken(UNBOUND_DEVICE_TOKEN))
    assertFailsWith<IOException> { execute(client, get("/").withToken(BOUND_DEVICE_TOKEN)) }

    assertEquals(1, server.requestCount)
  }

  private class FailingKey(private val failure: Exception) :
    DeviceKey by JvmDeviceKey.getOrCreate() {
    var signs = 0

    override fun sign(data: ByteArray): ByteArray {
      signs += 1
      throw failure
    }
  }

  private fun signingWith(key: DeviceKey): Deferred<DeviceBinding> =
    CompletableDeferred(DeviceBinding.Signing(DeviceKeyHolder { key }))

  private fun Request.withToken(token: String): Request =
    newBuilder().header("Authorization", token).build()

  private fun signing(): Deferred<DeviceBinding> =
    CompletableDeferred(DeviceBinding.Signing(holder))

  private fun get(path: String): Request = Request(server.url(path))

  private fun client(binding: Deferred<DeviceBinding>): OkHttpClient =
    client(DeviceBindingState(binding))

  private fun client(state: DeviceBindingState): OkHttpClient =
    OkHttpClient.Builder()
      .addNetworkInterceptor(DpopMiddleware(state, serverClock, DEVICE_KEY_WAIT))
      .build()

  private fun execute(binding: Deferred<DeviceBinding>, request: Request) {
    execute(client(binding), request)
  }

  private fun execute(client: OkHttpClient, request: Request) {
    client.newCall(request).execute().close()
  }

  private fun claims(recorded: RecordedRequest) =
    SignedJWT.parse(recorded.headers[DPOP_HEADER]).jwtClaimsSet

  private fun jti(recorded: RecordedRequest): String = claims(recorded).jwtid

  private fun htu(recorded: RecordedRequest): String = claims(recorded).getStringClaim("htu")

  private companion object {
    val BOUND_DEVICE_TOKEN = jwt("""{"id":"client_bound","cnf":{"jkt":"0ZcOCORZNYy"}}""")
    val UNBOUND_DEVICE_TOKEN = jwt("""{"id":"client_unbound"}""")

    fun jwt(payload: String): String =
      listOf("""{"alg":"RS256"}""", payload).joinToString(".", postfix = ".signature") {
        Base64.getUrlEncoder().withoutPadding().encodeToString(it.toByteArray())
      }
  }
}
