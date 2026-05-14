package com.clerk.snapshot.organizationprofile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.clerk.api.network.model.userdata.PublicUserData
import com.clerk.api.organizations.OrganizationInvitation
import com.clerk.api.organizations.OrganizationMembership
import com.clerk.api.organizations.OrganizationMembershipRequest
import com.clerk.api.organizations.Role
import com.clerk.base.BaseSnapshotTest
import com.clerk.ui.R
import com.clerk.ui.organizationprofile.custom.OrganizationProfileCustomRow
import com.clerk.ui.organizationprofile.custom.OrganizationProfileCustomRowPlacement
import com.clerk.ui.organizationprofile.custom.OrganizationProfileRow
import com.clerk.ui.organizationprofile.custom.OrganizationProfileRowIcon
import com.clerk.ui.organizationprofile.members.OrganizationMembersActions
import com.clerk.ui.organizationprofile.members.OrganizationMembersContent
import com.clerk.ui.organizationprofile.members.OrganizationMembersState
import com.clerk.ui.organizationprofile.members.OrganizationMembersTab
import com.clerk.ui.organizationprofile.previewOrganizationProfileMembership
import com.clerk.ui.organizationprofile.previewOrganizationProfileOrganization
import com.clerk.ui.organizationprofile.root.OrganizationProfileRootView
import com.clerk.ui.theme.ClerkMaterialTheme
import kotlinx.collections.immutable.persistentListOf
import kotlinx.serialization.json.JsonNull
import org.junit.Test

class OrganizationProfileSnapshotTest : BaseSnapshotTest() {

  @Test
  fun organizationProfileRoot() {
    paparazzi.snapshot {
      ClerkMaterialTheme {
        Box(modifier = Modifier.size(740.dp).background(ClerkMaterialTheme.colors.background)) {
          OrganizationProfileRootView(
            modifier = Modifier.fillMaxSize(),
            organization = previewOrganizationProfileOrganization(),
            membership = previewOrganizationProfileMembership(),
            onBackPressed = {},
            onUpdateProfile = {},
            onAction = {},
          )
        }
      }
    }
  }

  @Test
  fun organizationProfileRootWithCustomRows() {
    paparazzi.snapshot {
      ClerkMaterialTheme {
        Box(modifier = Modifier.size(740.dp).background(ClerkMaterialTheme.colors.background)) {
          OrganizationProfileRootView(
            modifier = Modifier.fillMaxSize(),
            organization = previewOrganizationProfileOrganization(),
            membership = previewOrganizationProfileMembership(),
            onBackPressed = {},
            onUpdateProfile = {},
            onAction = {},
            customRows =
              persistentListOf(
                OrganizationProfileCustomRow(
                  routeKey = "billing",
                  title = "Billing",
                  icon = OrganizationProfileRowIcon.Resource(R.drawable.ic_credit_card),
                  placement =
                    OrganizationProfileCustomRowPlacement.After(OrganizationProfileRow.Members),
                ),
                OrganizationProfileCustomRow(
                  routeKey = "preferences",
                  title = "Preferences",
                  icon = OrganizationProfileRowIcon.Resource(R.drawable.ic_cog),
                  placement =
                    OrganizationProfileCustomRowPlacement.Before(
                      OrganizationProfileRow.LeaveOrganization
                    ),
                ),
              ),
          )
        }
      }
    }
  }

  @Test
  fun organizationMembersLoading() {
    paparazzi.snapshot {
      MembersSnapshotSurface {
        OrganizationMembersContent(
          organization = previewOrganizationProfileOrganization(),
          viewerMembership = previewOrganizationProfileMembership(),
          state =
            OrganizationMembersState(
              availableTabs = allMembersTabs,
              selectedTab = OrganizationMembersTab.Members,
              isLoadingInitial = true,
            ),
          actions = noOpMembersActions,
        )
      }
    }
  }

  @Test
  fun organizationMembersMembersTab() {
    paparazzi.snapshot {
      MembersSnapshotSurface {
        OrganizationMembersContent(
          organization = previewOrganizationProfileOrganization(),
          viewerMembership = previewOrganizationProfileMembership(),
          state =
            OrganizationMembersState(
              availableTabs = allMembersTabs,
              selectedTab = OrganizationMembersTab.Members,
              roles = sampleRoles,
              members = listOf(sampleMember("mem_1", "Ada", "Lovelace")),
              membersTotalCount = 1,
            ),
          actions = noOpMembersActions,
        )
      }
    }
  }

