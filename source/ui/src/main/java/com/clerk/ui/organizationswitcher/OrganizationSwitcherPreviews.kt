package com.clerk.ui.organizationswitcher

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.clerk.ui.core.dimens.dp24
import com.clerk.ui.theme.ClerkMaterialTheme
import kotlinx.collections.immutable.toImmutableList

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
      .toImmutableList()

  PreviewSurface {
    OrganizationSwitcherSheetContent(
      state =
        OrganizationSwitcherState(
          memberships = memberships,
          membershipsTotalCount = memberships.size,
        ),
      memberships = memberships,
      activeOrganizationId = "org_acme",
      actions = OrganizationSwitcherSheetActions(onLoadMore = {}, onSelect = {}, onErrorShown = {}),
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
