package com.clerk.ui.organizationprofile.members

import com.clerk.api.Clerk
import com.clerk.api.organizations.Organization
import com.clerk.api.organizations.OrganizationInvitation
import com.clerk.api.organizations.OrganizationMembership
import com.clerk.api.organizations.OrganizationMembershipRequest
import com.clerk.api.organizations.Role

internal enum class OrganizationMembersTab {
  Members,
  Invitations,
  Requests,
}

internal data class OrganizationMembersState(
  val availableTabs: List<OrganizationMembersTab> = emptyList(),
  val selectedTab: OrganizationMembersTab? = null,
  val isLoadingInitial: Boolean = false,
  val errorMessage: String? = null,
  val roles: List<Role> = emptyList(),
  val selectedInviteRoleKey: String? = null,
  val members: List<OrganizationMembership> = emptyList(),
  val membersTotalCount: Int = 0,
  val membersHasNextPage: Boolean = false,
  val isLoadingMoreMembers: Boolean = false,
  val memberQuery: String = "",
  val invitations: List<OrganizationInvitation> = emptyList(),
  val invitationsTotalCount: Int = 0,
  val invitationsHasNextPage: Boolean = false,
  val isLoadingMoreInvitations: Boolean = false,
  val requests: List<OrganizationMembershipRequest> = emptyList(),
  val requestsTotalCount: Int = 0,
  val requestsHasNextPage: Boolean = false,
  val isLoadingMoreRequests: Boolean = false,
  val hasRoleSetMigration: Boolean = false,
  val activeMutationId: String? = null,
  val inviteEmails: List<String> = emptyList(),
) {
  val canInvite: Boolean
    get() =
      OrganizationMembersTab.Invitations in availableTabs &&
        selectedInviteRoleKey != null &&
        inviteEmails.isNotEmpty() &&
        activeMutationId == null

  fun remainingInviteSlots(organization: Organization): Int {
    if (organization.maxAllowedMemberships <= 0) return Int.MAX_VALUE
    val existingMembers = organization.membersCount ?: membersTotalCount
    val pendingInvitations = organization.pendingInvitationsCount ?: invitationsTotalCount
    return (organization.maxAllowedMemberships - existingMembers - pendingInvitations)
      .coerceAtLeast(0)
  }

  fun inviteWouldExceedMembershipLimit(organization: Organization): Boolean {
    return inviteEmails.size > remainingInviteSlots(organization)
  }
}

internal data class OrganizationMembersActions(
  val onRetry: () -> Unit,
  val onSelectTab: (OrganizationMembersTab) -> Unit,
  val onMemberSearchChanged: (String) -> Unit,
  val onLoadMoreMembers: () -> Unit,
  val onLoadMoreInvitations: () -> Unit,
  val onLoadMoreRequests: () -> Unit,
  val onSelectInviteRole: (String) -> Unit,
  val onInviteInputSubmitted: (String) -> Unit,
  val onRemoveInviteEmail: (String) -> Unit,
  val onSendInvitations: () -> Unit,
  val onUpdateMemberRole: (OrganizationMembership, String) -> Unit,
  val onRemoveMember: (OrganizationMembership) -> Unit,
  val onRevokeInvitation: (OrganizationInvitation) -> Unit,
  val onAcceptRequest: (OrganizationMembershipRequest) -> Unit,
  val onRejectRequest: (OrganizationMembershipRequest) -> Unit,
)

internal fun organizationMembersAvailableTabs(
  membership: OrganizationMembership?,
  domainsEnabled: Boolean = Clerk.organizationDomainsIsEnabled,
): List<OrganizationMembersTab> {
  return buildList {
    if (membership?.canReadMemberships == true || membership?.canManageMemberships == true) {
      add(OrganizationMembersTab.Members)
    }
    if (membership?.canManageMemberships == true) {
      add(OrganizationMembersTab.Invitations)
    }
    if (domainsEnabled && membership?.canManageMemberships == true) {
      add(OrganizationMembersTab.Requests)
    }
  }
}
