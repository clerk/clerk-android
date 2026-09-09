package com.clerk.ui.organizationprofile.invite

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clerk.api.*
import com.clerk.ui.core.common.displayMessage
import com.clerk.ui.core.common.runUiOperation
import com.clerk.ui.organizationprofile.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

internal class OrganizationInviteMembersViewModel(private val clerk: Clerk) : ViewModel() {

  private val _state = MutableStateFlow(OrganizationInviteMembersState())
  val state = _state.asStateFlow()

  fun loadRoles(organization: Organization) {
    _state.value = _state.value.copy(isLoadingRoles = true, errorMessage = null)
    viewModelScope.launch {
      runUiOperation { organization.getRoles().data }
        .onSuccess { roles ->
          _state.value =
            _state.value.copy(
              isLoadingRoles = false,
              roles = roles,
              selectedRoleKey = defaultSelectedRoleKey(roles),
            )
        }
        .onFailure {
          _state.value =
            _state.value.copy(
              isLoadingRoles = false,
              roles = emptyList(),
              selectedRoleKey = null,
              errorMessage = "Failed to load organization roles: ${it.displayMessage}",
            )
        }
    }
  }

  fun selectRole(roleKey: String) {
    _state.value = _state.value.copy(selectedRoleKey = roleKey, errorMessage = null)
  }

  fun sendInvitations(organization: Organization, emailAddresses: List<String>) {
    val roleKey = _state.value.selectedRoleKey
    if (roleKey == null || emailAddresses.isEmpty() || _state.value.isSubmitting) return

    _state.value = _state.value.copy(isSubmitting = true, errorMessage = null)
    viewModelScope.launch {
      runUiOperation { organization.inviteMembers(InviteMembersParams(emailAddresses, roleKey)) }
        .onSuccess {
          _state.value =
            _state.value.copy(isSubmitting = false, completion = OrganizationInviteCompletion.Sent)
        }
        .onFailure {
          _state.value =
            _state.value.copy(
              isSubmitting = false,
              errorMessage = "Failed to send invitations: ${it.displayMessage}",
            )
        }
    }
  }

  fun clearCompletion() {
    _state.value = _state.value.copy(completion = null)
  }

  fun clearError() {
    _state.value = _state.value.copy(errorMessage = null)
  }

  private fun defaultSelectedRoleKey(roles: List<Role>): String? {
    val defaultRole = clerk.environment.organizationSettings.domains.defaultRole
    return roles.firstOrNull { it.key == defaultRole }?.key ?: roles.singleOrNull()?.key
  }
}

internal data class OrganizationInviteMembersState(
  val isLoadingRoles: Boolean = false,
  val roles: List<Role> = emptyList(),
  val selectedRoleKey: String? = null,
  val isSubmitting: Boolean = false,
  val errorMessage: String? = null,
  val completion: OrganizationInviteCompletion? = null,
)

internal enum class OrganizationInviteCompletion {
  Sent
}
