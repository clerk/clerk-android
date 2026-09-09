package com.clerk.ui.organizationswitcher

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.clerk.api.OrganizationSuggestion
import com.clerk.ui.core.dimens.dp24
import com.clerk.ui.core.preview.ClerkPreview
import com.clerk.ui.core.preview.previewResource
import com.clerk.ui.organizationlist.OrganizationAccountListActions
import com.clerk.ui.organizationlist.OrganizationAccountListState
import com.clerk.ui.theme.ClerkMaterialTheme

@PreviewLightDark
@Composable
private fun OrganizationSwitcherButtonPreview() {
  ClerkPreview("profile") {
    PreviewSurface {
      OrganizationSwitcherButton(
        modifier = Modifier.fillMaxWidth().padding(dp24),
        membership = previewOrganizationMembership(),
        isLoading = false,
        onClick = {},
      )
    }
  }
}

@PreviewLightDark
@Composable
private fun OrganizationSwitcherSheetContentPreview() {
  ClerkPreview("profile") {
    val memberships = listOf(previewOrganizationMembership())

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
}

@PreviewLightDark
@Composable
private fun OrganizationSwitcherOverviewSheetContentPreview() {
  ClerkPreview("profile") {
    PreviewSurface {
      OrganizationSwitcherOverviewSheetContent(
        membership = previewOrganizationMembership(),
        onManageOrganization = {},
        onSwitchAccount = {},
      )
    }
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

@Composable
private fun previewSuggestion(): OrganizationSuggestion =
  previewResource("OrganizationSuggestion") as OrganizationSuggestion
