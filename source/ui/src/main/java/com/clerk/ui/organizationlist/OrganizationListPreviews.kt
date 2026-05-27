package com.clerk.ui.organizationlist

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.clerk.api.organizations.OrganizationSuggestion
import com.clerk.api.organizations.PublicOrganizationData
import com.clerk.ui.R
import com.clerk.ui.organizationswitcher.previewOrganizationMembership
import com.clerk.ui.organizationswitcher.previewOrganizationSwitcherUser
import com.clerk.ui.theme.ClerkMaterialTheme

@PreviewLightDark
@Composable
private fun OrganizationListLoadedPreview() {
  PreviewSurface {
    OrganizationAccountListContent(
      state =
        OrganizationAccountListState(
          isLoading = false,
          hasLoadedInitialResources = true,
          canCreateOrganization = true,
          memberships = previewMemberships,
          membershipsTotalCount = previewMemberships.size,
          suggestions = listOf(previewSuggestion()),
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
      actions = previewActions(),
    )
  }
}

@Composable
private fun PreviewSurface(content: @Composable () -> Unit) {
  ClerkMaterialTheme {
    Box(
      modifier =
        Modifier.size(width = 390.dp, height = 740.dp)
          .background(ClerkMaterialTheme.colors.background)
    ) {
      content()
    }
  }
}

private val previewMemberships =
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

private fun previewActions(): OrganizationAccountListActions {
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
