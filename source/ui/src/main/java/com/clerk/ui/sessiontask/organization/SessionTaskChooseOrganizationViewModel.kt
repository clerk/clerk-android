package com.clerk.ui.sessiontask.organization

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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Suppress("TooManyFunctions")
internal class SessionTaskChooseOrganizationViewModel : ViewModel() {

  private val _state = MutableStateFlow(SessionTaskChooseOrganizationState())
  val state = _state.asStateFlow()

  fun load() {
    if (_state.value.initialLoadAttempted) return

    val user = Clerk.user
    if (user == null) {
      _state.value =
        _state.value.copy(
          isLoading = false,
          initialLoadAttempted = true,
          hasLoadedInitialResources = true,
          canCreateOrganization = false,
        )
      return
    }

    _state.value =
      _state.value.copy(
        isLoading = true,
        errorMessage = null,
        initialLoadAttempted = true,
        hasLoadedInitialResources = false,
        canCreateOrganization = user.createOrganizationEnabled == true,
      )

    viewModelScope.launch(Dispatchers.IO) {
      val loadResult = loadInitialResources(user)
      withContext(Dispatchers.Main) {
        when (loadResult) {
          is InitialLoadResult.Success -> {
            _state.value =
              _state.value.copy(
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
            _state.value = _state.value.copy(isLoading = false, errorMessage = loadResult.message)
        }
      }
    }
  }

  fun retryLoad() {
    _state.value = _state.value.copy(initialLoadAttempted = false)
    load()
  }

  fun loadMoreMemberships() {
    val user = Clerk.user ?: return
    val current = _state.value
    if (!current.membershipsHasNextPage || current.isLoadingMoreMemberships) return

    _state.value = current.copy(isLoadingMoreMemberships = true, errorMessage = null)
    viewModelScope.launch(Dispatchers.IO) {
      user
        .getOrganizationMemberships(limit = PAGE_SIZE, offset = current.memberships.size)
        .onSuccess {
          withContext(Dispatchers.Main) {
            val latest = _state.value
            _state.value =
              latest.copy(
                memberships = latest.memberships + it.data,
                membershipsTotalCount = it.totalCount,
                isLoadingMoreMemberships = false,
              )
          }
        }
        .onFailure { failure ->
          withContext(Dispatchers.Main) {
            _state.value =
              _state.value.copy(
                isLoadingMoreMemberships = false,
                errorMessage = failure.errorMessage,
              )
          }
        }
    }
  }

  fun loadMoreInvitations() {
    val user = Clerk.user ?: return
    val current = _state.value
    if (!current.invitationsHasNextPage || current.isLoadingMoreInvitations) return

    _state.value = current.copy(isLoadingMoreInvitations = true, errorMessage = null)
    viewModelScope.launch(Dispatchers.IO) {
      user
        .getOrganizationInvitations(
          limit = PAGE_SIZE,
          offset = current.invitations.size,
          status = PENDING_STATUS,
        )
        .onSuccess {
          withContext(Dispatchers.Main) {
            val latest = _state.value
            _state.value =
              latest.copy(
                invitations = latest.invitations + it.data,
                invitationsTotalCount = it.totalCount,
                isLoadingMoreInvitations = false,
              )
          }
        }
        .onFailure { failure ->
          withContext(Dispatchers.Main) {
            _state.value =
              _state.value.copy(
                isLoadingMoreInvitations = false,
                errorMessage = failure.errorMessage,
              )
          }
        }
    }
  }

  fun loadMoreSuggestions() {
    val user = Clerk.user ?: return
    val current = _state.value
    if (!current.suggestionsHasNextPage || current.isLoadingMoreSuggestions) return

    _state.value = current.copy(isLoadingMoreSuggestions = true, errorMessage = null)
    viewModelScope.launch(Dispatchers.IO) {
      user
        .getOrganizationSuggestions(
          limit = PAGE_SIZE,
          offset = current.suggestions.size,
          statuses = SUGGESTION_STATUSES,
        )
        .onSuccess {
          withContext(Dispatchers.Main) {
            val latest = _state.value
            _state.value =
              latest.copy(
                suggestions = latest.suggestions + it.data,
                suggestionsTotalCount = it.totalCount,
                isLoadingMoreSuggestions = false,
              )
          }
        }
        .onFailure { failure ->
          withContext(Dispatchers.Main) {
            _state.value =
              _state.value.copy(
                isLoadingMoreSuggestions = false,
                errorMessage = failure.errorMessage,
              )
          }
        }
    }
  }

  fun selectOrganization(organizationId: String) {
    val session = Clerk.session
    if (session == null) {
      _state.value = _state.value.copy(errorMessage = "Session does not exist")
      return
    }
    if (_state.value.activeActionId != null) return

    _state.value = _state.value.copy(activeActionId = organizationId, errorMessage = null)
    viewModelScope.launch(Dispatchers.IO) {
      Clerk.auth
        .setActive(sessionId = session.id, organizationId = organizationId)
        .onSuccess {
          withContext(Dispatchers.Main) {
            _state.value = _state.value.copy(activeActionId = null, completedSession = it)
          }
        }
        .onFailure { failure ->
          withContext(Dispatchers.Main) {
            _state.value =
              _state.value.copy(
                activeActionId = null,
                errorMessage = organizationSelectionErrorMessage(failure),
              )
          }
        }
    }
  }

  fun acceptInvitation(invitation: UserOrganizationInvitation) {
    if (_state.value.activeActionId != null) return
    _state.value = _state.value.copy(activeActionId = invitation.id, errorMessage = null)

    viewModelScope.launch(Dispatchers.IO) {
      invitation
        .accept()
        .onSuccess {
          withContext(Dispatchers.Main) {
            val latest = _state.value
            _state.value =
              latest.copy(
                activeActionId = null,
                acceptedInvitationOrganizationIds =
                  latest.acceptedInvitationOrganizationIds + invitation.publicOrganizationData.id,
              )
          }
        }
        .onFailure { failure ->
          withContext(Dispatchers.Main) {
            _state.value =
              _state.value.copy(activeActionId = null, errorMessage = failure.errorMessage)
          }
        }
    }
  }

  fun acceptSuggestion(suggestion: OrganizationSuggestion) {
    if (_state.value.activeActionId != null) return
    _state.value = _state.value.copy(activeActionId = suggestion.id, errorMessage = null)

    viewModelScope.launch(Dispatchers.IO) {
      suggestion
        .accept()
        .onSuccess { accepted ->
          withContext(Dispatchers.Main) {
            val latest = _state.value
            _state.value =
              latest.copy(
                activeActionId = null,
                suggestions =
                  latest.suggestions.map { if (it.id == suggestion.id) accepted else it },
              )
          }
        }
        .onFailure { failure ->
          withContext(Dispatchers.Main) {
            _state.value =
              _state.value.copy(activeActionId = null, errorMessage = failure.errorMessage)
          }
        }
    }
  }

  fun clearCompletedSession() {
    _state.value = _state.value.copy(completedSession = null)
  }

  fun clearError() {
    _state.value = _state.value.copy(errorMessage = null)
  }

  private suspend fun loadInitialResources(user: User): InitialLoadResult = coroutineScope {
    val memberships = async { user.getOrganizationMemberships(limit = PAGE_SIZE, offset = 0) }
    val invitations = async {
      user.getOrganizationInvitations(limit = PAGE_SIZE, offset = 0, status = PENDING_STATUS)
    }
    val suggestions = async {
      user.getOrganizationSuggestions(limit = PAGE_SIZE, offset = 0, statuses = SUGGESTION_STATUSES)
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

  private fun shouldFetchCreationDefaults(user: User): Boolean {
    return user.createOrganizationEnabled == true && Clerk.organizationCreationDefaultsIsEnabled
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

  private fun organizationSelectionErrorMessage(
    failure: ClerkResult.Failure<ClerkErrorResponse>
  ): String {
    val code = failure.error?.errors?.firstOrNull()?.code
    if (code in ORGANIZATION_MEMBERSHIP_ERROR_CODES) {
      return if (_state.value.canCreateOrganization) {
        "You are no longer a member of this organization. Please choose or create another one."
      } else {
        "You are no longer a member of this organization. Please choose another one."
      }
    }
    return failure.errorMessage
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
    const val PAGE_SIZE = 10
    const val PENDING_STATUS = "pending"
    val SUGGESTION_STATUSES = listOf("pending", "accepted")
    val ORGANIZATION_MEMBERSHIP_ERROR_CODES =
      setOf("organization_not_found_or_unauthorized", "not_a_member_in_organization")
  }
}

internal data class SessionTaskChooseOrganizationState(
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

  val membershipsHasNextPage: Boolean
    get() = memberships.size < membershipsTotalCount

  val invitationsHasNextPage: Boolean
    get() = invitations.size < invitationsTotalCount

  val suggestionsHasNextPage: Boolean
    get() = suggestions.size < suggestionsTotalCount

  val hasNextPage: Boolean
    get() = membershipsHasNextPage || invitationsHasNextPage || suggestionsHasNextPage
}
