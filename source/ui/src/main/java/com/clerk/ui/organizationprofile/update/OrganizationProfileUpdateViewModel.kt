package com.clerk.ui.organizationprofile.update

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clerk.api.*
import com.clerk.ui.core.common.displayMessage
import com.clerk.ui.core.common.runUiOperation
import com.clerk.ui.organizationprofile.*
import java.io.File
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

internal class OrganizationProfileUpdateViewModel(private val clerk: Clerk) : ViewModel() {

  private val _state = MutableStateFlow<State>(State.Idle)
  val state = _state.asStateFlow()

  fun save(
    organization: Organization,
    name: String,
    slug: String?,
    logoFile: File?,
    removeLogo: Boolean,
  ) {
    if (_state.value is State.Loading) return

    _state.value = State.Loading
    viewModelScope.launch {
      runUiOperation {
          organization.update(UpdateOrganizationParams(name, slug))
          when {
            logoFile != null -> organization.setLogo(logoFile.organizationLogoInput())
            removeLogo -> organization.setLogo(SetOrganizationLogoParams(null))
          }
          organization.reload()
        }
        .onSuccess { _state.value = State.Success(organization) }
        .onFailure {
          _state.value = State.Error("Failed to update organization: ${it.displayMessage}")
        }
    }
  }

  fun reset() {
    _state.value = State.Idle
  }

  fun clearError() {
    if (_state.value is State.Error) _state.value = State.Idle
  }

  sealed interface State {
    data object Idle : State

    data object Loading : State

    data class Success(val organization: Organization) : State

    data class Error(val message: String) : State
  }
}
