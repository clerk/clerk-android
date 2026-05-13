package com.clerk.snapshot.organizationswitcher

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import com.clerk.api.Clerk
import com.clerk.api.ui.ClerkTheme
import com.clerk.base.BaseSnapshotTest
import com.clerk.ui.core.dimens.dp24
import com.clerk.ui.organizationswitcher.OrganizationSwitcherButton
import com.clerk.ui.organizationswitcher.OrganizationSwitcherSheetActions
import com.clerk.ui.organizationswitcher.OrganizationSwitcherSheetContent
import com.clerk.ui.organizationswitcher.OrganizationSwitcherState
import com.clerk.ui.organizationswitcher.previewOrganizationMembership
import com.clerk.ui.theme.ClerkMaterialTheme
import com.clerk.ui.theme.DefaultColors
import kotlinx.collections.immutable.toImmutableList
import org.junit.Test

class OrganizationSwitcherSnapshotTest : BaseSnapshotTest() {

  @Test
  fun organizationSwitcherButtonLight() {
    Clerk.customTheme = ClerkTheme(DefaultColors.light)
    paparazzi.snapshot {
      ClerkMaterialTheme {
        Box(
          modifier =
            Modifier.fillMaxWidth().background(ClerkMaterialTheme.colors.background).padding(dp24)
        ) {
          OrganizationSwitcherButton(
            modifier = Modifier.fillMaxWidth(),
            membership = previewOrganizationMembership(),
            isLoading = false,
            onClick = {},
          )
        }
      }
    }
  }

  @Test
  fun organizationSwitcherButtonEmpty() {
    Clerk.customTheme = ClerkTheme(DefaultColors.light)
    paparazzi.snapshot {
      ClerkMaterialTheme {
        Box(
          modifier =
            Modifier.fillMaxWidth().background(ClerkMaterialTheme.colors.background).padding(dp24)
        ) {
          OrganizationSwitcherButton(
            modifier = Modifier.fillMaxWidth(),
            membership = null,
            isLoading = false,
            onClick = {},
          )
        }
      }
    }
  }

  @Test
  fun organizationSwitcherButtonLoading() {
    Clerk.customTheme = ClerkTheme(DefaultColors.light)
    paparazzi.snapshot {
      ClerkMaterialTheme {
        Box(
          modifier =
            Modifier.fillMaxWidth().background(ClerkMaterialTheme.colors.background).padding(dp24)
        ) {
          OrganizationSwitcherButton(
            modifier = Modifier.fillMaxWidth(),
            membership = previewOrganizationMembership(),
            isLoading = true,
            onClick = {},
          )
        }
      }
    }
  }

  @Test
  fun organizationSwitcherButtonDark() {
    Clerk.customTheme = ClerkTheme(DefaultColors.dark)
    paparazzi.snapshot {
      ClerkMaterialTheme {
        Box(
          modifier =
            Modifier.fillMaxWidth().background(ClerkMaterialTheme.colors.background).padding(dp24)
        ) {
          OrganizationSwitcherButton(
            modifier = Modifier.fillMaxWidth(),
            membership = previewOrganizationMembership(),
            isLoading = false,
            onClick = {},
          )
        }
      }
    }
  }

  @Test
  fun organizationSwitcherSheetContentLight() {
    Clerk.customTheme = ClerkTheme(DefaultColors.light)
    paparazzi.snapshot {
      ClerkMaterialTheme {
        Box(modifier = Modifier.background(ClerkMaterialTheme.colors.background)) {
          OrganizationSwitcherSheetContent(
            state =
              OrganizationSwitcherState(
                memberships = sampleMemberships,
                membershipsTotalCount = sampleMemberships.size,
              ),
            memberships = sampleMemberships.toImmutableList(),
            activeOrganizationId = "org_acme",
            actions =
              OrganizationSwitcherSheetActions(onLoadMore = {}, onSelect = {}, onErrorShown = {}),
          )
        }
      }
    }
  }

  @Test
  fun organizationSwitcherSheetContentDark() {
    Clerk.customTheme = ClerkTheme(DefaultColors.dark)
    paparazzi.snapshot {
      ClerkMaterialTheme {
        Box(modifier = Modifier.background(ClerkMaterialTheme.colors.background)) {
          OrganizationSwitcherSheetContent(
            state =
              OrganizationSwitcherState(
                memberships = sampleMemberships,
                membershipsTotalCount = sampleMemberships.size,
              ),
            memberships = sampleMemberships.toImmutableList(),
            activeOrganizationId = "org_acme",
            actions =
              OrganizationSwitcherSheetActions(onLoadMore = {}, onSelect = {}, onErrorShown = {}),
          )
        }
      }
    }
  }

  @Test
  fun organizationSwitcherSheetContentLoadMore() {
    Clerk.customTheme = ClerkTheme(DefaultColors.light)
    paparazzi.snapshot {
      ClerkMaterialTheme {
        Box(modifier = Modifier.background(ClerkMaterialTheme.colors.background)) {
          OrganizationSwitcherSheetContent(
            state =
              OrganizationSwitcherState(
                memberships = sampleMemberships,
                membershipsTotalCount = sampleMemberships.size + 5,
              ),
            memberships = sampleMemberships.toImmutableList(),
            activeOrganizationId = "org_acme",
            actions =
              OrganizationSwitcherSheetActions(onLoadMore = {}, onSelect = {}, onErrorShown = {}),
          )
        }
      }
    }
  }

  @Test
  fun organizationSwitcherSheetContentEmptyLoading() {
    Clerk.customTheme = ClerkTheme(DefaultColors.light)
    paparazzi.snapshot {
      ClerkMaterialTheme {
        Box(modifier = Modifier.background(ClerkMaterialTheme.colors.background)) {
          OrganizationSwitcherSheetContent(
            state = OrganizationSwitcherState(isLoading = true),
            memberships =
              emptyList<com.clerk.api.organizations.OrganizationMembership>().toImmutableList(),
            activeOrganizationId = null,
            actions =
              OrganizationSwitcherSheetActions(onLoadMore = {}, onSelect = {}, onErrorShown = {}),
          )
        }
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
  }
}
