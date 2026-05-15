package com.clerk.ui.organizationprofile

import com.clerk.api.organizations.Organization
import com.clerk.api.organizations.OrganizationMembership
import com.clerk.api.organizations.OrganizationSystemPermission
import kotlinx.serialization.json.JsonNull

internal fun previewOrganizationProfileOrganization(
  id: String = "org_acme",
  name: String = "Acme Inc.",
  slug: String? = "acme",
): Organization {
  return Organization(
    id = id,
    name = name,
    slug = slug,
    imageUrl = "",
    maxAllowedMemberships = 0,
    adminDeleteEnabled = true,
    createdAt = 1,
    updatedAt = 1,
    publicMetadata = JsonNull,
  )
}

internal fun previewOrganizationProfileMembership(
  organization: Organization = previewOrganizationProfileOrganization(),
  permissions: List<OrganizationSystemPermission> =
    listOf(
      OrganizationSystemPermission.MANAGE_PROFILE,
      OrganizationSystemPermission.READ_MEMBERSHIPS,
      OrganizationSystemPermission.READ_DOMAINS,
      OrganizationSystemPermission.DELETE_PROFILE,
    ),
): OrganizationMembership {
  return OrganizationMembership(
    id = "mem_${organization.id}",
    publicMetadata = JsonNull,
    role = "org:admin",
    roleName = "Admin",
    permissions = permissions.map { it.value },
    organization = organization,
    createdAt = 1,
    updatedAt = 1,
  )
}
