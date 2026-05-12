package com.clerk.ui.organizationswitcher

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clerk.api.Clerk
import com.clerk.api.network.serialization.errorMessage
import com.clerk.api.network.serialization.onFailure
import com.clerk.api.network.serialization.onSuccess
import com.clerk.api.organizations.OrganizationMembership
import com.clerk.api.session.Session
import com.clerk.api.user.User
import com.clerk.api.user.getOrganizationMemberships
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal class OrganizationSwitcherViewModel : ViewModel() {

  private val _state = MutableStateFlow(OrganizationSwitcherState())
  val state = _state.asStateFlow()

  fun load(user: User?) {
    if (user == null) {
      _state.value = OrganizationSwitcherState()
      return
    }
    if (_state.value.initialLoadAttempted) return

    _state.value =
      _state.value.copy(isLoading = true, initialLoadAttempted = true, errorMessage = null)
    viewModelScope.launch(Dispatchers.IO) {
      user
        .getOrganizationMemberships(limit = PAGE_SIZE, offset = 0)
        .onSuccess { response ->
          withContext(Dispatchers.Main) {
            _state.value =
              _state.value.copy(
                isLoading = false,
                memberships = response.data,
                membershipsTotalCount = response.totalCount,
                errorMessage = null,
              )
          }
        }
        .onFailure { failure ->
          withContext(Dispatchers.Main) {
            _state.value = _state.value.copy(isLoading = false, errorMessage = failure.errorMessage)
          }
        }
    }
  }

  fun reset() {
    _state.value = OrganizationSwitcherState()
  }

  fun loadMore(user: User?) {
    val current = _state.value
    if (user == null || !current.hasNextPage || current.isLoadingMore) return

    _state.value = current.copy(isLoadingMore = true, errorMessage = null)
    viewModelScope.launch(Dispatchers.IO) {
      user
        .getOrganizationMemberships(limit = PAGE_SIZE, offset = current.memberships.size)
        .onSuccess { response ->
          withContext(Dispatchers.Main) {
            val latest = _state.value
            _state.value =
              latest.copy(
                isLoadingMore = false,
                memberships = latest.memberships + response.data,
                membershipsTotalCount = response.totalCount,
              )
          }
        }
        .onFailure { failure ->
          withContext(Dispatchers.Main) {
            _state.value =
              _state.value.copy(isLoadingMore = false, errorMessage = failure.errorMessage)
          }
        }
    }
  }

  fun selectOrganization(session: Session?, organizationId: String, onSelected: () -> Unit) {
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
            _state.value = _state.value.copy(activeActionId = null)
            onSelected()
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

  fun clearError() {
    _state.value = _state.value.copy(errorMessage = null)
  }

  private companion object {
    const val PAGE_SIZE = 20
  }
}

internal data class OrganizationSwitcherState(
  val isLoading: Boolean = false,
  val initialLoadAttempted: Boolean = false,
  val memberships: List<OrganizationMembership> = emptyList(),
  val membershipsTotalCount: Int = 0,
  val isLoadingMore: Boolean = false,
  val activeActionId: String? = null,
  val errorMessage: String? = null,
) {
  val hasNextPage: Boolean
    get() = memberships.size < membershipsTotalCount
}