  @Test
  fun organizationMembersInvitationsTab() {
    paparazzi.snapshot {
      MembersSnapshotSurface {
        OrganizationMembersContent(
          organization = previewOrganizationProfileOrganization(),
          viewerMembership = previewOrganizationProfileMembership(),
          state =
            OrganizationMembersState(
              availableTabs = allMembersTabs,
              selectedTab = OrganizationMembersTab.Invitations,
              roles = sampleRoles,
              selectedInviteRoleKey = "org:member",
              inviteEmails = listOf("new@example.com"),
              invitations = listOf(sampleInvitation("inv_1")),
              invitationsTotalCount = 1,
            ),
          actions = noOpMembersActions,
        )
      }
    }
  }

  @Test
  fun organizationMembersRequestsTab() {
    paparazzi.snapshot {
      MembersSnapshotSurface {
        OrganizationMembersContent(
          organization = previewOrganizationProfileOrganization(),
          viewerMembership = previewOrganizationProfileMembership(),
          state =
            OrganizationMembersState(
              availableTabs = allMembersTabs,
              selectedTab = OrganizationMembersTab.Requests,
              requests = listOf(sampleRequest("req_1")),
              requestsTotalCount = 1,
            ),
          actions = noOpMembersActions,
        )
      }
    }
  }

  @Test
  fun organizationMembersEmptyState() {
    paparazzi.snapshot {
      MembersSnapshotSurface {
        OrganizationMembersContent(
          organization = previewOrganizationProfileOrganization(),
          viewerMembership = previewOrganizationProfileMembership(),
          state =
            OrganizationMembersState(
              availableTabs = allMembersTabs,
              selectedTab = OrganizationMembersTab.Members,
            ),
          actions = noOpMembersActions,
        )
      }
    }
  }

  private val allMembersTabs =
    listOf(
      OrganizationMembersTab.Members,
      OrganizationMembersTab.Invitations,
      OrganizationMembersTab.Requests,
    )

  private val sampleRoles =
    listOf(
      Role(
        id = "role_admin",
        key = "org:admin",
        name = "Admin",
        description = "Admin",
        permissions = emptyList(),
        createdAt = 1,
        updatedAt = 1,
      ),
      Role(
        id = "role_member",
        key = "org:member",
        name = "Member",
        description = "Member",
        permissions = emptyList(),
        createdAt = 1,
        updatedAt = 1,
      ),
    )

  private val noOpMembersActions =
    OrganizationMembersActions(
      onRetry = {},
      onSelectTab = {},
      onMemberSearchChanged = {},
      onLoadMoreMembers = {},
      onLoadMoreInvitations = {},
      onLoadMoreRequests = {},
      onSelectInviteRole = {},
      onInviteInputSubmitted = {},
      onRemoveInviteEmail = {},
      onSendInvitations = {},
      onUpdateMemberRole = { _, _ -> },
      onRemoveMember = {},
      onRevokeInvitation = {},
      onAcceptRequest = {},
      onRejectRequest = {},
    )

  @Composable
  private fun MembersSnapshotSurface(content: @Composable () -> Unit) {
    ClerkMaterialTheme {
      Box(modifier = Modifier.size(740.dp).background(ClerkMaterialTheme.colors.background)) {
        content()
      }
    }
  }

  private fun sampleMember(
    id: String,
    firstName: String,
    lastName: String,
  ): OrganizationMembership {
    return OrganizationMembership(
      id = id,
      publicMetadata = JsonNull,
      role = "org:member",
      roleName = "Member",
      permissions = emptyList(),
      publicUserData = samplePublicUserData(id, firstName, lastName),
      organization = previewOrganizationProfileOrganization(),
      createdAt = 1,
      updatedAt = 1,
    )
  }

  private fun sampleInvitation(id: String): OrganizationInvitation {
    return OrganizationInvitation(
      id = id,
      emailAddress = "new.member@example.com",
      organizationId = "org_acme",
      publicMetadata = JsonNull,
      role = "org:member",
      status = OrganizationInvitation.Status.Pending,
      createdAt = 1,
      updatedAt = 1,
    )
  }

  private fun sampleRequest(id: String): OrganizationMembershipRequest {
    return OrganizationMembershipRequest(
      id = id,
      organizationId = "org_acme",
      publicUserData = samplePublicUserData(id, "Grace", "Hopper"),
      status = "pending",
      createdAt = 1,
      updatedAt = 1,
    )
  }

  private fun samplePublicUserData(
    id: String,
    firstName: String,
    lastName: String,
  ): PublicUserData {
    return PublicUserData(
      firstName = firstName,
      lastName = lastName,
      imageUrl = "",
      hasImage = false,
      identifier = "${firstName.lowercase()}@example.com",
      userId = "user_$id",
    )
  }
}
