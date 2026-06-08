package com.clerk.ui.organizationlist

import com.clerk.api.organizations.OrganizationCreationDefaults
import com.clerk.api.organizations.OrganizationMembership
import com.clerk.api.organizations.OrganizationSuggestion
import com.clerk.api.organizations.UserOrganizationInvitation
import com.clerk.api.session.Session

internal const val PERSONAL_ACCOUNT_ACTION_ID = "personal_account"

internal data class OrganizationAccountListState(
  val isLoading: Boolean = true,
  val initialLoadAttempted: Boolean = false,
  val hasLoadedInitialResources: Boolean = false,
  val canCreateOrganization: Boolean = false,
  val memberships: List<OrganizationMembership> = emptyList(),
  val membershipsTotalCount: Int = 0,
  val invitations: List<UserOrganizationInvitation> = emptyList(),
  val invitationsTotalCount: Int = 0,
  val suggestions: List<OrganizationSuggestion> = emptyList(),
  val suggestionsTotalCount: Int = 0,
  val acceptedInvitationOrganizationIds: Set<String> = emptySet(),
  val isLoadingMoreMemberships: Boolean = false,
  val isLoadingMoreInvitations: Boolean = false,
  val isLoadingMoreSuggestions: Boolean = false,
  val activeActionId: String? = null,
  val creationDefaults: OrganizationCreationDefaults? = null,
  val errorMessage: String? = null,
  val completedSession: Session? = null,
) {
  val hasExistingResources: Boolean
    get() =
      memberships.isNotEmpty() ||
        invitations.isNotEmpty() ||
        suggestions.isNotEmpty() ||
        membershipsTotalCount > 0 ||
        invitationsTotalCount > 0 ||
        suggestionsTotalCount > 0

  val initialLoadFailed: Boolean
    get() = initialLoadAttempted && !isLoading && !hasLoadedInitialResources

  val canShowNoOrganizationHelp: Boolean
    get() =
      !isLoading && hasLoadedInitialResources && !hasExistingResources && !canCreateOrganization

  val canOnlyCreateOrganization: Boolean
    get() =
      !isLoading && hasLoadedInitialResources && !hasExistingResources && canCreateOrganization

  val membershipsHasNextPage: Boolean
    get() = memberships.size < membershipsTotalCount

  val invitationsHasNextPage: Boolean
    get() = pendingInvitationsCount < invitationsTotalCount

  val suggestionsHasNextPage: Boolean
    get() = suggestions.size < suggestionsTotalCount

  val hasNextPage: Boolean
    get() = membershipsHasNextPage || invitationsHasNextPage || suggestionsHasNextPage

  val isLoadingMore: Boolean
    get() = isLoadingMoreMemberships || isLoadingMoreInvitations || isLoadingMoreSuggestions

  val pendingInvitationsCount: Int
    get() =
      invitations.count {
        it.status != ACCEPTED_INVITATION_STATUS &&
          it.publicOrganizationData.id !in acceptedInvitationOrganizationIds
      }
}

private const val ACCEPTED_INVITATION_STATUS = "accepted"
