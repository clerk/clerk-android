package com.clerk.api.organizations

import kotlinx.serialization.json.JsonObject
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OrganizationMembershipTest {

  @Test
  fun `permission helpers read raw and system permission keys`() {
    val membership =
      organizationMembership(
        permissions =
          listOf(
            "custom:permission",
            OrganizationSystemPermission.MANAGE_PROFILE.value,
            OrganizationSystemPermission.DELETE_PROFILE.value,
            OrganizationSystemPermission.READ_MEMBERSHIPS.value,
            OrganizationSystemPermission.MANAGE_MEMBERSHIPS.value,
            OrganizationSystemPermission.READ_DOMAINS.value,
            OrganizationSystemPermission.MANAGE_DOMAINS.value,
            OrganizationSystemPermission.READ_BILLING.value,
            OrganizationSystemPermission.MANAGE_BILLING.value,
            OrganizationSystemPermission.READ_API_KEYS.value,
            OrganizationSystemPermission.MANAGE_API_KEYS.value,
          )
      )

    assertTrue(membership.hasPermission("custom:permission"))
    assertTrue(membership.hasPermission(OrganizationSystemPermission.MANAGE_PROFILE))
    assertTrue(membership.canManageProfile)
    assertTrue(membership.canDeleteOrganization)
    assertTrue(membership.canReadMemberships)
    assertTrue(membership.canManageMemberships)
    assertTrue(membership.canReadDomains)
    assertTrue(membership.canManageDomains)
    assertTrue(membership.canReadBilling)
    assertTrue(membership.canManageBilling)
    assertTrue(membership.canReadApiKeys)
    assertTrue(membership.canManageApiKeys)
  }

  @Test
  fun `permission helpers return false for missing permissions`() {
    val membership = organizationMembership(permissions = null)

    assertFalse(membership.hasPermission("custom:permission"))
    assertFalse(membership.hasPermission(OrganizationSystemPermission.MANAGE_PROFILE))
    assertFalse(membership.canManageProfile)
    assertFalse(membership.canDeleteOrganization)
    assertFalse(membership.canReadMemberships)
    assertFalse(membership.canManageMemberships)
    assertFalse(membership.canReadDomains)
    assertFalse(membership.canManageDomains)
    assertFalse(membership.canReadBilling)
    assertFalse(membership.canManageBilling)
    assertFalse(membership.canReadApiKeys)
    assertFalse(membership.canManageApiKeys)
  }

  private fun organizationMembership(permissions: List<String>?): OrganizationMembership {
    return OrganizationMembership(
      id = "orgmem_123",
      publicMetadata = JsonObject(emptyMap()),
      role = "org:admin",
      roleName = "Admin",
      permissions = permissions,
      organization =
        Organization(
          id = "org_123",
          name = "Acme",
          slug = "acme",
          imageUrl = "https://example.com/acme.png",
          maxAllowedMemberships = 5,
          adminDeleteEnabled = true,
          createdAt = 1_000,
          updatedAt = 1_000,
          publicMetadata = JsonObject(emptyMap()),
        ),
      createdAt = 1_000,
      updatedAt = 1_000,
    )
  }
}
