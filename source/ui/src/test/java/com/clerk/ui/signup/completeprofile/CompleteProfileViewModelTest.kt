package com.clerk.ui.signup.completeprofile

import com.clerk.api.signup.SignUp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class CompleteProfileViewModelTest {

  @Test
  fun `completeProfileUpdateParams omits unsupported name fields`() {
    val signUp =
      signUp(
        requiredFields = listOf("legal_accepted", "password"),
        optionalFields = listOf("email_address"),
      )

    val params =
      signUp.completeProfileUpdateParams(firstName = "", lastName = "", legalAccepted = true)

    assertNull(params.firstName)
    assertNull(params.lastName)
    assertEquals(true, params.legalAccepted)
  }

  @Test
  fun `completeProfileUpdateParams keeps required non blank name fields`() {
    val signUp =
      signUp(
        requiredFields = listOf(FIRST_NAME_FIELD, LAST_NAME_FIELD),
        optionalFields = emptyList(),
      )

    val params =
      signUp.completeProfileUpdateParams(
        firstName = "Sam",
        lastName = "McTest",
        legalAccepted = null,
      )

    assertEquals("Sam", params.firstName)
    assertEquals("McTest", params.lastName)
    assertNull(params.legalAccepted)
  }

  @Test
  fun `completeProfileUpdateParams omits optional name fields`() {
    val signUp =
      signUp(
        requiredFields = emptyList(),
        optionalFields = listOf(FIRST_NAME_FIELD, LAST_NAME_FIELD),
      )

    val params =
      signUp.completeProfileUpdateParams(
        firstName = "Sam",
        lastName = "McTest",
        legalAccepted = null,
      )

    assertNull(params.firstName)
    assertNull(params.lastName)
    assertNull(params.legalAccepted)
  }

  private fun signUp(requiredFields: List<String>, optionalFields: List<String>): SignUp {
    return SignUp(
      id = "sua_123",
      status = SignUp.Status.MISSING_REQUIREMENTS,
      requiredFields = requiredFields,
      optionalFields = optionalFields,
      missingFields = requiredFields,
      unverifiedFields = emptyList(),
      verifications = emptyMap(),
      passwordEnabled = false,
    )
  }
}
