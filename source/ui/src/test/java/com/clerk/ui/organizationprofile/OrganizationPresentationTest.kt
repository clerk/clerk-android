package com.clerk.ui.organizationprofile

import com.clerk.api.OrganizationDomain
import com.clerk.api.OrganizationDomainVerification
import com.clerk.api.OrganizationDomainVerificationStatus
import com.clerk.api.OrganizationMembership
import io.mockk.every
import io.mockk.mockk
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class OrganizationPresentationTest {
  @Test
  fun `each organization action requires its own permission`() {
    val grants =
      listOf(
        "org:sys_profile:manage",
        "org:sys_profile:delete",
        "org:sys_memberships:read",
        "org:sys_memberships:manage",
        "org:sys_domains:read",
        "org:sys_domains:manage",
      )
    grants.forEachIndexed { index, grant ->
      val membership = mockk<OrganizationMembership>()
      every { membership.permissions } returns listOf(grant, "custom:permission")
      assertEquals(grants.indices.map { it == index }, actions(membership), grant)
    }
  }

  @Test
  fun `empty permissions hide all organization actions`() {
    val membership = mockk<OrganizationMembership>()
    for (permissions in listOf(emptyList(), listOf("custom:permission"))) {
      every { membership.permissions } returns permissions
      assertFalse(actions(membership).any { it })
    }
  }

  @Test
  fun `only verified domain status counts as verified`() {
    val statuses =
      listOf(
        OrganizationDomainVerificationStatus.Verified,
        OrganizationDomainVerificationStatus.Unverified,
        OrganizationDomainVerificationStatus.Failed,
        OrganizationDomainVerificationStatus.Expired,
        OrganizationDomainVerificationStatus.Unrecognized("future_status"),
        null,
      )
    for (status in statuses) {
      val domain = mockk<OrganizationDomain>()
      every { domain.verification } returns
        status?.let { OrganizationDomainVerification(it, 0.0, Instant.EPOCH) }
      assertEquals(
        status == OrganizationDomainVerificationStatus.Verified,
        domain.isVerified,
        status?.rawValue,
      )
    }
  }

  private fun actions(member: OrganizationMembership) =
    listOf(
      member.canManageProfile,
      member.canDeleteOrganization,
      member.canReadMemberships,
      member.canManageMemberships,
      member.canReadDomains,
      member.canManageDomains,
    )
}
