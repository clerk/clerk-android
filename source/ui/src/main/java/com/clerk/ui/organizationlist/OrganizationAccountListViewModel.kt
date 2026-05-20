package com.clerk.ui.organizationlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clerk.api.Clerk
import com.clerk.api.log.ClerkLog
import com.clerk.api.network.ClerkPaginatedResponse
import com.clerk.api.network.model.error.ClerkErrorResponse
import com.clerk.api.network.serialization.ClerkResult
import com.clerk.api.network.serialization.errorMessage
import com.clerk.api.network.serialization.onFailure
import com.clerk.api.network.serialization.onSuccess
import com.clerk.api.organizations.OrganizationCreationDefaults
import com.clerk.api.organizations.OrganizationMembership
import com.clerk.api.organizations.OrganizationSuggestion
import com.clerk.api.organizations.UserOrganizationInvitation
import com.clerk.api.organizations.accept
import com.clerk.api.session.Session
import com.clerk.api.user.User
import com.clerk.api.user.getOrganizationCreationDefaults
import com.clerk.api.user.getOrganizationInvitations
import com.clerk.api.user.getOrganizationMemberships
import com.clerk.api.user.getOrganizationSuggestions
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Suppress("TooManyFunctions")
internal open class OrganizationAccountListViewModel(
  private val pageSize: Int = DEFAULT_PAGE_SIZE,
  private val workDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ViewModel() {

  protected val mutableState = MutableStateFlow(OrganizationAccountListState())
  val state = mutableState.asStateFlow()

  fun load() {
    if (mutableState.value.initialLoadAttempted) return

    val user = currentUser()
    if (user == null) {
      mutableState.value = missingUserState(mutableState.value)
      return
    }

    mutableState.value =
      mutableState.value.copy(
        isLoading = true,
        errorMessage = null,
        initialLoadAttempted = true,
        hasLoadedInitialResources = false,
        canCreateOrganization = user.createOrganizationEnabled == true,
      )

    viewModelScope.launch(workDispatcher) {
      val loadResult = loadInitialResources(user)
      withContext(Dispatchers.Main) {
        when (loadResult) {
          is InitialLoadResult.Success -> {
            mutableState.value =
              mutableState.value.copy(
                isLoading = false,
                memberships = loadResult.memberships.data,
                membershipsTotalCount = loadResult.memberships.totalCount,
                invitations = loadResult.invitations.data,
                invitationsTotalCount = loadResult.invitations.totalCount,
                suggestions = loadResult.suggestions.data,
                suggestionsTotalCount = loadResult.suggestions.totalCount,
                creationDefaults = loadResult.creationDefaults,
                hasLoadedInitialResources = true,
                errorMessage = null,
              )
          }
          is InitialLoadResult.Failure ->
            mutableState.value =
              mutableState.value.copy(isLoading = false, errorMessage = loadResult.message)
        }
      }
    }
  }

  fun retryLoad() {
    mutableState.value = mutableState.value.copy(initialLoadAttempted = false)
    load()
  }

  fun reset() {
    mutableState.value = OrganizationAccountListState()
  }

  fun loadMoreMemberships() {
    val user = currentUser() ?: return
    val current = mutableState.value
    if (!current.membershipsHasNextPage || current.isLoadingMoreMemberships) return

    mutableState.value = current.copy(isLoadingMoreMemberships = true, errorMessage = null)
    viewModelScope.launch(workDispatcher) {
      user
        .getOrganizationMemberships(limit = pageSize, offset = current.memberships.size)
        .onSuccess { response ->
          withContext(Dispatchers.Main) {
            val latest = mutableState.value
            mutableState.value =
              latest.copy(
                memberships = latest.memberships + response.data,
                membershipsTotalCount = response.totalCount,
                isLoadingMoreMemberships = false,
              )
          }
        }
        .onFailure { failure ->
          withContext(Dispatchers.Main) {
            mutableState.value =
              mutableState.value.copy(
                isLoadingMoreMemberships = false,
                errorMessage = failure.errorMessage,
              )
          }
        }
    }
  }

  fun loadMoreInvitations() {
    val user = currentUser() ?: return
    val current = mutableState.value
    if (!current.invitationsHasNextPage || current.isLoadingMoreInvitations) return

    mutableState.value = current.copy(isLoadingMoreInvitations = true, errorMessage = null)
    viewModelScope.launch(workDispatcher) {
      user
        .getOrganizationInvitations(
          limit = pageSize,
          offset = current.pendingInvitationsCount,
          status = PENDING_STATUS,
        )
        .onSuccess { response ->
          withContext(Dispatchers.Main) {
            val latest = mutableState.value
            mutableState.value =
              latest.copy(
                invitations = latest.invitations + response.data,
                invitationsTotalCount = response.totalCount,
                isLoadingMoreInvitations = false,
              )
          }
        }
        .onFailure { failure ->
          withContext(Dispatchers.Main) {
            mutableState.value =
              mutableState.value.copy(
                isLoadingMoreInvitations = false,
                errorMessage = failure.errorMessage,
              )
          }
        }
    }
  }

  fun loadMoreSuggestions() {
    val user = currentUser() ?: return
    val current = mutableState.value
    if (!current.suggestionsHasNextPage || current.isLoadingMoreSuggestions) return

    mutableState.value = current.copy(isLoadingMoreSuggestions = true, errorMessage = null)
    viewModelScope.launch(workDispatcher) {
      user
        .getOrganizationSuggestions(
          limit = pageSize,
          offset = current.suggestions.size,
          statuses = SUGGESTION_STATUSES,
        )
        .onSuccess { response ->
          withContext(Dispatchers.Main) {
            val latest = mutableState.value
            mutableState.value =
              latest.copy(
                suggestions = latest.suggestions + response.data,
                suggestionsTotalCount = response.totalCount,
                isLoadingMoreSuggestions = false,
              )
          }
        }
        .onFailure { failure ->
          withContext(Dispatchers.Main) {
            mutableState.value =
              mutableState.value.copy(
                isLoadingMoreSuggestions = false,
                errorMessage = failure.errorMessage,
              )
          }
        }
    }
  }

  fun selectPersonalAccount(onSelected: (Session) -> Unit = {}) {
    val session = currentSession()
    if (session == null) {
      mutableState.value = mutableState.value.copy(errorMessage = SESSION_MISSING_MESSAGE)
      return
    }
    if (mutableState.value.activeActionId != null) return

    mutableState.value =
      mutableState.value.copy(activeActionId = PERSONAL_ACCOUNT_ACTION_ID, errorMessage = null)
    viewModelScope.launch(workDispatcher) {
      Clerk.auth
        .setActive(sessionId = session.id)
        .onSuccess { selectedSession ->
          withContext(Dispatchers.Main) {
            mutableState.value =
              mutableState.value.copy(activeActionId = null, completedSession = selectedSession)
            onSelected(selectedSession)
          }
        }
        .onFailure { failure ->
          withContext(Dispatchers.Main) {
            mutableState.value =
              mutableState.value.copy(activeActionId = null, errorMessage = failure.errorMessage)
          }
        }
    }
  }

  fun selectOrganization(organizationId: String, onSelected: (Session) -> Unit = {}) {
    val session = currentSession()
    if (session == null) {
      mutableState.value = mutableState.value.copy(errorMessage = SESSION_MISSING_MESSAGE)
      return
    }
    if (mutableState.value.activeActionId != null) return

    mutableState.value =
      mutableState.value.copy(activeActionId = organizationId, errorMessage = null)
    viewModelScope.launch(workDispatcher) {
      Clerk.auth
        .setActive(sessionId = session.id, organizationId = organizationId)
        .onSuccess { selectedSession ->
          withContext(Dispatchers.Main) {
            mutableState.value =
              mutableState.value.copy(activeActionId = null, completedSession = selectedSession)
            onSelected(selectedSession)
          }
        }
        .onFailure { failure ->
          withContext(Dispatchers.Main) {
            mutableState.value =
              mutableState.value.copy(
                activeActionId = null,
                errorMessage = organizationSelectionErrorMessage(failure),
              )
          }
        }
    }
  }

  fun acceptInvitation(invitation: UserOrganizationInvitation) {
    if (mutableState.value.activeActionId != null) return
    mutableState.value =
      mutableState.value.copy(activeActionId = invitation.id, errorMessage = null)

    viewModelScope.launch(workDispatcher) {
      invitation
        .accept()
        .onSuccess { accepted ->
          withContext(Dispatchers.Main) {
            val latest = mutableState.value
            val hadInvitation = latest.invitations.any { it.id == invitation.id }
            mutableState.value =
              latest.copy(
                activeActionId = null,
                invitations =
                  latest.invitations.map { if (it.id == invitation.id) accepted else it },
                invitationsTotalCount =
                  if (hadInvitation) maxOf(0, latest.invitationsTotalCount - 1)
                  else latest.invitationsTotalCount,
                acceptedInvitationOrganizationIds =
                  latest.acceptedInvitationOrganizationIds + accepted.publicOrganizationData.id,
              )
          }
        }
        .onFailure { failure ->
          withContext(Dispatchers.Main) {
            mutableState.value =
              mutableState.value.copy(activeActionId = null, errorMessage = failure.errorMessage)
          }
        }
    }
  }

  fun acceptSuggestion(suggestion: OrganizationSuggestion) {
    if (mutableState.value.activeActionId != null) return
    mutableState.value =
      mutableState.value.copy(activeActionId = suggestion.id, errorMessage = null)

    viewModelScope.launch(workDispatcher) {
      suggestion
        .accept()
        .onSuccess { accepted ->
          withContext(Dispatchers.Main) {
            val latest = mutableState.value
            mutableState.value =
              latest.copy(
                activeActionId = null,
                suggestions =
                  latest.suggestions.map { if (it.id == suggestion.id) accepted else it },
              )
          }
        }
        .onFailure { failure ->
          withContext(Dispatchers.Main) {
            mutableState.value =
              mutableState.value.copy(activeActionId = null, errorMessage = failure.errorMessage)
          }
        }
    }
  }

  fun clearCompletedSession() {
    mutableState.value = mutableState.value.copy(completedSession = null)
  }

  fun clearError() {
    mutableState.value = mutableState.value.copy(errorMessage = null)
  }

  protected open fun currentUser(): User? = Clerk.user

  protected open fun currentSession(): Session? = Clerk.session

  protected open fun missingUserState(
    current: OrganizationAccountListState
  ): OrganizationAccountListState {
    return current.copy(isLoading = false, errorMessage = null, initialLoadAttempted = true)
  }

  protected open fun shouldFetchCreationDefaults(user: User): Boolean {
    return user.createOrganizationEnabled == true && Clerk.organizationCreationDefaultsIsEnabled
  }

  protected open fun organizationSelectionErrorMessage(
    failure: ClerkResult.Failure<ClerkErrorResponse>
  ): String {
    val code = failure.error?.errors?.firstOrNull()?.code
    if (code in ORGANIZATION_MEMBERSHIP_ERROR_CODES) {
      return "You are no longer a member of this organization. Please choose another one."
    }
    return failure.errorMessage
  }

  private suspend fun loadInitialResources(user: User): InitialLoadResult = coroutineScope {
    val memberships = async { user.getOrganizationMemberships(limit = pageSize, offset = 0) }
    val invitations = async {
      user.getOrganizationInvitations(limit = pageSize, offset = 0, status = PENDING_STATUS)
    }
    val suggestions = async {
      user.getOrganizationSuggestions(limit = pageSize, offset = 0, statuses = SUGGESTION_STATUSES)
    }
    val creationDefaults = async {
      if (shouldFetchCreationDefaults(user)) user.getOrganizationCreationDefaults() else null
    }

    val membershipsResult = memberships.await()
    val invitationsResult = invitations.await()
    val suggestionsResult = suggestions.await()
    val creationDefaultsResult = creationDefaults.await()

    val failure =
      listOf(membershipsResult, invitationsResult, suggestionsResult)
        .filterIsInstance<ClerkResult.Failure<ClerkErrorResponse>>()
        .firstOrNull()

    if (failure != null) {
      InitialLoadResult.Failure(failure.errorMessage)
    } else {
      InitialLoadResult.Success(
        memberships =
          (membershipsResult as ClerkResult.Success<ClerkPaginatedResponse<OrganizationMembership>>)
            .value,
        invitations =
          (invitationsResult
              as ClerkResult.Success<ClerkPaginatedResponse<UserOrganizationInvitation>>)
            .value,
        suggestions =
          (suggestionsResult as ClerkResult.Success<ClerkPaginatedResponse<OrganizationSuggestion>>)
            .value,
        creationDefaults = creationDefaultsResult?.creationDefaultsOrNull(),
      )
    }
  }

  private fun ClerkResult<OrganizationCreationDefaults, ClerkErrorResponse>
    .creationDefaultsOrNull(): OrganizationCreationDefaults? {
    return when (this) {
      is ClerkResult.Success -> value
      is ClerkResult.Failure -> {
        ClerkLog.e("Failed to fetch organization creation defaults: $errorMessage")
        null
      }
    }
  }

  private sealed interface InitialLoadResult {
    data class Success(
      val memberships: ClerkPaginatedResponse<OrganizationMembership>,
      val invitations: ClerkPaginatedResponse<UserOrganizationInvitation>,
      val suggestions: ClerkPaginatedResponse<OrganizationSuggestion>,
      val creationDefaults: OrganizationCreationDefaults?,
    ) : InitialLoadResult

    data class Failure(val message: String) : InitialLoadResult
  }

  private companion object {
    const val DEFAULT_PAGE_SIZE = 10
    const val PENDING_STATUS = "pending"
    const val SESSION_MISSING_MESSAGE = "Session does not exist"
    val SUGGESTION_STATUSES = listOf("pending", "accepted")
    val ORGANIZATION_MEMBERSHIP_ERROR_CODES =
      setOf("organization_not_found_or_unauthorized", "not_a_member_in_organization")
  }
}
