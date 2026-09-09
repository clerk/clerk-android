package com.clerk.ui.organizationprofile.root

import com.clerk.api.*
import com.clerk.ui.organizationprofile.custom.OrganizationProfileRow
import io.mockk.*
import kotlin.test.Test
import kotlin.test.assertEquals

class OrganizationProfileRowsTest {

  @Test
  fun `profile rows include members and domains when permissions allow`() {
    val membership =
      previewOrganizationProfileMembership(
        permissions =
          listOf(
            "org:sys_memberships:read",
            "org:sys_domains:read",
          )
      )

    assertEquals(
      listOf(OrganizationProfileRow.Members, OrganizationProfileRow.VerifiedDomains),
      organizationProfileRows(membership = membership, domainsEnabled = true),
    )
  }

  @Test
  fun `profile rows hide domains when domains are disabled`() {
    val membership =
      previewOrganizationProfileMembership(
        permissions =
          listOf(
            "org:sys_memberships:read",
            "org:sys_domains:read",
          )
      )

    assertEquals(
      listOf(OrganizationProfileRow.Members),
      organizationProfileRows(membership = membership, domainsEnabled = false),
    )
  }

  @Test
  fun `action rows include leave and delete when delete is allowed`() {
    val organization = previewOrganizationProfileOrganization()
    val membership =
      previewOrganizationProfileMembership(
        organization = organization,
        permissions = listOf("org:sys_profile:delete"),
      )

    assertEquals(
      listOf(OrganizationProfileRow.LeaveOrganization, OrganizationProfileRow.DeleteOrganization),
      organizationProfileActionRows(
        organization = organization,
        membership = membership,
        adminDeleteEnabled = true,
      ),
    )
  }

  @Test
  fun `action rows hide delete when admin delete is disabled`() {
    val organization = previewOrganizationProfileOrganization()
    val membership =
      previewOrganizationProfileMembership(
        organization = organization,
        permissions = listOf("org:sys_profile:delete"),
      )

    assertEquals(
      listOf(OrganizationProfileRow.LeaveOrganization),
      organizationProfileActionRows(
        organization = organization,
        membership = membership,
        adminDeleteEnabled = false,
      ),
    )
  }

  private fun previewOrganizationProfileOrganization(): Organization =
    mockk(relaxed = true) { every { adminDeleteEnabled } returns true }

  private fun previewOrganizationProfileMembership(
    organization: Organization = previewOrganizationProfileOrganization(),
    permissions: List<String>,
  ): OrganizationMembership {
    val membership = mockk<OrganizationMembership>(relaxed = true)
    every { membership.organization } returns organization
    every { membership.permissions } returns permissions
    return membership
  }
}
