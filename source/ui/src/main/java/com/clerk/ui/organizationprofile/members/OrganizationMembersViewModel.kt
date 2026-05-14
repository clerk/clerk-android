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
import com.clerk.api.organizations.Role
import com.clerk.api.organizations.accept
import com.clerk.api.organizations.bulkCreateInvitations
import com.clerk.api.organizations.getInvitations
import com.clerk.api.organizations.getMembershipRequests
import com.clerk.api.organizations.getOrganizationMemberships
import com.clerk.api.organizations.getRolesPaginated
import com.clerk.api.organizations.reject
import com.clerk.api.organizations.removeMember
import com.clerk.api.organizations.revoke
import com.clerk.api.organizations.updateMembership
import com.clerk.ui.organizationprofile.invite.parseInviteEmailAddresses
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
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

  fun load(
    organization: Organization,
    membership: OrganizationMembership?,
    domainsEnabled: Boolean = Clerk.organizationDomainsIsEnabled,
  ) {
    this.organization = organization
    this.membership = membership
    this.domainsEnabled = domainsEnabled
    val tabs = organizationMembersAvailableTabs(membership, domainsEnabled)
    val selectedTab = mutableState.value.selectedTab?.takeIf { it in tabs } ?: tabs.firstOrNull()

    mutableState.value =
      mutableState.value.copy(
        availableTabs = tabs,
        selectedTab = selectedTab,
        isLoadingInitial = true,
        errorMessage = null,
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
    mutableState.value = mutableState.value.copy(memberQuery = query)
    searchJob?.cancel()
    searchJob =
      viewModelScope.launch(dispatcher) {
        delay(MEMBER_SEARCH_DEBOUNCE_MS)
        loadMembers(reset = true)
      }
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

  fun selectInviteRole(roleKey: String) {
    mutableState.value = mutableState.value.copy(selectedInviteRoleKey = roleKey)
  }

  fun addInviteEmails(input: String) {
    val emails = parseInviteEmailAddresses(input)
    if (emails.isEmpty()) return

    mutableState.value =
      mutableState.value.copy(inviteEmails = (mutableState.value.inviteEmails + emails).distinct())
  }

  fun removeInviteEmail(email: String) {
    mutableState.value =
      mutableState.value.copy(inviteEmails = mutableState.value.inviteEmails - email)
  }

  @Suppress("ReturnCount")
  fun sendInvitations() {
    val currentOrganization = organization ?: return
    val currentState = mutableState.value
    val role = currentState.selectedInviteRoleKey ?: return
    val emailAddresses = currentState.inviteEmails
    if (emailAddresses.isEmpty() || currentState.activeMutationId != null) return
    if (currentState.inviteWouldExceedMembershipLimit(currentOrganization)) {
      mutableState.value =
        currentState.copy(errorMessage = "Invite limit would exceed allowed memberships")
      return
    }

    mutableState.value =
      currentState.copy(activeMutationId = INVITE_MUTATION_ID, errorMessage = null)
    viewModelScope.launch(dispatcher) {
      when (
        val result =
          currentOrganization.bulkCreateInvitations(emailAddresses = emailAddresses, role = role)
      ) {
        is ClerkResult.Success -> {
          mutableState.value =
            mutableState.value.copy(activeMutationId = null, inviteEmails = emptyList())
          loadInvitations(reset = true)
        }
        is ClerkResult.Failure -> mutationFailed(result.errorMessage)
      }
    }
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
      when (val result = membership.updateMembership(userId = userId, role = role)) {
        is ClerkResult.Success -> {
          mutableState.value =
            mutableState.value.copy(
              activeMutationId = null,
              members =
                mutableState.value.members.map { current ->
                  if (current.id == membership.id) result.value else current
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
      mutableState.value = mutableState.value.copy(errorMessage = "Member user ID is unavailable")
      return
    }
    if (mutableState.value.activeMutationId != null) return

    mutableState.value =
      mutableState.value.copy(activeMutationId = membership.id, errorMessage = null)
    viewModelScope.launch(dispatcher) {
      when (val result = currentOrganization.removeMember(userId = userId)) {
        is ClerkResult.Success -> {
          mutableState.value =
            mutableState.value.copy(
              activeMutationId = null,
              members = mutableState.value.members.filterNot { it.id == membership.id },
              membersTotalCount = (mutableState.value.membersTotalCount - 1).coerceAtLeast(0),
            )
        }
        is ClerkResult.Failure -> mutationFailed(result.errorMessage)
      }
    }
  }

  fun revokeInvitation(invitation: OrganizationInvitation) {
    if (mutableState.value.activeMutationId != null) return
    mutableState.value =
      mutableState.value.copy(activeMutationId = invitation.id, errorMessage = null)
    viewModelScope.launch(dispatcher) {
      when (val result = invitation.revoke()) {
        is ClerkResult.Success -> {
          mutableState.value =
            mutableState.value.copy(
              activeMutationId = null,
              invitations = mutableState.value.invitations.filterNot { it.id == invitation.id },
              invitationsTotalCount =
                (mutableState.value.invitationsTotalCount - 1).coerceAtLeast(0),
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
    mutableState.value = mutableState.value.copy(errorMessage = null)
  }

  private suspend fun loadRoles() {
    val currentOrganization = organization ?: return
    when (val result = currentOrganization.getRolesPaginated()) {
      is ClerkResult.Success ->
        mutableState.value =
          mutableState.value.copy(
            roles = result.value.data,
            selectedInviteRoleKey = defaultInviteRole(result.value.data),
            hasRoleSetMigration = result.value.hasRoleSetMigration == true,
          )
      is ClerkResult.Failure ->
        mutableState.value = mutableState.value.copy(errorMessage = result.errorMessage)
    }
  }

  private suspend fun loadMembers(reset: Boolean) {
    val currentOrganization = organization ?: return
    val current = mutableState.value
    mutableState.value =
      current.copy(
        isLoadingMoreMembers = !reset,
        errorMessage = null,
        membersHasNextPage = if (reset) false else current.membersHasNextPage,
      )

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
        mutableState.value =
          mutableState.value.copy(isLoadingMoreMembers = false, errorMessage = result.errorMessage)
    }
  }

  private fun applyMembersPage(
    page: ClerkPaginatedResponse<OrganizationMembership>,
    append: Boolean,
  ) {
    val memberships = if (append) mutableState.value.members + page.data else page.data
    mutableState.value =
      mutableState.value.copy(
        members = memberships,
        membersTotalCount = page.totalCount,
        membersHasNextPage = memberships.size < page.totalCount,
        isLoadingMoreMembers = false,
        hasRoleSetMigration =
          mutableState.value.hasRoleSetMigration || page.hasRoleSetMigration == true,
      )
  }

  private suspend fun loadInvitations(reset: Boolean) {
    val currentOrganization = organization ?: return
    val current = mutableState.value
    mutableState.value = current.copy(isLoadingMoreInvitations = !reset, errorMessage = null)
    val offset = if (reset) 0 else current.invitations.size
    when (
      val result =
        currentOrganization.getInvitations(
          limit = pageSize,
          offset = offset,
          status = OrganizationInvitation.Status.Pending,
        )
    ) {
      is ClerkResult.Success -> {
        val invitations =
          if (reset) result.value.data else mutableState.value.invitations + result.value.data
        mutableState.value =
          mutableState.value.copy(
            invitations = invitations,
            invitationsTotalCount = result.value.totalCount,
            invitationsHasNextPage = invitations.size < result.value.totalCount,
            isLoadingMoreInvitations = false,
          )
      }
      is ClerkResult.Failure ->
        mutableState.value =
          mutableState.value.copy(
            isLoadingMoreInvitations = false,
            errorMessage = result.errorMessage,
          )
    }
  }

  private suspend fun loadRequests(reset: Boolean) {
    val currentOrganization = organization ?: return
    val current = mutableState.value
    mutableState.value = current.copy(isLoadingMoreRequests = !reset, errorMessage = null)
    val offset = if (reset) 0 else current.requests.size
    when (
      val result =
        currentOrganization.getMembershipRequests(
          limit = pageSize,
          offset = offset,
          status = REQUEST_PENDING_STATUS,
        )
    ) {
      is ClerkResult.Success -> {
        val requests =
          if (reset) result.value.data else mutableState.value.requests + result.value.data
        mutableState.value =
          mutableState.value.copy(
            requests = requests,
            requestsTotalCount = result.value.totalCount,
            requestsHasNextPage = requests.size < result.value.totalCount,
            isLoadingMoreRequests = false,
          )
      }
      is ClerkResult.Failure ->
        mutableState.value =
          mutableState.value.copy(isLoadingMoreRequests = false, errorMessage = result.errorMessage)
    }
  }

  private fun updateMembershipRequest(request: OrganizationMembershipRequest, accept: Boolean) {
    if (mutableState.value.activeMutationId != null) return
    mutableState.value = mutableState.value.copy(activeMutationId = request.id, errorMessage = null)
    viewModelScope.launch(dispatcher) {
      val result = if (accept) request.accept() else request.reject()
      when (result) {
        is ClerkResult.Success ->
          mutableState.value =
            mutableState.value.copy(
              activeMutationId = null,
              requests = mutableState.value.requests.filterNot { it.id == request.id },
              requestsTotalCount = (mutableState.value.requestsTotalCount - 1).coerceAtLeast(0),
            )
        is ClerkResult.Failure -> mutationFailed(result.errorMessage)
      }
    }
  }

  private fun mutationFailed(errorMessage: String) {
    mutableState.value =
      mutableState.value.copy(activeMutationId = null, errorMessage = errorMessage)
  }

  private fun defaultInviteRole(roles: List<Role>): String? {
    val defaultRole = Clerk.organizationDefaultRoleKey
    return roles.firstOrNull { it.key == defaultRole }?.key ?: roles.singleOrNull()?.key
  }
}

private const val DEFAULT_PAGE_SIZE = 20
private const val MEMBER_SEARCH_DEBOUNCE_MS = 300L
private const val REQUEST_PENDING_STATUS = "pending"
private const val INVITE_MUTATION_ID = "invite"
