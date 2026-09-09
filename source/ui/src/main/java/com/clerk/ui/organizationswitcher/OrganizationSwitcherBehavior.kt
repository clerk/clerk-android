package com.clerk.ui.organizationswitcher

import com.clerk.api.OrganizationMembership
import com.clerk.api.Session
import com.clerk.api.User

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
