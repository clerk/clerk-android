package com.clerk.snapshot.organizationlist

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.clerk.api.Clerk
import com.clerk.api.organizations.OrganizationSuggestion
import com.clerk.api.organizations.PublicOrganizationData
import com.clerk.api.organizations.UserOrganizationInvitation
import com.clerk.api.ui.ClerkTheme
import com.clerk.base.BaseSnapshotTest
import com.clerk.ui.R
import com.clerk.ui.organizationlist.OrganizationAccountListActions
import com.clerk.ui.organizationlist.OrganizationAccountListContent
import com.clerk.ui.organizationlist.OrganizationAccountListHeader
import com.clerk.ui.organizationlist.OrganizationAccountListState
import com.clerk.ui.organizationswitcher.previewOrganizationMembership
import com.clerk.ui.organizationswitcher.previewOrganizationSwitcherUser
import com.clerk.ui.theme.ClerkMaterialTheme
import com.clerk.ui.theme.DefaultColors
import org.junit.Test

class OrganizationListSnapshotTest : BaseSnapshotTest() {

  @Test
  fun organizationListLoaded() {
    Clerk.customTheme = ClerkTheme(DefaultColors.light)
    paparazzi.snapshot {
      OrganizationListSnapshotSurface {
        OrganizationAccountListContent(
          modifier = Modifier.fillMaxSize(),
          state =
            OrganizationAccountListState(
              isLoading = false,
              hasLoadedInitialResources = true,
              canCreateOrganization = true,
              memberships = sampleMemberships,
              membershipsTotalCount = sampleMemberships.size,
              invitations = listOf(sampleInvitation()),
              invitationsTotalCount = 1,
              suggestions = listOf(sampleSuggestion()),
              suggestionsTotalCount = 1,
            ),
          user = previewOrganizationSwitcherUser(),
          activeOrganizationId = "org_acme",
          header =
            OrganizationAccountListHeader(
              title = stringResource(R.string.choose_an_account),
              subtitle = stringResource(R.string.select_the_account_with_which_you_wish_to_continue),
            ),
          showPersonalAccount = true,
          showSelectedAccessory = true,
          contentPadding = PaddingValues(24.dp),
          actions = noOpActions,
        )
      }
    }
  }

  @Composable
  private fun OrganizationListSnapshotSurface(content: @Composable () -> Unit) {
    ClerkMaterialTheme {
      Box(modifier = Modifier.size(740.dp).background(ClerkMaterialTheme.colors.background)) {
        content()
      }
    }
  }

  private companion object {
    val sampleMemberships =
      listOf(
        previewOrganizationMembership(
          organizationId = "org_acme",
          organizationName = "Acme Inc.",
          roleName = "Admin",
        ),
        previewOrganizationMembership(
          organizationId = "org_mosaic",
          organizationName = "Mosaic Labs",
          roleName = "Member",
        ),
      )

    val noOpActions =
      OrganizationAccountListActions(
        onRetryInitialLoad = {},
        onLoadMoreMemberships = {},
        onLoadMoreInvitations = {},
        onLoadMoreSuggestions = {},
        onSelectPersonalAccount = {},
        onSelectOrganization = {},
        onAcceptInvitation = {},
        onAcceptSuggestion = {},
        onCreateOrganization = {},
      )

    fun sampleInvitation(): UserOrganizationInvitation {
      return UserOrganizationInvitation(
        id = "inv_1",
        emailAddress = "ava@example.com",
        publicOrganizationData =
          UserOrganizationInvitation.PublicOrganizationData(
            hasImage = false,
            imageUrl = "",
            name = "Invited Co.",
            id = "org_invited",
            slug = "invited",
          ),
        publicMetadata = "{}",
        role = "org:member",
        status = "pending",
        createdAt = 0L,
        updatedAt = 0L,
      )
    }

    fun sampleSuggestion(): OrganizationSuggestion {
      return OrganizationSuggestion(
        id = "sug_1",
        publicOrganizationData =
          PublicOrganizationData(
            id = "org_suggested",
            hasImage = false,
            imageUrl = "",
            name = "Suggested Labs",
            slug = "suggested",
          ),
        status = "pending",
        createdAt = 1,
        updatedAt = 1,
      )
    }
  }
}
