package com.clerk.ui.organizationswitcher

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.clerk.api.organizations.OrganizationSuggestion
import com.clerk.api.organizations.PublicOrganizationData
import com.clerk.ui.core.dimens.dp24
import com.clerk.ui.organizationlist.OrganizationAccountListActions
import com.clerk.ui.organizationlist.OrganizationAccountListState
import com.clerk.ui.theme.ClerkMaterialTheme

@PreviewLightDark
@Composable
private fun OrganizationSwitcherButtonPreview() {
  PreviewSurface {
    OrganizationSwitcherButton(
      modifier = Modifier.fillMaxWidth().padding(dp24),
      membership = previewOrganizationMembership(),
      isLoading = false,
      onClick = {},
    )
  }
}

@PreviewLightDark
@Composable
private fun OrganizationSwitcherSheetContentPreview() {
  val memberships =
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
      previewOrganizationMembership(
        organizationId = "org_clerk",
        organizationName = "Clerk",
        roleName = "Owner",
      ),
    )

  PreviewSurface {
    OrganizationSwitcherAccountListSheetContent(
      state =
        OrganizationAccountListState(
          isLoading = false,
          hasLoadedInitialResources = true,
          canCreateOrganization = true,
          memberships = memberships,
          membershipsTotalCount = memberships.size,
          suggestions = listOf(previewSuggestion()),
          suggestionsTotalCount = 1,
        ),
      user = previewOrganizationSwitcherUser(),
      activeOrganizationId = "org_acme",
      showPersonalAccount = true,
      showCreateOrganization = true,
      actions = previewOrganizationAccountListActions(),
      onErrorShown = {},
    )
  }
}

@PreviewLightDark
@Composable
private fun OrganizationSwitcherOverviewSheetContentPreview() {
  PreviewSurface {
    OrganizationSwitcherOverviewSheetContent(
      membership = previewOrganizationMembership(),
      onManageOrganization = {},
      onSwitchAccount = {},
    )
  }
}

@Composable
private fun PreviewSurface(content: @Composable () -> Unit) {
  ClerkMaterialTheme {
    Box(modifier = Modifier.fillMaxWidth().background(ClerkMaterialTheme.colors.background)) {
      content()
    }
  }
}

private fun previewOrganizationAccountListActions(): OrganizationAccountListActions {
  return OrganizationAccountListActions(
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
}

private fun previewSuggestion(): OrganizationSuggestion {
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
