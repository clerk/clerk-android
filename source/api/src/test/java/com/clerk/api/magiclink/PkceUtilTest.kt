package com.clerk.api.magiclink

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PkceUtilTest {
  @Test
  fun `generatePair returns verifier and challenge in base64url format`() {
    val pair = PkceUtil.generatePair()

    assertTrue(pair.verifier.length in 43..128)
    assertFalse(pair.verifier.contains("="))
    assertTrue(BASE64_URL_REGEX.matches(pair.verifier))

    assertFalse(pair.challenge.contains("="))
    assertTrue(BASE64_URL_REGEX.matches(pair.challenge))
  }

  @Test
  fun `challenge matches verifier using S256`() {
    val verifier = "dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk"
    val expectedChallenge = "E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM"

    val challenge = PkceUtil.createS256CodeChallenge(verifier)

    assertEquals(expectedChallenge, challenge)
  }

  private companion object {
    private val BASE64_URL_REGEX = Regex("^[A-Za-z0-9_-]+$")
  }
}
