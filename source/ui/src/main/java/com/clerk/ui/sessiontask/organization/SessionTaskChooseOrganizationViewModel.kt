package com.clerk.ui.sessiontask.organization

import com.clerk.api.Clerk
import com.clerk.api.network.model.error.ClerkErrorResponse
import com.clerk.api.network.serialization.ClerkResult
import com.clerk.api.network.serialization.errorMessage
import com.clerk.api.session.Session
import com.clerk.api.session.SessionTaskKey
import com.clerk.api.session.pendingTaskKey
import com.clerk.api.user.User
import com.clerk.ui.organizationlist.OrganizationAccountListState
import com.clerk.ui.organizationlist.OrganizationAccountListViewModel

internal class SessionTaskChooseOrganizationViewModel : OrganizationAccountListViewModel() {

  fun selectOrganization(organizationId: String) {
    super.selectOrganization(organizationId = organizationId, onSelected = {})
  }

  override fun currentUser(): User? = currentTaskSession()?.user ?: Clerk.user

  override fun currentSession(): Session? = currentTaskSession()

  override fun missingUserState(
    current: OrganizationAccountListState
  ): OrganizationAccountListState {
    return current.copy(isLoading = true, errorMessage = null)
  }

  override fun organizationSelectionErrorMessage(
    failure: ClerkResult.Failure<ClerkErrorResponse>
  ): String {
    val code = failure.error?.errors?.firstOrNull()?.code
    if (code in ORGANIZATION_MEMBERSHIP_ERROR_CODES) {
      return if (state.value.canCreateOrganization) {
        "You are no longer a member of this organization. Please choose or create another one."
      } else {
        "You are no longer a member of this organization. Please choose another one."
      }
    }
    return failure.errorMessage
  }

  private fun currentTaskSession(): Session? {
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

    return clientSession ?: Clerk.session
  }

  private companion object {
    val ORGANIZATION_MEMBERSHIP_ERROR_CODES =
      setOf("organization_not_found_or_unauthorized", "not_a_member_in_organization")
  }
}

internal typealias SessionTaskChooseOrganizationState = OrganizationAccountListState
