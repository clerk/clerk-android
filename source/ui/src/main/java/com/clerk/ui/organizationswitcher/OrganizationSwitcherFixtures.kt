package com.clerk.ui.organizationswitcher

import com.clerk.api.organizations.Organization
import com.clerk.api.organizations.OrganizationMembership
import com.clerk.api.user.User
import kotlinx.serialization.json.JsonNull

internal fun previewOrganizationMembership(
  organizationId: String = "org_acme",
  organizationName: String = "Acme Inc.",
  roleName: String = "Admin",
): OrganizationMembership {
  return OrganizationMembership(
    id = "mem_$organizationId",
    publicMetadata = JsonNull,
    role = "org:${roleName.lowercase()}",
    roleName = roleName,
    organization =
      Organization(
        id = organizationId,
        name = organizationName,
        slug = organizationName.lowercase().replace(" ", "-"),
        imageUrl = "",
        maxAllowedMemberships = 0,
        adminDeleteEnabled = true,
        createdAt = 1,
        updatedAt = 1,
        publicMetadata = JsonNull,
      ),
    createdAt = 1,
    updatedAt = 1,
  )
}

internal fun previewOrganizationSwitcherUser(): User {
  return User(
    id = "user_123",
    firstName = "Ava",
    lastName = "Stone",
    username = "ava",
    imageUrl = "",
    hasImage = false,
    passkeys = emptyList(),
    passwordEnabled = true,
    phoneNumbers = emptyList(),
    totpEnabled = false,
    twoFactorEnabled = false,
    updatedAt = 1,
  )
}
