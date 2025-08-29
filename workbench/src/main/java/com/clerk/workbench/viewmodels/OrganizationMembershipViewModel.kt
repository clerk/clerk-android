package com.clerk.workbench.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clerk.api.Clerk
import com.clerk.api.network.serialization.longErrorMessageOrNull
import com.clerk.api.network.serialization.onFailure
import com.clerk.api.network.serialization.onSuccess
import com.clerk.api.organizations.removeMember
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class OrganizationMembershipViewModel : ViewModel() {

  private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
  val uiState = _uiState.asStateFlow()

  fun createMembership() {
    viewModelScope.launch {
      Clerk.user!!
        .organizationMemberships!!
        .first()
        .organization
        .removeMember(userId = "user_31wkL6tSr26OnLfA7OBbCpwdFg6")
        .onSuccess { Log.d("OrganizationMembershipViewModel", "removed member: $it") }
        .onFailure {
          Log.e(
            "OrganizationMembershipViewModel",
            "Failed to create membership: ${it.longErrorMessageOrNull}",
          )
        }
    }
  }

  sealed interface UiState {
    object Idle : UiState
  }
}
