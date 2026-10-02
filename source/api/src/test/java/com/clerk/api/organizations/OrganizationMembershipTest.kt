package com.clerk.api.organizations

import kotlinx.serialization.json.JsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OrganizationMembershipTest {

  @Test
  fun `permission helpers read raw and system permission keys`() {
    val membership =
      organizationMembership(
        permissions = listOf("custom:permission", OrganizationSystemPermission.MANAGE_PROFILE.value)
      )

    assertTrue(membership.hasPermission("custom:permission"))
    assertTrue(membership.hasPermission(OrganizationSystemPermission.MANAGE_PROFILE))
    assertFalse(membership.hasPermission(OrganizationSystemPermission.DELETE_PROFILE))
  }

  @Test
  fun `each permission helper is granted only by its own system permission`() {
    val helpers: Map<OrganizationSystemPermission, (OrganizationMembership) -> Boolean> =
      mapOf(
        OrganizationSystemPermission.MANAGE_PROFILE to { it.canManageProfile },
        OrganizationSystemPermission.DELETE_PROFILE to { it.canDeleteOrganization },
        OrganizationSystemPermission.READ_MEMBERSHIPS to { it.canReadMemberships },
        OrganizationSystemPermission.MANAGE_MEMBERSHIPS to { it.canManageMemberships },
        OrganizationSystemPermission.READ_DOMAINS to { it.canReadDomains },
        OrganizationSystemPermission.MANAGE_DOMAINS to { it.canManageDomains },
        OrganizationSystemPermission.READ_BILLING to { it.canReadBilling },
        OrganizationSystemPermission.MANAGE_BILLING to { it.canManageBilling },
        OrganizationSystemPermission.READ_API_KEYS to { it.canReadApiKeys },
        OrganizationSystemPermission.MANAGE_API_KEYS to { it.canManageApiKeys },
      )
    assertEquals(OrganizationSystemPermission.entries.toSet(), helpers.keys)

    helpers.keys.forEach { granted ->
      val membership = organizationMembership(permissions = listOf(granted.value))
      helpers.forEach { (permission, helper) ->
        assertEquals(
          "helper for $permission with only $granted granted",
          permission == granted,
          helper(membership),
        )
      }
    }
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
