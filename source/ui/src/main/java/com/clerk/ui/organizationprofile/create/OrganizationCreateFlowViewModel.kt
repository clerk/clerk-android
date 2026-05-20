package com.clerk.ui.organizationprofile.create

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clerk.api.Clerk
import com.clerk.api.network.serialization.errorMessage
import com.clerk.api.network.serialization.onFailure
import com.clerk.api.network.serialization.onSuccess
import com.clerk.api.organizations.Organization
import com.clerk.api.organizations.updateLogo
import com.clerk.api.session.Session
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal class OrganizationCreateFlowViewModel : ViewModel() {

  private val _state = MutableStateFlow(OrganizationCreateFlowState())
  val state = _state.asStateFlow()

  fun createOrganization(name: String, slug: String?, logoFile: File?) {
    val sessionId = Clerk.session?.id
    if (sessionId == null) {
      _state.value = _state.value.copy(errorMessage = "Session does not exist")
      return
    }
    if (_state.value.isLoading) return

    _state.value = _state.value.copy(isLoading = true, errorMessage = null)
    viewModelScope.launch(Dispatchers.IO) {
      Organization.create(name = name, slug = slug)
        .onSuccess { organization ->
          val organizationWithLogo = uploadLogoIfNeeded(organization, logoFile)
          setActiveOrganization(sessionId = sessionId, organization = organizationWithLogo)
        }
        .onFailure { failure ->
          withContext(Dispatchers.Main) {
            _state.value = _state.value.copy(isLoading = false, errorMessage = failure.errorMessage)
          }
        }
    }
  }

  fun clearCompletedCreate() {
    _state.value = _state.value.copy(createdOrganization = null, completedSession = null)
  }

  fun clearError() {
    _state.value = _state.value.copy(errorMessage = null)
  }

  private suspend fun uploadLogoIfNeeded(
    organization: Organization,
    logoFile: File?,
  ): Organization {
    if (logoFile == null) return organization

    return when (val result = organization.updateLogo(logoFile)) {
      is com.clerk.api.network.serialization.ClerkResult.Success -> result.value
      is com.clerk.api.network.serialization.ClerkResult.Failure -> organization
    }
  }

  private suspend fun setActiveOrganization(sessionId: String, organization: Organization) {
    Clerk.auth
      .setActive(sessionId = sessionId, organizationId = organization.id)
      .onSuccess {
        withContext(Dispatchers.Main) {
          _state.value =
            _state.value.copy(
              isLoading = false,
              createdOrganization = organization,
              completedSession = it,
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

internal data class OrganizationCreateFlowState(
  val isLoading: Boolean = false,
  val errorMessage: String? = null,
  val createdOrganization: Organization? = null,
  val completedSession: Session? = null,
)
