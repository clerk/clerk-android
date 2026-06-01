package com.clerk.api.organizations

import com.clerk.api.network.ClerkApi
import org.junit.Assert.assertEquals
import org.junit.Test

class UserOrganizationInvitationTest {

  @Test
  fun `decodes invitation timestamps and metadata objects`() {
    val invitation =
      ClerkApi.json.decodeFromString<UserOrganizationInvitation>(
        """
        {
          "id": "invite_123",
          "email_address": "sam@example.com",
          "public_organization_data": {
            "id": "org_123",
            "name": "Acme",
            "image_url": null,
            "has_image": false,
            "slug": "acme"
          },
          "public_metadata": {
            "source": "test"
          },
          "role": "org:member",
          "status": "pending",
          "created_at": 1713200000000,
          "updated_at": "2024-04-15T19:17:53Z"
        }
        """
          .trimIndent()
      )

    assertEquals("invite_123", invitation.id)
    assertEquals("""{"source":"test"}""", invitation.publicMetadata)
    assertEquals(1713200000000, invitation.createdAt)
    assertEquals(1713208673000, invitation.updatedAt)
  }

  @Test
  fun `decodes invitation timestamps from epoch seconds`() {
    val invitation =
      ClerkApi.json.decodeFromString<UserOrganizationInvitation>(
        """
        {
          "id": "invite_123",
          "email_address": "sam@example.com",
          "public_organization_data": {
            "id": "org_123",
            "name": "Acme",
            "image_url": null,
            "has_image": false
          },
          "public_metadata": {},
          "role": "org:member",
          "status": "pending",
          "created_at": 1713200000,
          "updated_at": 1713200000
        }
        """
          .trimIndent()
      )

    assertEquals(1713200000000, invitation.createdAt)
    assertEquals(1713200000000, invitation.updatedAt)
  }
}
