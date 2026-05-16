package com.clerk.ui.organizationprofile.update

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clerk.api.network.serialization.errorMessage
import com.clerk.api.network.serialization.flatMap
import com.clerk.api.network.serialization.onFailure
import com.clerk.api.network.serialization.onSuccess
import com.clerk.api.organizations.Organization
import com.clerk.api.organizations.deleteLogo
import com.clerk.api.organizations.reload
import com.clerk.api.organizations.update
import com.clerk.api.organizations.updateLogo
import java.io.File
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

internal class OrganizationProfileUpdateViewModel : ViewModel() {

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
      organization
        .update(name = name, slug = slug)
        .flatMap { updated ->
          applyLogoChange(updated, logoFile = logoFile, removeLogo = removeLogo)
        }
        .flatMap { updated -> updated.reload() }
        .onSuccess { _state.value = State.Success(it) }
        .onFailure {
          _state.value = State.Error("Failed to update organization: ${it.errorMessage}")
        }
    }
  }

  fun reset() {
    _state.value = State.Idle
  }

  private suspend fun applyLogoChange(
    organization: Organization,
    logoFile: File?,
    removeLogo: Boolean,
  ) =
    when {
      logoFile != null -> organization.updateLogo(logoFile)
      removeLogo -> organization.deleteLogo()
      else -> com.clerk.api.network.serialization.ClerkResult.success(organization)
    }

  sealed interface State {
    data object Idle : State

    data object Loading : State

    data class Success(val organization: Organization) : State

    data class Error(val message: String) : State
  }
}
