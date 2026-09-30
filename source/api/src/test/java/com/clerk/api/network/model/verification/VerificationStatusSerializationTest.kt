package com.clerk.api.network.model.verification

import com.clerk.api.network.ClerkApi
import org.junit.Assert.assertEquals
import org.junit.Test

class VerificationStatusSerializationTest {

  @Test
  fun `expired status decodes to EXPIRED`() {
    val verification = ClerkApi.json.decodeFromString<Verification>("""{"status":"expired"}""")

    assertEquals(Verification.Status.EXPIRED, verification.status)
  }

  @Test
  fun `failed status decodes to FAILED`() {
    val verification = ClerkApi.json.decodeFromString<Verification>("""{"status":"failed"}""")

    assertEquals(Verification.Status.FAILED, verification.status)
  }

  @Test
  fun `statuses round trip through their serial names`() {
    val expected =
      mapOf(
        Verification.Status.UNVERIFIED to "unverified",
        Verification.Status.VERIFIED to "verified",
        Verification.Status.TRANSFERABLE to "transferable",
        Verification.Status.FAILED to "failed",
        Verification.Status.EXPIRED to "expired",
        Verification.Status.UNKNOWN to "state_unknown",
      )

    expected.forEach { (status, serialName) ->
      val encoded = ClerkApi.json.encodeToString(status)
      assertEquals("\"$serialName\"", encoded)
      assertEquals(status, ClerkApi.json.decodeFromString<Verification.Status>(encoded))

      val verification = Verification(status = status)
      val decoded =
        ClerkApi.json.decodeFromString<Verification>(ClerkApi.json.encodeToString(verification))
      assertEquals(status, decoded.status)
    }
  }
}
