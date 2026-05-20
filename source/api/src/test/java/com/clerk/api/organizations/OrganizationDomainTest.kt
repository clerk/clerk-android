package com.clerk.api.organizations

import com.clerk.api.network.ClerkApi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OrganizationDomainTest {

  @Test
  fun `enrollmentModeType maps known enrollment modes`() {
    val knownModes =
      mapOf(
        "manual_invitation" to OrganizationDomain.EnrollmentMode.ManualInvitation,
        "automatic_invitation" to OrganizationDomain.EnrollmentMode.AutomaticInvitation,
        "automatic_suggestion" to OrganizationDomain.EnrollmentMode.AutomaticSuggestion,
      )

    knownModes.forEach { (rawValue, enrollmentMode) ->
      assertEquals(enrollmentMode, organizationDomain(enrollmentMode = rawValue).enrollmentModeType)
    }
  }

  @Test
  fun `enrollmentModeType preserves unknown enrollment modes`() {
    val domain = organizationDomain(enrollmentMode = "future_mode")

    assertEquals(
      OrganizationDomain.EnrollmentMode.Unknown("future_mode"),
      domain.enrollmentModeType,
    )
  }

  @Test
  fun `isVerified reflects verification status`() {
    assertTrue(organizationDomain(verificationStatus = "verified").isVerified)
    assertFalse(organizationDomain(verificationStatus = "unverified").isVerified)
    assertFalse(organizationDomain(verificationStatus = null).isVerified)
  }

  private fun organizationDomain(
    enrollmentMode: String = "manual_invitation",
    verificationStatus: String? = "verified",
  ): OrganizationDomain {
    val verification =
      if (verificationStatus == null) {
        "null"
      } else {
        """
        {
          "status": "$verificationStatus",
          "strategy": "email_code",
          "attempts": 0,
          "expire_at": null
        }
        """
          .trimIndent()
      }

    return ClerkApi.json.decodeFromString(
      """
      {
        "id": "orgdmn_123",
        "name": "example.com",
        "organization_id": "org_123",
        "enrollment_mode": "$enrollmentMode",
        "verification": $verification,
        "affiliation_email_address": null,
        "total_pending_invitations": 0,
        "created_at": 1713200000000,
        "updated_at": 1713200000000,
        "public_organization_data": null,
        "total_pending_suggestions": 0
      }
      """
        .trimIndent()
    )
  }
}
