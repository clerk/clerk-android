package com.clerk.api.hostedauth

import java.security.SecureRandom
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class HostedAuthPkceTest {
  @Test
  fun generatedStateUsesSecureRandomBytes() {
    val state = generateHostedAuthState(FixedSecureRandom(ByteArray(16) { it.toByte() }))

    assertEquals("000102030405060708090a0b0c0d0e0f", state)
  }

  @Test
  fun generatedPkceUsesS256WithUrlSafeEncoding() {
    val pkce = generateHostedAuthPkce(FixedSecureRandom(ByteArray(32) { it.toByte() }))

    assertEquals(
      "000102030405060708090a0b0c0d0e0f101112131415161718191a1b1c1d1e1f",
      pkce.codeVerifier,
    )
    assertEquals(43, pkce.codeChallenge.length)
    assertFalse(pkce.codeChallenge.contains('+'))
    assertFalse(pkce.codeChallenge.contains('/'))
    assertFalse(pkce.codeChallenge.contains('='))
  }

  @Test
  fun codeChallengeMatchesRfc7636S256Vector() {
    assertEquals(
      "E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM",
      hostedAuthCodeChallenge("dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk"),
    )
  }

  private class FixedSecureRandom(private val bytes: ByteArray) : SecureRandom() {
    override fun nextBytes(target: ByteArray) {
      require(target.size == bytes.size)
      bytes.copyInto(target)
    }
  }
}
