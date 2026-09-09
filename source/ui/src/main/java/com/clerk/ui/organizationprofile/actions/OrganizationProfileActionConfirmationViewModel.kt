package com.clerk.ui.organizationprofile.actions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clerk.api.*
import com.clerk.ui.core.common.displayMessage
import com.clerk.ui.core.common.runUiOperation
import com.clerk.ui.organizationprofile.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

internal class OrganizationProfileActionConfirmationViewModel(private val clerk: Clerk) :
  ViewModel() {

  private val mutableState = MutableStateFlow(OrganizationProfileActionConfirmationState())
  val state = mutableState.asStateFlow()

  fun setConfirmationText(text: String) {
    mutableState.value = mutableState.value.copy(confirmationText = text)
  }

  fun confirm(
    action: OrganizationProfileConfirmationAction,
    organization: Organization,
    membership: OrganizationMembership?,
  ) {
    val current = mutableState.value
    if (!current.canSubmit(organization.name) || current.isLoading || current.isComplete) return

    if (action == OrganizationProfileConfirmationAction.LeaveOrganization && membership == null) {
      mutableState.value = current.copy(errorMessage = MISSING_MEMBERSHIP_ERROR)
      return
    }

    mutableState.value = current.copy(isLoading = true, errorMessage = null)
    viewModelScope.launch {
      runUiOperation {
          when (action) {
            OrganizationProfileConfirmationAction.LeaveOrganization ->
              requireNotNull(membership).destroy()
            OrganizationProfileConfirmationAction.DeleteOrganization -> organization.destroy()
          }
        }
        .onSuccess {
          mutableState.value = mutableState.value.copy(isLoading = false, isComplete = true)
        }
        .onFailure {
          mutableState.value =
            mutableState.value.copy(isLoading = false, errorMessage = it.displayMessage)
        }
    }
  }

  fun reset() {
    mutableState.value = OrganizationProfileActionConfirmationState()
  }

  fun clearError() {
    mutableState.value = mutableState.value.copy(errorMessage = null)
  }
}

private const val MISSING_MEMBERSHIP_ERROR =
  "Unable to leave organization because no active membership was found."
