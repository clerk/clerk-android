package com.clerk.testing

import com.clerk.api.*
import io.mockk.*

fun snapshotOrganization(
  id: String = "org_acme",
  name: String = "Acme Inc.",
  slug: String? = "acme",
): Organization {
  val organization = mockk<Organization>(relaxed = true)
  every { organization.id } returns id
  every { organization.name } returns name
  every { organization.slug } returns slug
  every { organization.imageUrl } returns ""
  every { organization.hasImage } returns false
  every { organization.adminDeleteEnabled } returns true
  return organization
}

fun snapshotMembership(
  organizationId: String = "org_acme",
  organizationName: String = "Acme Inc.",
  roleName: String = "Admin",
): OrganizationMembership {
  val membership = mockk<OrganizationMembership>(relaxed = true)
  every { membership.id } returns "mem_$organizationId"
  every { membership.organization } returns snapshotOrganization(organizationId, organizationName)
  every { membership.role } returns "org:${roleName.lowercase()}"
  every { membership.roleName } returns roleName
  every { membership.permissions } returns
    listOf(
      "org:sys_profile:manage",
      "org:sys_memberships:read",
      "org:sys_domains:read",
      "org:sys_profile:delete",
    )
  every { membership.publicUserData } returns null
  return membership
}

fun snapshotUser(): User =
  mockk(relaxed = true) {
    every { id } returns "user_123"
    every { firstName } returns "Ava"
    every { lastName } returns "Stone"
    every { fullName } returns "Ava Stone"
    every { username } returns "ava"
    every { imageUrl } returns ""
    every { hasImage } returns false
    every { primaryEmailAddress } returns null
  }

fun snapshotRole(key: String, name: String): Role =
  mockk(relaxed = true) {
    every { this@mockk.key } returns key
    every { this@mockk.name } returns name
  }
