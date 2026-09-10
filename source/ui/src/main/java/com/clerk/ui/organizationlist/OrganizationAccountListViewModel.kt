package com.clerk.ui.organizationlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clerk.api.*
import com.clerk.ui.core.common.displayMessage
import com.clerk.ui.core.common.runUiOperation
import com.clerk.ui.organizationprofile.*
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
  protected val clerk: Clerk,
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
                membershipsTotalCount = loadResult.memberships.totalCount.toInt(),
                invitations = loadResult.invitations.data,
                invitationsTotalCount = loadResult.invitations.totalCount.toInt(),
                suggestions = loadResult.suggestions.data,
                suggestionsTotalCount = loadResult.suggestions.totalCount.toInt(),
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
      runUiOperation {
          user.getOrganizationMemberships(
            GetUserOrganizationMembershipParams(
              pageSize = pageSize.toDouble(),
              initialPage = (current.memberships.size / pageSize + 1).toDouble(),
            )
          )
        }
        .onSuccess { response ->
          withContext(Dispatchers.Main) {
            val latest = mutableState.value
            mutableState.value =
              latest.copy(
                memberships = (latest.memberships + response.data).distinctBy { it.id },
                membershipsTotalCount = response.totalCount.toInt(),
                isLoadingMoreMemberships = false,
              )
          }
        }
        .onFailure { failure ->
          withContext(Dispatchers.Main) {
            mutableState.value =
              mutableState.value.copy(
                isLoadingMoreMemberships = false,
                errorMessage = failure.displayMessage,
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
      runUiOperation {
          user.getOrganizationInvitations(
            GetUserOrganizationInvitationsParams(
              pageSize = pageSize.toDouble(),
              initialPage = (current.pendingInvitationsCount / pageSize + 1).toDouble(),
              status = OrganizationInvitationStatus.Pending,
            )
          )
        }
        .onSuccess { response ->
          withContext(Dispatchers.Main) {
            val latest = mutableState.value
            mutableState.value =
              latest.copy(
                invitations = (latest.invitations + response.data).distinctBy { it.id },
                invitationsTotalCount = response.totalCount.toInt(),
                isLoadingMoreInvitations = false,
              )
          }
        }
        .onFailure { failure ->
          withContext(Dispatchers.Main) {
            mutableState.value =
              mutableState.value.copy(
                isLoadingMoreInvitations = false,
                errorMessage = failure.displayMessage,
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
      runUiOperation {
          user.getOrganizationSuggestions(
            GetUserOrganizationSuggestionsParams(
              pageSize = pageSize.toDouble(),
              initialPage = (current.suggestions.size / pageSize + 1).toDouble(),
              status = SUGGESTION_STATUSES,
            )
          )
        }
        .onSuccess { response ->
          withContext(Dispatchers.Main) {
            val latest = mutableState.value
            mutableState.value =
              latest.copy(
                suggestions = (latest.suggestions + response.data).distinctBy { it.id },
                suggestionsTotalCount = response.totalCount.toInt(),
                isLoadingMoreSuggestions = false,
              )
          }
        }
        .onFailure { failure ->
          withContext(Dispatchers.Main) {
            mutableState.value =
              mutableState.value.copy(
                isLoadingMoreSuggestions = false,
                errorMessage = failure.displayMessage,
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
      runUiOperation {
          clerk.setActive(
            MobileSetActiveParams(
              session = Field.Value(MobileSetActiveParamsSession.Case1(session.id)),
              organization = Field.Null,
            )
          )
          clerk.session?.takeIf { it.id == session.id } ?: throw CoreException("session_changed")
        }
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
              mutableState.value.copy(activeActionId = null, errorMessage = failure.displayMessage)
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
      runUiOperation {
          clerk.setActive(
            MobileSetActiveParams(
              session = Field.Value(MobileSetActiveParamsSession.Case1(session.id)),
              organization = Field.Value(MobileSetActiveParamsOrganization.Case1(organizationId)),
            )
          )
          clerk.session?.takeIf { it.id == session.id } ?: throw CoreException("session_changed")
        }
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
      runUiOperation { invitation.accept() }
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
              mutableState.value.copy(activeActionId = null, errorMessage = failure.displayMessage)
          }
        }
    }
  }

  fun acceptSuggestion(suggestion: OrganizationSuggestion) {
    if (mutableState.value.activeActionId != null) return
    mutableState.value =
      mutableState.value.copy(activeActionId = suggestion.id, errorMessage = null)

    viewModelScope.launch(workDispatcher) {
      runUiOperation { suggestion.accept() }
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
              mutableState.value.copy(activeActionId = null, errorMessage = failure.displayMessage)
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

  protected open fun currentUser(): User? = clerk.user

  protected open fun currentSession(): Session? = clerk.session

  protected open fun missingUserState(
    current: OrganizationAccountListState
  ): OrganizationAccountListState {
    return current.copy(isLoading = false, errorMessage = null, initialLoadAttempted = true)
  }

  protected open fun shouldFetchCreationDefaults(user: User): Boolean {
    return user.createOrganizationEnabled == true &&
      clerk.environment.organizationSettings.organizationCreationDefaults.enabled
  }

  protected open fun organizationSelectionErrorMessage(failure: Throwable): String {
    val code = (failure as? CoreException)?.errors?.firstOrNull()?.code
    if (code in ORGANIZATION_MEMBERSHIP_ERROR_CODES) {
      return "You are no longer a member of this organization. Please choose another one."
    }
    return failure.displayMessage
  }

  private suspend fun loadInitialResources(user: User): InitialLoadResult {
    return runUiOperation {
        coroutineScope {
          val memberships = async {
            user.getOrganizationMemberships(
              GetUserOrganizationMembershipParams(pageSize = pageSize.toDouble(), initialPage = 1.0)
            )
          }
          val invitations = async {
            user.getOrganizationInvitations(
              GetUserOrganizationInvitationsParams(
                pageSize = pageSize.toDouble(),
                initialPage = 1.0,
                status = OrganizationInvitationStatus.Pending,
              )
            )
          }
          val suggestions = async {
            user.getOrganizationSuggestions(
              GetUserOrganizationSuggestionsParams(
                pageSize = pageSize.toDouble(),
                initialPage = 1.0,
                status = SUGGESTION_STATUSES,
              )
            )
          }
          val defaults = async {
            if (shouldFetchCreationDefaults(user))
              runUiOperation { user.getOrganizationCreationDefaults() }.getOrNull()
            else null
          }
          InitialLoadResult.Success(
            memberships.await(),
            invitations.await(),
            suggestions.await(),
            defaults.await(),
          )
        }
      }
      .getOrElse { InitialLoadResult.Failure(it.displayMessage) }
  }

  private sealed interface InitialLoadResult {
    data class Success(
      val memberships: ClerkPaginatedResponseOrganizationMembership,
      val invitations: ClerkPaginatedResponseUserOrganizationInvitation,
      val suggestions: ClerkPaginatedResponseOrganizationSuggestion,
      val creationDefaults: OrganizationCreationDefaults?,
    ) : InitialLoadResult

    data class Failure(val message: String) : InitialLoadResult
  }

  private companion object {
    const val DEFAULT_PAGE_SIZE = 10
    const val PENDING_STATUS = "pending"
    const val SESSION_MISSING_MESSAGE = "Session does not exist"
    val SUGGESTION_STATUSES =
      GetUserOrganizationSuggestionsParamsStatus.Case3(
        listOf(OrganizationSuggestionStatus.Pending, OrganizationSuggestionStatus.Accepted)
      )
    val ORGANIZATION_MEMBERSHIP_ERROR_CODES =
      setOf("organization_not_found_or_unauthorized", "not_a_member_in_organization")
  }
}
