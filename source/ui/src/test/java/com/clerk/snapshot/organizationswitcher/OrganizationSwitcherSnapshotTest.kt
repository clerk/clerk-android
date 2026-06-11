package com.clerk.snapshot.organizationswitcher

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.clerk.api.Clerk
import com.clerk.api.ui.ClerkTheme
import com.clerk.base.BaseSnapshotTest
import com.clerk.ui.core.dimens.dp24
import com.clerk.ui.organizationlist.OrganizationAccountListActions
import com.clerk.ui.organizationlist.OrganizationAccountListState
import com.clerk.ui.organizationswitcher.OrganizationSwitcherAccountListSheetContent
import com.clerk.ui.organizationswitcher.OrganizationSwitcherButton
import com.clerk.ui.organizationswitcher.OrganizationSwitcherCustomTrigger
import com.clerk.ui.organizationswitcher.OrganizationSwitcherDisplayMode
import com.clerk.ui.organizationswitcher.OrganizationSwitcherOverviewSheetContent
import com.clerk.ui.organizationswitcher.previewOrganizationMembership
import com.clerk.ui.organizationswitcher.previewOrganizationSwitcherUser
import com.clerk.ui.theme.ClerkMaterialTheme
import com.clerk.ui.theme.DefaultColors
import org.junit.Test

class OrganizationSwitcherSnapshotTest : BaseSnapshotTest() {

  @Test
  fun organizationSwitcherNormalActiveOrganization() {
    Clerk.customTheme = ClerkTheme(DefaultColors.light)
    paparazzi.snapshot {
      SwitcherSnapshotSurface {
        OrganizationSwitcherButton(
          modifier = Modifier.fillMaxWidth(),
          membership = previewOrganizationMembership(),
          isLoading = false,
          onClick = {},
        )
      }
    }
  }

  @Test
  fun organizationSwitcherCompactActiveOrganization() {
    Clerk.customTheme = ClerkTheme(DefaultColors.light)
    paparazzi.snapshot {
      SwitcherSnapshotSurface {
        OrganizationSwitcherButton(
          membership = previewOrganizationMembership(),
          displayMode = OrganizationSwitcherDisplayMode.Compact,
          isLoading = false,
          onClick = {},
        )
      }
    }
  }

  @Test
  fun organizationSwitcherLoading() {
    Clerk.customTheme = ClerkTheme(DefaultColors.light)
    paparazzi.snapshot {
      SwitcherSnapshotSurface {
        OrganizationSwitcherButton(
          modifier = Modifier.fillMaxWidth(),
          membership = null,
          isLoading = true,
          onClick = {},
        )
      }
    }
  }

  @Test
  fun organizationSwitcherNoActiveOrganization() {
    Clerk.customTheme = ClerkTheme(DefaultColors.light)
    paparazzi.snapshot {
      SwitcherSnapshotSurface {
        OrganizationSwitcherButton(
          modifier = Modifier.fillMaxWidth(),
          membership = null,
          user = previewOrganizationSwitcherUser(),
          isLoading = false,
          onClick = {},
        )
      }
    }
  }

  @Test
  fun organizationSwitcherCustomTrigger() {
    Clerk.customTheme = ClerkTheme(DefaultColors.light)
    paparazzi.snapshot {
      SwitcherSnapshotSurface {
        OrganizationSwitcherCustomTrigger(isLoading = false, onClick = {}) {
          Text(text = "Switch account", color = ClerkMaterialTheme.colors.foreground)
        }
      }
    }
  }

  @Test
  fun organizationSwitcherOverviewSheet() {
    Clerk.customTheme = ClerkTheme(DefaultColors.light)
    paparazzi.snapshot {
      ClerkMaterialTheme {
        Box(modifier = Modifier.background(ClerkMaterialTheme.colors.background)) {
          OrganizationSwitcherOverviewSheetContent(
            membership = previewOrganizationMembership(),
            onManageOrganization = {},
            onSwitchAccount = {},
          )
        }
      }
    }
  }

  @Test
  fun organizationSwitcherAccountListSheet() {
    Clerk.customTheme = ClerkTheme(DefaultColors.light)
    paparazzi.snapshot {
      ClerkMaterialTheme {
        Box(modifier = Modifier.background(ClerkMaterialTheme.colors.background)) {
          OrganizationSwitcherAccountListSheetContent(
            state =
              OrganizationAccountListState(
                isLoading = false,
                hasLoadedInitialResources = true,
                canCreateOrganization = true,
                memberships = sampleMemberships,
                membershipsTotalCount = sampleMemberships.size,
              ),
            user = previewOrganizationSwitcherUser(),
            activeOrganizationId = null,
            showPersonalAccount = true,
            showCreateOrganization = true,
            actions = previewOrganizationAccountListActions(),
            onErrorShown = {},
          )
        }
      }
    }
  }

  @Test
  fun organizationSwitcherAccountListSheetLoading() {
    Clerk.customTheme = ClerkTheme(DefaultColors.light)
    paparazzi.snapshot {
      ClerkMaterialTheme {
        Box(modifier = Modifier.background(ClerkMaterialTheme.colors.background)) {
          OrganizationSwitcherAccountListSheetContent(
            state = OrganizationAccountListState(isLoading = true),
            user = previewOrganizationSwitcherUser(),
            activeOrganizationId = null,
            showPersonalAccount = true,
            showCreateOrganization = false,
            actions = previewOrganizationAccountListActions(),
            onErrorShown = {},
          )
        }
      }
    }
  }

  @Composable
  private fun SwitcherSnapshotSurface(content: @Composable () -> Unit) {
    ClerkMaterialTheme {
      Box(
        modifier =
          Modifier.fillMaxWidth().background(ClerkMaterialTheme.colors.background).padding(dp24)
      ) {
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
        previewOrganizationMembership(
          organizationId = "org_clerk",
          organizationName = "Clerk",
          roleName = "Owner",
        ),
      )

    fun previewOrganizationAccountListActions(): OrganizationAccountListActions {
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
  }
}
