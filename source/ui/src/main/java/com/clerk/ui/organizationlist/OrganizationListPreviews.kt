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
import com.clerk.api.OrganizationSuggestion
import com.clerk.ui.R
import com.clerk.ui.core.preview.ClerkPreview
import com.clerk.ui.core.preview.previewResource
import com.clerk.ui.organizationswitcher.previewOrganizationMembership
import com.clerk.ui.organizationswitcher.previewOrganizationSwitcherUser
import com.clerk.ui.theme.ClerkMaterialTheme

@PreviewLightDark
@Composable
private fun OrganizationListLoadedPreview() {
  ClerkPreview("profile") {
    PreviewSurface {
      OrganizationAccountListContent(
        state =
          OrganizationAccountListState(
            isLoading = false,
            hasLoadedInitialResources = true,
            canCreateOrganization = true,
            memberships = previewMemberships(),
            membershipsTotalCount = previewMemberships().size,
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

@Composable private fun previewMemberships() = listOf(previewOrganizationMembership())

@Composable
private fun previewSuggestion(): OrganizationSuggestion =
  previewResource("OrganizationSuggestion") as OrganizationSuggestion

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
