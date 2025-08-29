package com.clerk.workbench.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clerk.api.Clerk
import com.clerk.api.network.serialization.flatMap
import com.clerk.api.network.serialization.longErrorMessageOrNull
import com.clerk.api.network.serialization.onFailure
import com.clerk.api.network.serialization.onSuccess
import com.clerk.api.organizations.OrganizationDomain
import com.clerk.api.organizations.createDomain
import com.clerk.api.organizations.delete
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

  fun deleteOrgDomain() {
    viewModelScope.launch {
      val org = Clerk.user?.organizationMemberships?.first()!!.organization
      org
        .getDomains()
        .flatMap { it.data.first().delete() }
        .onSuccess { _uiState.value = OrgDomainUiState.DomainDeleted }
        .onFailure {
          Log.e("OrgDomainViewModel", "Failed to fetch domains: ${it.longErrorMessageOrNull}")
        }
    }
  }

  sealed interface OrgDomainUiState {
    data class DomainsFetched(val domains: List<OrganizationDomain>) : OrgDomainUiState

    data object Idle : OrgDomainUiState

    data object DomainCreated : OrgDomainUiState

    data object DomainDeleted : OrgDomainUiState
  }
}
