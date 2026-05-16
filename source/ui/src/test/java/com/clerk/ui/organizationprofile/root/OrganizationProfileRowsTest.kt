package com.clerk.ui.organizationprofile.root

import com.clerk.api.organizations.OrganizationSystemPermission
import com.clerk.ui.organizationprofile.custom.OrganizationProfileRow
import com.clerk.ui.organizationprofile.previewOrganizationProfileMembership
import com.clerk.ui.organizationprofile.previewOrganizationProfileOrganization
import kotlin.test.Test
import kotlin.test.assertEquals

class OrganizationProfileRowsTest {

  @Test
  fun `profile rows include members and domains when permissions allow`() {
    val membership =
      previewOrganizationProfileMembership(
        permissions =
          listOf(
            OrganizationSystemPermission.READ_MEMBERSHIPS,
            OrganizationSystemPermission.READ_DOMAINS,
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
            OrganizationSystemPermission.READ_MEMBERSHIPS,
            OrganizationSystemPermission.READ_DOMAINS,
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
        permissions = listOf(OrganizationSystemPermission.DELETE_PROFILE),
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
        permissions = listOf(OrganizationSystemPermission.DELETE_PROFILE),
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
}
