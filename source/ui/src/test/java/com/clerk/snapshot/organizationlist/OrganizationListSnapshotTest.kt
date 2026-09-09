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
import com.clerk.api.*
import com.clerk.base.BaseSnapshotTest
import com.clerk.testing.*
import com.clerk.ui.R
import com.clerk.ui.organizationlist.OrganizationAccountListActions
import com.clerk.ui.organizationlist.OrganizationAccountListContent
import com.clerk.ui.organizationlist.OrganizationAccountListHeader
import com.clerk.ui.organizationlist.OrganizationAccountListState
import com.clerk.ui.theme.ClerkMaterialTheme
import com.clerk.ui.theme.ClerkTheme
import com.clerk.ui.theme.DefaultColors
import io.mockk.*
import org.junit.Test

class OrganizationListSnapshotTest : BaseSnapshotTest() {

  @Test
  fun organizationListLoaded() {
    snapshotTheme = ClerkTheme(DefaultColors.light)
    snapshot {
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
          user = snapshotUser(),
          activeOrganizationId = "org_acme",
          header =
            OrganizationAccountListHeader(
              title = stringResource(R.string.choose_an_account),
              subtitle =
                stringResource(R.string.select_the_account_with_which_you_wish_to_continue),
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
        snapshotMembership(
          organizationId = "org_acme",
          organizationName = "Acme Inc.",
          roleName = "Admin",
        ),
        snapshotMembership(
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

    fun sampleInvitation(): UserOrganizationInvitation =
      mockk(relaxed = true) {
        every { id } returns "inv_1"
        every { emailAddress } returns "ava@example.com"
        every { publicOrganizationData.id } returns "org_invited"
        every { publicOrganizationData.name } returns "Invited Co."
        every { publicOrganizationData.imageUrl } returns ""
        every { publicOrganizationData.hasImage } returns false
        every { role } returns "org:member"
        every { status } returns OrganizationInvitationStatus.Pending
      }

    fun sampleSuggestion(): OrganizationSuggestion =
      mockk(relaxed = true) {
        every { id } returns "sug_1"
        every { publicOrganizationData.id } returns "org_suggested"
        every { publicOrganizationData.name } returns "Suggested Labs"
        every { publicOrganizationData.imageUrl } returns ""
        every { publicOrganizationData.hasImage } returns false
        every { status } returns OrganizationSuggestionStatus.Pending
      }
  }
}
