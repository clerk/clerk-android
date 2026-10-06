package com.clerk.e2e

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TotpTest {
  private val rfc6238Secret = "GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ"

  @Test
  fun `code matches RFC 6238 SHA1 vectors truncated to six digits`() {
    assertEquals("287082", Totp.code(rfc6238Secret, 59))
    assertEquals("081804", Totp.code(rfc6238Secret, 1_111_111_109))
    assertEquals("050471", Totp.code(rfc6238Secret, 1_111_111_111))
    assertEquals("005924", Totp.code(rfc6238Secret, 1_234_567_890))
    assertEquals("279037", Totp.code(rfc6238Secret, 2_000_000_000))
  }

  @Test
  fun `code ignores case, spaces and padding in the secret`() {
    assertEquals(
      Totp.code(rfc6238Secret, 59),
      Totp.code("gezd gnbv gy3t qojq gezd gnbv gy3t qojq====", 59),
    )
  }

  @Test
  fun `secretFrom accepts a raw base32 secret`() {
    assertEquals(rfc6238Secret, Totp.secretFrom("  $rfc6238Secret\n"))
  }

  @Test
  fun `secretFrom extracts the secret from an otpauth uri`() {
    val uri = "otpauth://totp/Clerk:user@example.com?secret=JBSWY3DPEHPK3PXP&issuer=Clerk&digits=6"

    assertEquals("JBSWY3DPEHPK3PXP", Totp.secretFrom(uri))
  }

  @Test
  fun `secretFrom rejects text that is not a base32 secret`() {
    assertNull(Totp.secretFrom("Copied to clipboard!"))
    assertNull(Totp.secretFrom(""))
    assertNull(Totp.secretFrom("otpauth://totp/Clerk?issuer=Clerk"))
  }

  @Test
  fun `secondsRemaining counts down within each 30 second window`() {
    assertEquals(30, Totp.secondsRemaining(60))
    assertEquals(1, Totp.secondsRemaining(89))
  }
}
