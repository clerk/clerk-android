package com.clerk.workbench

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clerk.api.Clerk
import com.clerk.api.network.serialization.flatMap
import com.clerk.api.network.serialization.longErrorMessageOrNull
import com.clerk.api.network.serialization.onFailure
import com.clerk.api.network.serialization.onSuccess
import com.clerk.api.organizations.OrganizationInvitation
import com.clerk.api.organizations.getInvitations
import com.clerk.api.organizations.revoke
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class OrgViewModel : ViewModel() {

  val uiState = MutableStateFlow<UiState>(UiState.Idle)

  fun createInvite() {
    viewModelScope.launch {
      val org = requireNotNull(Clerk.user?.organizationMemberships?.first()!!.organization)
      org
        .getInvitations(status = OrganizationInvitation.Status.Pending)
        .flatMap { it.data.first().revoke() }
        .onFailure {
          Log.e("OrgViewModel", "Failed to create invite: ${it.longErrorMessageOrNull}")
        }
        .onSuccess { Log.d("OrgViewModel", "Invite created successfully") }
    }
  }

  sealed interface UiState {
    data object Idle : UiState
  }
}
