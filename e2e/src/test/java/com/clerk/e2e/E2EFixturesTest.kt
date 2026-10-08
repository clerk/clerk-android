package com.clerk.e2e

import java.util.UUID
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class E2EFixturesTest {
  @Test
  fun `unique test email uses the clerk_test subaddress so the instance accepts code 424242`() {
    val email = E2EFixtures.uniqueTestEmail(UUID.fromString("00000000-0000-0000-0000-00000000abcd"))

    assertEquals("clerk_android_e2e+clerk_test_0000000000000000000000000000abcd@example.com", email)
  }

  @Test
  fun `unique test emails differ between runs`() {
    assertNotEquals(E2EFixtures.uniqueTestEmail(), E2EFixtures.uniqueTestEmail())
  }

  @Test
  fun `sign-up phone number stays inside the reserved 555-555-0100 to 0199 range`() {
    val digits = E2EFixtures.SIGN_UP_PHONE_E164.removePrefix("+1").toLong()

    assertTrue(digits in 5_555_550_100L..5_555_550_199L)
  }
}
