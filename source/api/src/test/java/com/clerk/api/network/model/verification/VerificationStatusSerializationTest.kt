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
}
