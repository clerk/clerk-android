package com.clerk.workbench.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clerk.api.Clerk
import com.clerk.api.network.serialization.longErrorMessageOrNull
import com.clerk.api.network.serialization.onFailure
import com.clerk.api.network.serialization.onSuccess
import com.clerk.api.organizations.OrganizationDomain
import com.clerk.api.organizations.createDomain
import com.clerk.api.organizations.getDomains
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class OrgDomainViewModel : ViewModel() {

  private val _uiState = MutableStateFlow<OrgDomainUiState>(OrgDomainUiState.Idle)
  val uiState = _uiState.asStateFlow()

  fun createOrgDomain() {
    viewModelScope.launch {
      val org = Clerk.user?.organizationMemberships?.first()!!.organization
      org
        .createDomain(name = "4-domain.com")
        .onSuccess { _uiState.value = OrgDomainUiState.DomainCreated }
        .onFailure {
          Log.e("OrgDomainViewModel", "Failed to create domain: ${it.longErrorMessageOrNull}")
        }
    }
  }

  fun getAllDomains() {
    viewModelScope.launch {
      val org = Clerk.user?.organizationMemberships?.first()!!.organization
      org
        .getDomains()
        .onSuccess { _uiState.value = OrgDomainUiState.DomainsFetched(it.data) }
        .onFailure {
          Log.e("OrgDomainViewModel", "Failed to fetch domains: ${it.longErrorMessageOrNull}")
        }
    }
  }

  fun setActive() {
    viewModelScope.launch {
      val session = Clerk.client.sessions.first()
      Clerk.setActive(session.id, organizationId = "org_31qYzhGqspBIUkeg0FYWzC3P0dY")
    }
  }

  sealed interface OrgDomainUiState {
    data class DomainsFetched(val domains: List<OrganizationDomain>) : OrgDomainUiState

    data object Idle : OrgDomainUiState

    data object DomainCreated : OrgDomainUiState
  }
}
