@file:Suppress("TooManyFunctions")

package com.clerk.ui.organizationprofile.members

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clerk.api.Clerk
import com.clerk.api.network.ClerkPaginatedResponse
import com.clerk.api.network.serialization.ClerkResult
import com.clerk.api.network.serialization.errorMessage
import com.clerk.api.organizations.Organization
import com.clerk.api.organizations.OrganizationInvitation
import com.clerk.api.organizations.OrganizationMembership
import com.clerk.api.organizations.OrganizationMembershipRequest
import com.clerk.api.organizations.accept
import com.clerk.api.organizations.getInvitations
import com.clerk.api.organizations.getMembershipRequests
import com.clerk.api.organizations.getOrganizationMemberships
import com.clerk.api.organizations.getRolesPaginated
import com.clerk.api.organizations.reject
import com.clerk.api.organizations.removeMember
import com.clerk.api.organizations.revoke
import com.clerk.api.organizations.updateMembership
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.coroutines.launch

internal class OrganizationMembersViewModel(
  private val pageSize: Int = DEFAULT_PAGE_SIZE,
  private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ViewModel() {

  private val mutableState = MutableStateFlow(OrganizationMembersState())
  val state = mutableState.asStateFlow()

  private var organization: Organization? = null
  private var membership: OrganizationMembership? = null
  private var domainsEnabled: Boolean = false
  private var searchJob: Job? = null
  private var lastLoadRequest: MembersLoadRequest? = null

  fun load(
    organization: Organization,
    membership: OrganizationMembership?,
    domainsEnabled: Boolean = Clerk.organizationDomainsIsEnabled,
    initialTab: OrganizationMembersTab? = null,
    refreshKey: Int = 0,
  ) {
    val request =
      MembersLoadRequest(
        organizationId = organization.id,
        membershipId = membership?.id,
        initialTab = initialTab,
        refreshKey = refreshKey,
      )
    if (request == lastLoadRequest) return
    lastLoadRequest = request
    reload(organization, membership, domainsEnabled, initialTab)
  }

  fun retry() {
    val currentOrganization = organization ?: return
    reload(currentOrganization, membership, domainsEnabled, initialTab = null)
  }

  private fun reload(
    organization: Organization,
    membership: OrganizationMembership?,
    domainsEnabled: Boolean,
    initialTab: OrganizationMembersTab?,
  ) {
    this.organization = organization
    this.membership = membership
    this.domainsEnabled = domainsEnabled
    val tabs = organizationMembersAvailableTabs(membership, domainsEnabled)

    mutableState.update { current ->
      current.copy(
        availableTabs = tabs,
        selectedTab =
          current.selectedTab?.takeIf { it in tabs }
            ?: initialTab?.takeIf { it in tabs }
            ?: tabs.firstOrNull(),
        isLoadingInitial = true,
        errorMessage = null,
        isSearchingMembers = false,
        hasRoleSetMigration = false,
      )
    }

    viewModelScope.launch(dispatcher) {
      if (membership?.canManageMemberships == true) loadRoles()
      if (OrganizationMembersTab.Members in tabs) loadMembers(reset = true)
      if (OrganizationMembersTab.Invitations in tabs) loadInvitations(reset = true)
      if (OrganizationMembersTab.Requests in tabs) loadRequests(reset = true)
      mutableState.update { it.copy(isLoadingInitial = false) }
    }
  }

  fun selectTab(tab: OrganizationMembersTab) {
    mutableState.update { current ->
      if (tab in current.availableTabs) current.copy(selectedTab = tab) else current
    }
  }

  fun setMemberQuery(query: String) {
    if (query == mutableState.value.memberQuery) return
    mutableState.update { it.copy(memberQuery = query, isSearchingMembers = true) }
    searchJob?.cancel()
    searchJob =
      viewModelScope.launch(dispatcher) {
        delay(MEMBER_SEARCH_DEBOUNCE_MS)
        loadMembers(reset = true)
      }
  }

  fun submitMemberSearch() {
    searchJob?.cancel()
    searchJob = null
    mutableState.update { it.copy(isSearchingMembers = true) }
    viewModelScope.launch(dispatcher) { loadMembers(reset = true) }
  }

  fun loadMoreMembers() {
    if (!mutableState.value.membersHasNextPage || mutableState.value.isLoadingMoreMembers) return
    viewModelScope.launch(dispatcher) { loadMembers(reset = false) }
  }

  fun loadMoreInvitations() {
    if (!mutableState.value.invitationsHasNextPage || mutableState.value.isLoadingMoreInvitations) {
      return
    }
    viewModelScope.launch(dispatcher) { loadInvitations(reset = false) }
  }

  fun loadMoreRequests() {
    if (!mutableState.value.requestsHasNextPage || mutableState.value.isLoadingMoreRequests) return
    viewModelScope.launch(dispatcher) { loadRequests(reset = false) }
  }

  @Suppress("ReturnCount")
  fun updateMemberRole(membership: OrganizationMembership, role: String) {
    if (mutableState.value.hasRoleSetMigration) return
    val userId = membership.publicUserData?.userId
    if (userId == null) {
      mutableState.update { it.copy(errorMessage = "Member user ID is unavailable") }
      return
    }
    if (mutableState.value.activeMutationId != null) return

    mutableState.update { it.copy(activeMutationId = membership.id, errorMessage = null) }
    viewModelScope.launch(dispatcher) {
      when (val result = membership.updateMembership(userId = userId, role = role)) {
        is ClerkResult.Success ->
          mutableState.update { current ->
            current.copy(
              activeMutationId = null,
              members =
                current.members.map { member ->
                  if (member.id == membership.id) result.value else member
                },
            )
          }
        is ClerkResult.Failure -> mutationFailed(result.errorMessage)
      }
    }
  }

  @Suppress("ReturnCount")
  fun removeMember(membership: OrganizationMembership) {
    val currentOrganization = organization ?: return
    val userId = membership.publicUserData?.userId
    if (userId == null) {
      mutableState.update { it.copy(errorMessage = "Member user ID is unavailable") }
      return
    }
    if (mutableState.value.activeMutationId != null) return

    mutableState.update { it.copy(activeMutationId = membership.id, errorMessage = null) }
    viewModelScope.launch(dispatcher) {
      when (val result = currentOrganization.removeMember(userId = userId)) {
        is ClerkResult.Success ->
          mutableState.update { current ->
            current.copy(
              activeMutationId = null,
              members = current.members.filterNot { it.id == membership.id },
              membersTotalCount = (current.membersTotalCount - 1).coerceAtLeast(0),
            )
          }
        is ClerkResult.Failure -> mutationFailed(result.errorMessage)
      }
    }
  }

  fun revokeInvitation(invitation: OrganizationInvitation) {
    if (mutableState.value.activeMutationId != null) return
    mutableState.update { it.copy(activeMutationId = invitation.id, errorMessage = null) }
    viewModelScope.launch(dispatcher) {
      when (val result = invitation.revoke()) {
        is ClerkResult.Success ->
          mutableState.update { current ->
            current.copy(
              activeMutationId = null,
              invitations = current.invitations.filterNot { it.id == invitation.id },
              invitationsTotalCount = (current.invitationsTotalCount - 1).coerceAtLeast(0),
            )
          }
        is ClerkResult.Failure -> mutationFailed(result.errorMessage)
      }
    }
  }

  fun acceptRequest(request: OrganizationMembershipRequest) {
    updateMembershipRequest(request = request, accept = true)
  }

  fun rejectRequest(request: OrganizationMembershipRequest) {
    updateMembershipRequest(request = request, accept = false)
  }

  fun clearError() {
    mutableState.update { it.copy(errorMessage = null) }
  }

  private suspend fun loadRoles() {
    val currentOrganization = organization ?: return
    when (val result = currentOrganization.getRolesPaginated()) {
      is ClerkResult.Success ->
        mutableState.update {
          it.copy(
            roles = result.value.data,
            hasRoleSetMigration = result.value.hasRoleSetMigration == true,
          )
        }
      is ClerkResult.Failure -> mutableState.update { it.copy(errorMessage = result.errorMessage) }
    }
  }

  private suspend fun loadMembers(reset: Boolean) {
    val currentOrganization =
      organization
        ?: run {
          mutableState.update { it.copy(isSearchingMembers = false, isLoadingMoreMembers = false) }
          return
        }
    val current = mutableState.updateAndGet {
      it.copy(
        isSearchingMembers = reset && !it.isLoadingInitial,
        isLoadingMoreMembers = !reset,
        errorMessage = null,
        membersHasNextPage = if (reset) false else it.membersHasNextPage,
      )
    }

    val offset = if (reset) 0 else current.members.size
    when (
      val result =
        currentOrganization.getOrganizationMemberships(
          query = current.memberQuery.trim().takeIf { it.isNotEmpty() },
          limit = pageSize,
          offset = offset,
        )
    ) {
      is ClerkResult.Success -> applyMembersPage(result.value, append = !reset)
      is ClerkResult.Failure ->
        mutableState.update {
          it.copy(
            isSearchingMembers = false,
            isLoadingMoreMembers = false,
            errorMessage = result.errorMessage,
          )
        }
    }
  }

  private fun applyMembersPage(
    page: ClerkPaginatedResponse<OrganizationMembership>,
    append: Boolean,
  ) {
    mutableState.update { current ->
      val memberships = if (append) current.members + page.data else page.data
      current.copy(
        members = memberships,
        membersTotalCount = page.totalCount,
        membersHasNextPage = memberships.size < page.totalCount,
        isSearchingMembers = false,
        isLoadingMoreMembers = false,
        hasRoleSetMigration = current.hasRoleSetMigration || page.hasRoleSetMigration == true,
      )
    }
  }

  private suspend fun loadInvitations(reset: Boolean) {
    val currentOrganization = organization ?: return
    val current = mutableState.updateAndGet {
      it.copy(isLoadingMoreInvitations = !reset, errorMessage = null)
    }
    val offset = if (reset) 0 else current.invitations.size
    when (
      val result =
        currentOrganization.getInvitations(
          limit = pageSize,
          offset = offset,
          status = OrganizationInvitation.Status.Pending,
        )
    ) {
      is ClerkResult.Success ->
        mutableState.update { latest ->
          val invitations = if (reset) result.value.data else latest.invitations + result.value.data
          latest.copy(
            invitations = invitations,
            invitationsTotalCount = result.value.totalCount,
            invitationsHasNextPage = invitations.size < result.value.totalCount,
            isLoadingMoreInvitations = false,
          )
        }
      is ClerkResult.Failure ->
        mutableState.update {
          it.copy(isLoadingMoreInvitations = false, errorMessage = result.errorMessage)
        }
    }
  }

  private suspend fun loadRequests(reset: Boolean) {
    val currentOrganization = organization ?: return
    val current = mutableState.updateAndGet {
      it.copy(isLoadingMoreRequests = !reset, errorMessage = null)
    }
    val offset = if (reset) 0 else current.requests.size
    when (
      val result =
        currentOrganization.getMembershipRequests(
          limit = pageSize,
          offset = offset,
          status = REQUEST_PENDING_STATUS,
        )
    ) {
      is ClerkResult.Success ->
        mutableState.update { latest ->
          val requests = if (reset) result.value.data else latest.requests + result.value.data
          latest.copy(
            requests = requests,
            requestsTotalCount = result.value.totalCount,
            requestsHasNextPage = requests.size < result.value.totalCount,
            isLoadingMoreRequests = false,
          )
        }
      is ClerkResult.Failure ->
        mutableState.update {
          it.copy(isLoadingMoreRequests = false, errorMessage = result.errorMessage)
        }
    }
  }

  private fun updateMembershipRequest(request: OrganizationMembershipRequest, accept: Boolean) {
    if (mutableState.value.activeMutationId != null) return
    mutableState.update { it.copy(activeMutationId = request.id, errorMessage = null) }
    viewModelScope.launch(dispatcher) {
      val result = if (accept) request.accept() else request.reject()
      when (result) {
        is ClerkResult.Success ->
          mutableState.update { current ->
            current.copy(
              activeMutationId = null,
              requests = current.requests.filterNot { it.id == request.id },
              requestsTotalCount = (current.requestsTotalCount - 1).coerceAtLeast(0),
            )
          }
        is ClerkResult.Failure -> mutationFailed(result.errorMessage)
      }
    }
  }

  private fun mutationFailed(errorMessage: String) {
    mutableState.update { it.copy(activeMutationId = null, errorMessage = errorMessage) }
  }

  private data class MembersLoadRequest(
    val organizationId: String,
    val membershipId: String?,
    val initialTab: OrganizationMembersTab?,
    val refreshKey: Int,
  )
}

private const val DEFAULT_PAGE_SIZE = 20
private const val MEMBER_SEARCH_DEBOUNCE_MS = 300L
private const val REQUEST_PENDING_STATUS = "pending"
