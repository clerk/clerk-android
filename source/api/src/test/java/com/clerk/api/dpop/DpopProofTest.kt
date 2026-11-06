package com.clerk.api.dpop

import com.clerk.api.devicekey.DeviceKey
import com.clerk.api.devicekey.JvmDeviceKey
import com.nimbusds.jose.JOSEObjectType
import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.crypto.ECDSAVerifier
import com.nimbusds.jose.jwk.ECKey
import com.nimbusds.jwt.SignedJWT
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DpopProofTest {
  private val key: DeviceKey = JvmDeviceKey.getOrCreate()

  @Test
  fun `the embedded jwk verifies the signature`() {
    val proof =
      buildProof(
        url = "https://clerk.example.com/v1/client?_is_native=true",
        now = Clock.System.now(),
      )
    println("DPoP proof for GET https://clerk.example.com/v1/client: $proof")

    val jwt = SignedJWT.parse(proof)
    val jwk = assertIs<ECKey>(jwt.header.jwk)

    assertTrue(jwt.verify(ECDSAVerifier(jwk)))
    assertEquals(JOSEObjectType("dpop+jwt"), jwt.header.type)
    assertEquals(JWSAlgorithm.ES256, jwt.header.algorithm)
    assertFalse(jwk.isPrivate)
    assertEquals(key.thumbprint, jwk.computeThumbprint().toString())
    assertEquals("https://clerk.example.com/v1/client", jwt.jwtClaimsSet.getStringClaim("htu"))
    assertEquals("GET", jwt.jwtClaimsSet.getStringClaim("htm"))
    assertEquals("proof-id", jwt.jwtClaimsSet.jwtid)
    assertEquals("software", jwt.jwtClaimsSet.getStringClaim("clerk_sl"))
  }

  @Test
  fun `htu drops the query and fragment and keeps a proxy path`() {
    val proof = buildProof(url = "https://app.example.com/__clerk/v1/client?_is_native=true#top")

    assertEquals("https://app.example.com/__clerk/v1/client", claims(proof).getStringClaim("htu"))
  }

  @Test
  fun `htu drops userinfo, which the server's verifier rejects`() {
    val proof = buildProof(url = "https://user:secret@clerk.example.com/v1/client")

    assertEquals("https://clerk.example.com/v1/client", claims(proof).getStringClaim("htu"))
  }

  @Test
  fun `ath is the RFC 9449 hash of the access token`() {
    val proof = buildProof(accessToken = "Kz~8mXK1EalYznwH-LC-1fBAo.4Ljp~zsPE_NeO.gxU")

    assertEquals("fUHyO2r2Z3DZ53EsNrWBb0xWXoaNy59IiKCAqksmQEo", claims(proof).getStringClaim("ath"))
  }

  @Test
  fun `ath hashes the UTF-8 bytes of a non-ASCII access token`() {
    val proof = buildProof(accessToken = "tökén-日本")

    assertEquals("TNYs-KYciDl4w33VRjNr0j_SXJmNv8v1ZvJ5VQIPz68", claims(proof).getStringClaim("ath"))
  }

  @Test
  fun `a proof without an access token has no ath claim`() {
    val proof = buildProof(accessToken = null)

    assertFalse("ath" in claims(proof).claims)
    assertFalse("\"ath\"" in SignedJWT.parse(proof).payload.toString())
  }

  @Test
  fun `iat is whole seconds`() {
    val proof = buildProof(now = Instant.fromEpochMilliseconds(1_700_000_000_999))

    assertEquals(JsonPrimitive(1_700_000_000L), payload(proof)["iat"])
  }

  @Test
  fun `a proof for a long URL fits the server's size limit`() {
    val proof =
      buildProof(
        url =
          "https://clerk.example-with-a-long-name.com/v1/client/sessions/${"s".repeat(64)}/tokens"
      )

    assertTrue(proof.length < 8192, "proof is ${proof.length} bytes")
  }

  private fun buildProof(
    url: String = "https://clerk.example.com/v1/client",
    accessToken: String? = null,
    now: Instant = Instant.fromEpochSeconds(1_700_000_000),
  ): String =
    DpopProof.build(
      key = key,
      request = BoundRequest(method = "GET", url = url.toHttpUrl(), accessToken = accessToken),
      now = now,
      jti = "proof-id",
    )

  private fun claims(proof: String) = SignedJWT.parse(proof).jwtClaimsSet

  private fun payload(proof: String) =
    Json.parseToJsonElement(SignedJWT.parse(proof).payload.toString()).jsonObject
}
