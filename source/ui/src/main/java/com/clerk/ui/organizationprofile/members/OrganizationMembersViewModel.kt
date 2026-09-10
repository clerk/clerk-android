@file:Suppress("TooManyFunctions")

package com.clerk.ui.organizationprofile.members

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clerk.api.*
import com.clerk.ui.core.common.displayMessage
import com.clerk.ui.core.common.runUiOperation
import com.clerk.ui.organizationprofile.*
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

internal class OrganizationMembersViewModel(
  private val clerk: Clerk,
  private val pageSize: Int = DEFAULT_PAGE_SIZE,
  private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ViewModel() {

  private val mutableState = MutableStateFlow(OrganizationMembersState())
  val state = mutableState.asStateFlow()

  private var organization: Organization? = null
  private var membership: OrganizationMembership? = null
  private var domainsEnabled: Boolean = false
  private var searchJob: Job? = null

  fun load(
    organization: Organization,
    membership: OrganizationMembership?,
    domainsEnabled: Boolean = clerk.environment.organizationSettings.domains.enabled,
    initialTab: OrganizationMembersTab? = null,
  ) {
    this.organization = organization
    this.membership = membership
    this.domainsEnabled = domainsEnabled
    val tabs = organizationMembersAvailableTabs(membership, domainsEnabled)
    val selectedTab =
      mutableState.value.selectedTab?.takeIf { it in tabs }
        ?: initialTab?.takeIf { it in tabs }
        ?: tabs.firstOrNull()

    mutableState.value =
      mutableState.value.copy(
        availableTabs = tabs,
        selectedTab = selectedTab,
        isLoadingInitial = true,
        errorMessage = null,
        isSearchingMembers = false,
        hasRoleSetMigration = false,
      )

    viewModelScope.launch(dispatcher) {
      if (membership?.canManageMemberships == true) loadRoles()
      if (OrganizationMembersTab.Members in tabs) loadMembers(reset = true)
      if (OrganizationMembersTab.Invitations in tabs) loadInvitations(reset = true)
      if (OrganizationMembersTab.Requests in tabs) loadRequests(reset = true)
      mutableState.value = mutableState.value.copy(isLoadingInitial = false)
    }
  }

  fun retry() {
    val currentOrganization = organization ?: return
    load(currentOrganization, membership, domainsEnabled)
  }

  fun selectTab(tab: OrganizationMembersTab) {
    if (tab in mutableState.value.availableTabs) {
      mutableState.value = mutableState.value.copy(selectedTab = tab)
    }
  }

  fun setMemberQuery(query: String) {
    if (query == mutableState.value.memberQuery) return
    mutableState.value = mutableState.value.copy(memberQuery = query, isSearchingMembers = true)
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
    mutableState.value = mutableState.value.copy(isSearchingMembers = true)
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
      mutableState.value = mutableState.value.copy(errorMessage = "Member user ID is unavailable")
      return
    }
    if (mutableState.value.activeMutationId != null) return

    mutableState.value =
      mutableState.value.copy(activeMutationId = membership.id, errorMessage = null)
    viewModelScope.launch(dispatcher) {
      runUiOperation { membership.update(UpdateOrganizationMembershipParams(role)) }
        .onSuccess { result ->
          mutableState.value =
            mutableState.value.copy(
              activeMutationId = null,
              members =
                mutableState.value.members.map { current ->
                  if (current.id == membership.id) result else current
                },
            )
        }
        .onFailure { failure ->
          mutationFailed(failure.displayMessage)
        }
    }
  }

  @Suppress("ReturnCount")
  fun removeMember(membership: OrganizationMembership) {
    val currentOrganization = organization ?: return
    val userId = membership.publicUserData?.userId
    if (userId == null) {
      mutableState.value = mutableState.value.copy(errorMessage = "Member user ID is unavailable")
      return
    }
    if (mutableState.value.activeMutationId != null) return

    mutableState.value =
      mutableState.value.copy(activeMutationId = membership.id, errorMessage = null)
    viewModelScope.launch(dispatcher) {
      runUiOperation { currentOrganization.removeMember(userId) }
        .onSuccess { result ->
          mutableState.value =
            mutableState.value.copy(
              activeMutationId = null,
              members = mutableState.value.members.filterNot { it.id == membership.id },
              membersTotalCount = (mutableState.value.membersTotalCount - 1).coerceAtLeast(0),
            )
        }
        .onFailure { failure ->
          mutationFailed(failure.displayMessage)
        }
    }
  }

  fun revokeInvitation(invitation: OrganizationInvitation) {
    if (mutableState.value.activeMutationId != null) return
    mutableState.value =
      mutableState.value.copy(activeMutationId = invitation.id, errorMessage = null)
    viewModelScope.launch(dispatcher) {
      runUiOperation { invitation.revoke() }
        .onSuccess { result ->
          mutableState.value =
            mutableState.value.copy(
              activeMutationId = null,
              invitations = mutableState.value.invitations.filterNot { it.id == invitation.id },
              invitationsTotalCount =
                (mutableState.value.invitationsTotalCount - 1).coerceAtLeast(0),
            )
        }
        .onFailure { failure ->
          mutationFailed(failure.displayMessage)
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
    mutableState.value = mutableState.value.copy(errorMessage = null)
  }

  private suspend fun loadRoles() {
    val currentOrganization = organization ?: return
    runUiOperation { currentOrganization.getRoles() }
      .onSuccess { result ->
        mutableState.value =
          mutableState.value.copy(
            roles = result.data,
            hasRoleSetMigration = result.hasRoleSetMigration == true,
          )
      }
      .onFailure { failure ->
        mutableState.value = mutableState.value.copy(errorMessage = failure.displayMessage)
      }
  }

  private suspend fun loadMembers(reset: Boolean) {
    val currentOrganization =
      organization
        ?: run {
          mutableState.value =
            mutableState.value.copy(isSearchingMembers = false, isLoadingMoreMembers = false)
          return
        }
    val current = mutableState.value
    mutableState.value =
      current.copy(
        isSearchingMembers = reset && !current.isLoadingInitial,
        isLoadingMoreMembers = !reset,
        errorMessage = null,
        membersHasNextPage = if (reset) false else current.membersHasNextPage,
      )

    val offset = if (reset) 0 else current.members.size
    runUiOperation {
        currentOrganization.getMemberships(
          GetMembersParams(
            query = current.memberQuery.trim().takeIf { it.isNotEmpty() },
            pageSize = pageSize.toDouble(),
            initialPage = (offset / pageSize + 1).toDouble(),
          )
        )
      }
      .onSuccess { result ->
        applyMembersPage(result, append = !reset)
      }
      .onFailure { failure ->
        mutableState.value =
          mutableState.value.copy(
            isSearchingMembers = false,
            isLoadingMoreMembers = false,
            errorMessage = failure.displayMessage,
          )
      }
  }

  private fun applyMembersPage(
    page: ClerkPaginatedResponseOrganizationMembership,
    append: Boolean,
  ) {
    val memberships =
      if (append) (mutableState.value.members + page.data).distinctBy { it.id } else page.data
    mutableState.value =
      mutableState.value.copy(
        members = memberships,
        membersTotalCount = page.totalCount.toInt(),
        membersHasNextPage = memberships.size < page.totalCount,
        isSearchingMembers = false,
        isLoadingMoreMembers = false,
        hasRoleSetMigration = mutableState.value.hasRoleSetMigration,
      )
  }

  private suspend fun loadInvitations(reset: Boolean) {
    val currentOrganization = organization ?: return
    val current = mutableState.value
    mutableState.value = current.copy(isLoadingMoreInvitations = !reset, errorMessage = null)
    val offset = if (reset) 0 else current.invitations.size
    runUiOperation {
        currentOrganization.getInvitations(
          GetInvitationsParams(
            pageSize = pageSize.toDouble(),
            initialPage = (offset / pageSize + 1).toDouble(),
            status = listOf(OrganizationInvitationStatus.Pending),
          )
        )
      }
      .onSuccess { result ->
        val invitations =
          if (reset) result.data
          else (mutableState.value.invitations + result.data).distinctBy { it.id }
        mutableState.value =
          mutableState.value.copy(
            invitations = invitations,
            invitationsTotalCount = result.totalCount.toInt(),
            invitationsHasNextPage = invitations.size < result.totalCount,
            isLoadingMoreInvitations = false,
          )
      }
      .onFailure { failure ->
        mutableState.value =
          mutableState.value.copy(
            isLoadingMoreInvitations = false,
            errorMessage = failure.displayMessage,
          )
      }
  }

  private suspend fun loadRequests(reset: Boolean) {
    val currentOrganization = organization ?: return
    val current = mutableState.value
    mutableState.value = current.copy(isLoadingMoreRequests = !reset, errorMessage = null)
    val offset = if (reset) 0 else current.requests.size
    runUiOperation {
        currentOrganization.getMembershipRequests(
          GetMembershipRequestParams(
            pageSize = pageSize.toDouble(),
            initialPage = (offset / pageSize + 1).toDouble(),
            status = OrganizationInvitationStatus.Pending,
          )
        )
      }
      .onSuccess { result ->
        val requests =
          if (reset) result.data
          else (mutableState.value.requests + result.data).distinctBy { it.id }
        mutableState.value =
          mutableState.value.copy(
            requests = requests,
            requestsTotalCount = result.totalCount.toInt(),
            requestsHasNextPage = requests.size < result.totalCount,
            isLoadingMoreRequests = false,
          )
      }
      .onFailure { failure ->
        mutableState.value =
          mutableState.value.copy(
            isLoadingMoreRequests = false,
            errorMessage = failure.displayMessage,
          )
      }
  }

  private fun updateMembershipRequest(request: OrganizationMembershipRequest, accept: Boolean) {
    if (mutableState.value.activeMutationId != null) return
    mutableState.value = mutableState.value.copy(activeMutationId = request.id, errorMessage = null)
    viewModelScope.launch(dispatcher) {
      runUiOperation { if (accept) request.accept() else request.reject() }
        .onSuccess { result ->
          mutableState.value =
            mutableState.value.copy(
              activeMutationId = null,
              requests = mutableState.value.requests.filterNot { it.id == request.id },
              requestsTotalCount = (mutableState.value.requestsTotalCount - 1).coerceAtLeast(0),
            )
        }
        .onFailure { failure ->
          mutationFailed(failure.displayMessage)
        }
    }
  }

  private fun mutationFailed(errorMessage: String) {
    mutableState.value =
      mutableState.value.copy(activeMutationId = null, errorMessage = errorMessage)
  }
}

private const val DEFAULT_PAGE_SIZE = 20
private const val MEMBER_SEARCH_DEBOUNCE_MS = 300L
private const val REQUEST_PENDING_STATUS = "pending"
