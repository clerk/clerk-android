package com.clerk.api.signup

import com.clerk.api.network.ClerkApi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class SignUpSerializationTest {

  @Test
  fun `sign up preserves protect check as a missing requirement`() {
    val signUp =
      ClerkApi.json.decodeFromString<SignUp>(
        """
        {
          "id": "sua_123",
          "status": "missing_requirements",
          "required_fields": [],
          "optional_fields": [],
          "missing_fields": ["protect_check"],
          "unverified_fields": [],
          "verifications": {},
          "password_enabled": false,
          "protect_check": {
            "status": "pending",
            "token": "challenge-token",
            "sdk_url": "https://example.com/protect.js",
            "future_field": ["preserved"]
          }
        }
        """
          .trimIndent()
      )

    assertEquals(SignUp.Status.MISSING_REQUIREMENTS, signUp.status)
    assertEquals(listOf("protect_check"), signUp.missingFields)
    assertNotNull(signUp.protectCheck)

    val encoded = ClerkApi.json.encodeToString(SignUp.serializer(), signUp)
    val decoded = ClerkApi.json.decodeFromString<SignUp>(encoded)

    assertEquals(signUp.protectCheck, decoded.protectCheck)
    assertEquals(signUp.missingFields, decoded.missingFields)
  }
}
