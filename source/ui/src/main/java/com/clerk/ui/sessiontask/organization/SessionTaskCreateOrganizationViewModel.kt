package com.clerk.ui.sessiontask.organization

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clerk.api.Clerk
import com.clerk.api.log.ClerkLog
import com.clerk.api.network.serialization.errorMessage
import com.clerk.api.network.serialization.onFailure
import com.clerk.api.network.serialization.onSuccess
import com.clerk.api.organizations.Organization
import com.clerk.api.organizations.updateLogo
import com.clerk.api.session.Session
import com.clerk.api.session.SessionTaskKey
import com.clerk.api.session.pendingTaskKey
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal class SessionTaskCreateOrganizationViewModel : ViewModel() {

  private val _state = MutableStateFlow(SessionTaskCreateOrganizationState())
  val state = _state.asStateFlow()

  fun createOrganization(name: String, slug: String?, logoFile: File?) {
    val sessionId = currentTaskSessionId()
    if (sessionId == null) {
      _state.value = _state.value.copy(errorMessage = "Session does not exist")
      return
    }
    if (_state.value.isLoading) return

    _state.value = _state.value.copy(isLoading = true, errorMessage = null)
    viewModelScope.launch(Dispatchers.IO) {
      Organization.create(name = name, slug = slug)
        .onSuccess { organization ->
          uploadLogoIfNeeded(organization = organization, logoFile = logoFile)
          setActiveOrganization(sessionId = sessionId, organizationId = organization.id)
        }
        .onFailure { failure ->
          withContext(Dispatchers.Main) {
            _state.value = _state.value.copy(isLoading = false, errorMessage = failure.errorMessage)
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

  private fun currentTaskSessionId(): String? {
    val clientSession =
      runCatching {
          val client = Clerk.client
          val pendingChooseOrganizationSession =
            client.sessions.firstOrNull { it.pendingTaskKey == SessionTaskKey.CHOOSE_ORGANIZATION }
          val lastActiveSession =
            client.lastActiveSessionId?.let { lastActiveSessionId ->
              client.sessions.firstOrNull { it.id == lastActiveSessionId }
            }
          pendingChooseOrganizationSession ?: lastActiveSession
        }
        .getOrNull()

    return clientSession?.id ?: Clerk.session?.id
  }

  private suspend fun uploadLogoIfNeeded(organization: Organization, logoFile: File?) {
    if (logoFile == null) return

    organization.updateLogo(logoFile).onFailure {
      ClerkLog.e("Failed to set organization logo: ${it.errorMessage}")
    }
  }

  private suspend fun setActiveOrganization(sessionId: String, organizationId: String) {
    Clerk.auth
      .setActive(sessionId = sessionId, organizationId = organizationId)
      .onSuccess {
        withContext(Dispatchers.Main) {
          _state.value = _state.value.copy(isLoading = false, completedSession = it)
        }
      }
      .onFailure { failure ->
        withContext(Dispatchers.Main) {
          _state.value = _state.value.copy(isLoading = false, errorMessage = failure.errorMessage)
        }
      }
  }
}

internal data class SessionTaskCreateOrganizationState(
  val isLoading: Boolean = false,
  val errorMessage: String? = null,
  val completedSession: Session? = null,
)
