package com.clerk.ui.organizationprofile.create

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

internal class OrganizationCreateFlowViewModel(private val clerk: Clerk) : ViewModel() {

  private val _state = MutableStateFlow(OrganizationCreateFlowState())
  val state = _state.asStateFlow()

  fun createOrganization(name: String, slug: String?, logoFile: File?) {
    val sessionId = clerk.session?.id
    if (sessionId == null) {
      _state.value = _state.value.copy(errorMessage = "Session does not exist")
      return
    }
    if (_state.value.isLoading) return
    _state.value = _state.value.copy(isLoading = true, errorMessage = null)
    viewModelScope.launch {
      runUiOperation {
          val organization = clerk.createOrganization(CreateOrganizationParams(name, slug))
          if (logoFile != null)
            runUiOperation { organization.setLogo(logoFile.organizationLogoInput()) }
          if (clerk.session?.id != sessionId) throw CoreException("session_changed")
          clerk.setActive(
            MobileSetActiveParams(
              session = Field.Value(MobileSetActiveParamsSession.Case1(sessionId)),
              organization = Field.Value(MobileSetActiveParamsOrganization.Case1(organization.id)),
            )
          )
          organization
        }
        .onSuccess { organization ->
          _state.value =
            _state.value.copy(
              isLoading = false,
              createdOrganization = organization,
              completedSession = clerk.session,
            )
        }
        .onFailure {
          _state.value = _state.value.copy(isLoading = false, errorMessage = it.displayMessage)
        }
    }
  }

  fun clearCompletedCreate() {
    _state.value = _state.value.copy(createdOrganization = null, completedSession = null)
  }

  fun clearError() {
    _state.value = _state.value.copy(errorMessage = null)
  }
}

internal data class OrganizationCreateFlowState(
  val isLoading: Boolean = false,
  val errorMessage: String? = null,
  val createdOrganization: Organization? = null,
  val completedSession: Session? = null,
)
