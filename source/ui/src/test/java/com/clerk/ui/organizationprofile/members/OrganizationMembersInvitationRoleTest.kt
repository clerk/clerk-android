package com.clerk.ui.organizationprofile.members

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.clerk.api.organizations.OrganizationInvitation
import com.clerk.api.organizations.Role
import com.clerk.ui.theme.ClerkMaterialTheme
import kotlinx.serialization.json.JsonNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class OrganizationMembersInvitationRoleTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun invitationShowsTheNameOfItsRole() {
    showInvitations(roles = listOf(role(key = "org:member", name = "Member")))

    composeTestRule.onNodeWithText("Member").assertExists()
    composeTestRule.onNodeWithText("org:member").assertDoesNotExist()
  }

  @Test
  fun invitationShowsTheRoleKeyWhenNoRoleMatches() {
    showInvitations(roles = listOf(role(key = "org:admin", name = "Admin")))

    composeTestRule.onNodeWithText("org:member").assertExists()
  }

  private fun showInvitations(roles: List<Role>) {
    composeTestRule.setContent {
      ClerkMaterialTheme {
        OrganizationMembersContent(
          viewerMembership = null,
          state =
            OrganizationMembersState(
              availableTabs = listOf(OrganizationMembersTab.Invitations),
              selectedTab = OrganizationMembersTab.Invitations,
              roles = roles,
              invitations = listOf(invitation(role = "org:member")),
            ),
          actions = noActions(),
        )
      }
    }
  }

  private fun invitation(role: String): OrganizationInvitation {
    return OrganizationInvitation(
      id = "orginv_1",
      emailAddress = "invitee@example.com",
      organizationId = "org_123",
      publicMetadata = JsonNull,
      role = role,
      status = OrganizationInvitation.Status.Pending,
      createdAt = 1,
      updatedAt = 1,
    )
  }

  private fun role(key: String, name: String): Role {
    return Role(
      id = key,
      key = key,
      name = name,
      permissions = emptyList(),
      createdAt = 1,
      updatedAt = 1,
    )
  }

  private fun noActions(): OrganizationMembersActions {
    return OrganizationMembersActions(
      onRetry = {},
      onSelectTab = {},
      onMemberSearchChanged = {},
      onSubmitMemberSearch = {},
      onLoadMoreMembers = {},
      onLoadMoreInvitations = {},
      onLoadMoreRequests = {},
      onUpdateMemberRole = { _, _ -> },
      onRemoveMember = {},
      onRevokeInvitation = {},
      onAcceptRequest = {},
      onRejectRequest = {},
    )
  }
}
