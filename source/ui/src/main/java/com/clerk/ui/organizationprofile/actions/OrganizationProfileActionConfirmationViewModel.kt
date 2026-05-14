package com.clerk.ui.organizationprofile.actions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clerk.api.network.model.client.Client
import com.clerk.api.network.model.deleted.DeletedObject
import com.clerk.api.network.model.error.ClerkErrorResponse
import com.clerk.api.network.model.error.Error
import com.clerk.api.network.serialization.ClerkResult
import com.clerk.api.network.serialization.errorMessage
import com.clerk.api.organizations.Organization
import com.clerk.api.organizations.OrganizationMembership
import com.clerk.api.organizations.delete
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

internal class OrganizationProfileActionConfirmationViewModel(
  private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
  private val refreshClient: suspend () -> ClerkResult<Client, ClerkErrorResponse> = {
    Client.get()
  },
) : ViewModel() {

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
    viewModelScope.launch(dispatcher) {
      when (val result = performAction(action, organization, membership)) {
        is ClerkResult.Success -> {
          runCatching { refreshClient() }
          mutableState.value = mutableState.value.copy(isLoading = false, isComplete = true)
        }
        is ClerkResult.Failure ->
          mutableState.value =
            mutableState.value.copy(isLoading = false, errorMessage = result.errorMessage)
      }
    }
  }

  fun reset() {
    mutableState.value = OrganizationProfileActionConfirmationState()
  }

  private suspend fun performAction(
    action: OrganizationProfileConfirmationAction,
    organization: Organization,
    membership: OrganizationMembership?,
  ): ClerkResult<DeletedObject, ClerkErrorResponse> =
    when (action) {
      OrganizationProfileConfirmationAction.LeaveOrganization ->
        membership?.delete() ?: missingMembershipFailure()
      OrganizationProfileConfirmationAction.DeleteOrganization -> organization.delete()
    }

  private fun missingMembershipFailure(): ClerkResult.Failure<ClerkErrorResponse> {
    return ClerkResult.Failure(
      ClerkErrorResponse(errors = listOf(Error(longMessage = MISSING_MEMBERSHIP_ERROR)))
    )
  }
}

private const val MISSING_MEMBERSHIP_ERROR =
  "Unable to leave organization because no active membership was found."
