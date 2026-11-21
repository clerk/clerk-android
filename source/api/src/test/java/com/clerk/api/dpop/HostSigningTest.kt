package com.clerk.api.dpop

import com.clerk.api.configuration.DEVICE_KEY_WAIT
import com.clerk.api.configuration.DeviceBinding
import com.clerk.api.configuration.DeviceBindingState
import com.clerk.api.devicekey.DeviceKeyHolder
import com.clerk.api.devicekey.JvmDeviceKey
import com.nimbusds.jose.crypto.ECDSAVerifier
import com.nimbusds.jose.jwk.ECKey
import com.nimbusds.jwt.SignedJWT
import java.io.IOException
import java.security.MessageDigest
import java.util.Base64
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.coroutines.CompletableDeferred
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class HostSigningTest {
  private val holder = DeviceKeyHolder { JvmDeviceKey.getOrCreate() }
  private val localTime = Instant.parse("2026-11-04T12:00:00Z")
  private val localClock =
    object : Clock {
      override fun now(): Instant = localTime
    }

  @Test
  fun `a proof signs the wire method, the URL without its query, and the token as sent`() {
    val proof =
      SignedJWT.parse(
        hostSigning(signing())
          .proof(
            method = "post",
            url = "https://clerk.example.com/api/__clerk/v1/client/sessions?_method=PATCH#frag",
            authorization = BOUND_DEVICE_TOKEN,
          )
      )

    val jwk = assertIs<ECKey>(proof.header.jwk)
    assertTrue(proof.verify(ECDSAVerifier(jwk)))
    val claims = proof.jwtClaimsSet
    assertEquals("POST", claims.getStringClaim("htm"))
    assertEquals(
      "https://clerk.example.com/api/__clerk/v1/client/sessions",
      claims.getStringClaim("htu"),
    )
    assertEquals(sha256(BOUND_DEVICE_TOKEN), claims.getStringClaim("ath"))
    assertEquals(localTime.epochSeconds, claims.issueTime.time / 1000)
  }

  @Test
  fun `a request without a token gets a proof without ath`() {
    val proof = hostSigning(signing()).proof("GET", "https://clerk.example.com/v1/client", null)

    assertNull(SignedJWT.parse(proof).jwtClaimsSet.getClaim("ath"))
  }

  @Test
  fun `every call gets a new jti`() {
    val signing = hostSigning(signing())

    val first = signing.proof("GET", "https://clerk.example.com/v1/client", null)
    val second = signing.proof("GET", "https://clerk.example.com/v1/client", null)

    assertNotEquals(
      SignedJWT.parse(first).jwtClaimsSet.jwtid,
      SignedJWT.parse(second).jwtClaimsSet.jwtid,
    )
  }

  @Test
  fun `a host that did not opt in gets no proof, even for a bound token`() {
    val signing = hostSigning(notSigning(DeviceBinding.Reason.FRAMEWORK_HOST))

    assertNull(signing.proof("GET", "https://clerk.example.com/v1/client", null))
    assertNull(signing.proof("GET", "https://clerk.example.com/v1/client", BOUND_DEVICE_TOKEN))
  }

  @Test
  fun `an unavailable key sends the request unsigned`() {
    val signing = hostSigning(notSigning(DeviceBinding.Reason.KEY_UNAVAILABLE))

    assertNull(signing.proof("GET", "https://clerk.example.com/v1/client", BOUND_DEVICE_TOKEN))
  }

  @Test
  fun `a bound token whose key is not ready fails instead of going unsigned`() {
    val signing = hostSigning(notSigning(DeviceBinding.Reason.TIMEOUT))

    assertNull(signing.proof("GET", "https://clerk.example.com/v1/client", UNBOUND_DEVICE_TOKEN))
    assertFailsWith<IOException> {
      signing.proof("GET", "https://clerk.example.com/v1/client", BOUND_DEVICE_TOKEN)
    }
  }

  @Test
  fun `a recorded server date dates the next proof`() {
    val signing = hostSigning(signing())

    signing.recordServerDate("Wed, 04 Nov 2026 12:20:00 GMT")
    val proof = signing.proof("GET", "https://clerk.example.com/v1/client", null)

    val issuedAt = SignedJWT.parse(proof).jwtClaimsSet.issueTime.time / 1000
    assertEquals(Instant.parse("2026-11-04T12:20:00Z").epochSeconds, issuedAt)
  }

  @Test
  fun `a server date that does not parse leaves the clock alone`() {
    val signing = hostSigning(signing())

    signing.recordServerDate("yesterday")
    signing.recordServerDate("Wed, 04 Nov 2026\n12:20:00 GMT")
    val proof = assertNotNull(signing.proof("GET", "https://clerk.example.com/v1/client", null))

    assertEquals(localTime.epochSeconds, SignedJWT.parse(proof).jwtClaimsSet.issueTime.time / 1000)
  }

  @Test
  fun `a URL that is not http rejects`() {
    assertFailsWith<IllegalArgumentException> {
      hostSigning(signing()).proof("GET", "clerk.example.com/v1/client", null)
    }
  }

  private fun hostSigning(binding: DeviceBinding): HostSigning {
    val clock = ServerClock(localClock)
    val state = DeviceBindingState(CompletableDeferred(binding))
    return HostSigning(DpopSigner(state, clock, DEVICE_KEY_WAIT), clock)
  }

  private fun signing(): DeviceBinding = DeviceBinding.Signing(holder)

  private fun notSigning(reason: DeviceBinding.Reason): DeviceBinding =
    DeviceBinding.NotSigning(reason)

  private fun sha256(value: String): String =
    Base64.getUrlEncoder()
      .withoutPadding()
      .encodeToString(MessageDigest.getInstance("SHA-256").digest(value.toByteArray()))

  private companion object {
    val BOUND_DEVICE_TOKEN = jwt("""{"id":"client_bound","cnf":{"jkt":"0ZcOCORZNYy"}}""")
    val UNBOUND_DEVICE_TOKEN = jwt("""{"id":"client_unbound"}""")

    fun jwt(payload: String): String =
      listOf("""{"alg":"RS256"}""", payload).joinToString(".", postfix = ".signature") {
        Base64.getUrlEncoder().withoutPadding().encodeToString(it.toByteArray())
      }
  }
}
