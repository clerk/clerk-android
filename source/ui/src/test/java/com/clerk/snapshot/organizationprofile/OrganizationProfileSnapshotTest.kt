package com.clerk.snapshot.organizationprofile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.clerk.base.BaseSnapshotTest
import com.clerk.ui.R
import com.clerk.ui.organizationprofile.custom.OrganizationProfileCustomRow
import com.clerk.ui.organizationprofile.custom.OrganizationProfileCustomRowPlacement
import com.clerk.ui.organizationprofile.custom.OrganizationProfileRow
import com.clerk.ui.organizationprofile.custom.OrganizationProfileRowIcon
import com.clerk.ui.organizationprofile.previewOrganizationProfileMembership
import com.clerk.ui.organizationprofile.previewOrganizationProfileOrganization
import com.clerk.ui.organizationprofile.root.OrganizationProfileRootView
import com.clerk.ui.theme.ClerkMaterialTheme
import kotlinx.collections.immutable.persistentListOf
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
}
