package com.clerk.ui.sessiontask.organization

import com.clerk.api.*
import com.clerk.ui.core.common.displayMessage
import com.clerk.ui.organizationlist.OrganizationAccountListState
import com.clerk.ui.organizationlist.OrganizationAccountListViewModel
import com.clerk.ui.organizationprofile.*

internal class SessionTaskChooseOrganizationViewModel(clerk: Clerk) :
  OrganizationAccountListViewModel(clerk) {

  fun selectOrganization(organizationId: String) {
    super.selectOrganization(organizationId = organizationId, onSelected = {})
  }

  override fun currentUser(): User? = currentTaskSession()?.user ?: clerk.user

  override fun currentSession(): Session? = currentTaskSession()

  override fun missingUserState(
    current: OrganizationAccountListState
  ): OrganizationAccountListState {
    return current.copy(isLoading = true, errorMessage = null)
  }

  override fun organizationSelectionErrorMessage(failure: Throwable): String {
    val code = (failure as? CoreException)?.errors?.firstOrNull()?.code
    if (code in ORGANIZATION_MEMBERSHIP_ERROR_CODES) {
      return if (state.value.canCreateOrganization) {
        "You are no longer a member of this organization. Please choose or create another one."
      } else {
        "You are no longer a member of this organization. Please choose another one."
      }
    }
    return failure.displayMessage
  }

  private fun currentTaskSession(): Session? =
    clerk.session?.takeIf { it.currentTask?.key == SessionTaskKey.ChooseOrganization }

  private companion object {
    val ORGANIZATION_MEMBERSHIP_ERROR_CODES =
      setOf("organization_not_found_or_unauthorized", "not_a_member_in_organization")
  }
}

internal typealias SessionTaskChooseOrganizationState = OrganizationAccountListState
