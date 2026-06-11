package com.clerk.api.signup

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SignUpFieldCollectionTest {

  @Test
  fun `firstFieldToCollect ignores optional missing fields`() {
    val signUp =
      signUp(
        requiredFields = listOf("email_address", "password"),
        optionalFields = listOf("first_name", "last_name"),
        missingFields = listOf("first_name", "password", "last_name"),
      )

    assertEquals("password", signUp.firstFieldToCollect)
  }

  @Test
  fun `firstFieldToCollect returns null when only optional fields are missing`() {
    val signUp =
      signUp(
        requiredFields = listOf("email_address"),
        optionalFields = listOf("first_name", "last_name"),
        missingFields = listOf("first_name", "last_name"),
      )

    assertNull(signUp.firstFieldToCollect)
  }

  private fun signUp(
    requiredFields: List<String>,
    optionalFields: List<String>,
    missingFields: List<String>,
  ): SignUp {
    return SignUp(
      id = "sua_123",
      status = SignUp.Status.MISSING_REQUIREMENTS,
      requiredFields = requiredFields,
      optionalFields = optionalFields,
      missingFields = missingFields,
      unverifiedFields = emptyList(),
      verifications = emptyMap(),
      passwordEnabled = false,
    )
  }
}
