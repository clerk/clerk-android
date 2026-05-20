package com.clerk.ui.organizationprofile.root

import com.clerk.api.Clerk
import com.clerk.api.organizations.Organization
import com.clerk.api.organizations.OrganizationMembership
import com.clerk.ui.organizationprofile.custom.OrganizationProfileRow

internal fun organizationProfileRows(
  membership: OrganizationMembership?,
  domainsEnabled: Boolean = Clerk.organizationDomainsIsEnabled,
): List<OrganizationProfileRow> {
  return buildList {
    if (membership?.canReadMemberships == true || membership?.canManageMemberships == true) {
      add(OrganizationProfileRow.Members)
    }
    if (
      domainsEnabled && (membership?.canReadDomains == true || membership?.canManageDomains == true)
    ) {
      add(OrganizationProfileRow.VerifiedDomains)
    }
  }
}

internal fun organizationProfileActionRows(
  organization: Organization,
  membership: OrganizationMembership?,
  adminDeleteEnabled: Boolean = Clerk.organizationAdminDeleteIsEnabled,
): List<OrganizationProfileRow> {
  return buildList {
    if (membership != null) {
      add(OrganizationProfileRow.LeaveOrganization)
    }
    if (
      adminDeleteEnabled &&
        organization.adminDeleteEnabled &&
        membership?.canDeleteOrganization == true
    ) {
      add(OrganizationProfileRow.DeleteOrganization)
    }
  }
}
