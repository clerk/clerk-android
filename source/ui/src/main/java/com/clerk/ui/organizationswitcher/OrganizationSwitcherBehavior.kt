package com.clerk.ui.organizationswitcher

import com.clerk.api.organizations.OrganizationMembership
import com.clerk.api.session.Session
import com.clerk.api.user.User

internal fun activeOrganizationMembership(
  user: User?,
  session: Session?,
  loadedMemberships: List<OrganizationMembership>,
): OrganizationMembership? {
  val activeOrganizationId = session?.lastActiveOrganizationId ?: return null
  return (loadedMemberships + user?.organizationMemberships.orEmpty()).firstOrNull {
    it.organization.id == activeOrganizationId
  }
}

internal fun shouldShowOrganizationSwitcher(
  hasUser: Boolean,
  hasSession: Boolean,
  organizationsEnabled: Boolean,
): Boolean {
  return hasUser && hasSession && organizationsEnabled
}
