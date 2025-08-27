package com.clerk.workbench.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clerk.api.network.serialization.flatMap
import com.clerk.api.network.serialization.longErrorMessageOrNull
import com.clerk.api.network.serialization.onFailure
import com.clerk.api.network.serialization.onSuccess
import com.clerk.api.organizations.Organization
import com.clerk.api.organizations.delete
import com.clerk.api.organizations.deleteLogo
import com.clerk.api.organizations.update
import com.clerk.api.organizations.updateLogo
import com.clerk.api.signin.SignIn
import java.io.File
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class OrgViewModel : ViewModel() {

  private val _uiState = MutableStateFlow<OrgUiState>(OrgUiState.SignedOut)
  val uiState = _uiState.asStateFlow()

  // region Top level org functions
  fun signIn(email: String, password: String) {
    viewModelScope.launch {
      SignIn.create(SignIn.CreateParams.Strategy.Password(identifier = email, password = password))
        .onSuccess { _uiState.value = OrgUiState.SignedIn }
        .onFailure { Log.e("SignInViewModel", "${it.longErrorMessageOrNull}", it.throwable) }
    }
  }

  fun createOrg() {
    viewModelScope.launch { Organization.create("Crispy Org") }
  }

  fun getOrg() {
    viewModelScope.launch {
      Organization.get("org_31qVA5lAwmdNaa2DDMNANzLViDu")
        .onSuccess { _uiState.value = OrgUiState.OrgFetched(it) }
        .onFailure { Log.e("MainViewModel", "${it.longErrorMessageOrNull}", it.throwable) }
    }
  }

  fun updateOrg() {
    viewModelScope.launch {
      Organization.get("org_31qVA5lAwmdNaa2DDMNANzLViDu")
        .flatMap { it.update("Crispy Sunglasses Org") }
        .onSuccess { _uiState.value = OrgUiState.OrgUpdated(it) }
        .onFailure { Log.e("MainViewModel", "${it.longErrorMessageOrNull}", it.throwable) }
    }
  }

  fun updateOrgLogo(file: File) {
    viewModelScope.launch {
      Organization.get("org_31qVA5lAwmdNaa2DDMNANzLViDu")
        .flatMap { it.updateLogo(file) }
        .onSuccess { _uiState.value = OrgUiState.LogoUpdated(it) }
    }
  }

  fun deleteOrgLogo() {
    viewModelScope.launch {
      Organization.get("org_31qVA5lAwmdNaa2DDMNANzLViDu").flatMap { it.deleteLogo() }
    }
  }

  fun deleteOrg() {
    viewModelScope.launch {
      Organization.get("org_31qVA5lAwmdNaa2DDMNANzLViDu").flatMap { it.delete() }
    }
  }

  // endregion

  sealed interface OrgUiState {
    data object SignedOut : OrgUiState

    data object SignedIn : OrgUiState

    data class OrgFetched(val organization: Organization) : OrgUiState

    data class OrgUpdated(val organization: Organization) : OrgUiState

    data class LogoUpdated(val organization: Organization) : OrgUiState
  }
}
