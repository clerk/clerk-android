package com.clerk.snapshot.userprofile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.ui.Modifier
import com.clerk.base.BaseSnapshotTest
import com.clerk.ui.R
import com.clerk.ui.core.dimens.dp1
import com.clerk.ui.theme.ClerkMaterialTheme
import com.clerk.ui.userprofile.custom.CustomRowView
import com.clerk.ui.userprofile.custom.UserProfileCustomRow
import com.clerk.ui.userprofile.custom.UserProfileRowIcon
import org.junit.Test

class CustomRowSnapshotTest : BaseSnapshotTest() {

  @Test
  fun customRowWithResourceIcon() {
    paparazzi.snapshot {
      ClerkMaterialTheme {
        Column(
          modifier = Modifier.fillMaxWidth().background(ClerkMaterialTheme.colors.background)
        ) {
          CustomRowView(
            customRow =
              UserProfileCustomRow(
                routeKey = "billing",
                title = "Billing",
                icon = UserProfileRowIcon.Resource(R.drawable.ic_user),
              ),
            onClick = {},
          )
          HorizontalDivider(thickness = dp1, color = ClerkMaterialTheme.computedColors.border)
          CustomRowView(
            customRow =
              UserProfileCustomRow(
                routeKey = "support",
                title = "Support",
                icon = UserProfileRowIcon.Resource(R.drawable.ic_lock),
              ),
            onClick = {},
          )
        }
      }
    }
  }

  @Test
  fun customRowWithVectorIcon() {
    paparazzi.snapshot {
      ClerkMaterialTheme {
        Column(
          modifier = Modifier.fillMaxWidth().background(ClerkMaterialTheme.colors.background)
        ) {
          CustomRowView(
            customRow =
              UserProfileCustomRow(
                routeKey = "favorites",
                title = "Favorites",
                icon = UserProfileRowIcon.Vector(Icons.Default.Favorite),
              ),
            onClick = {},
          )
          HorizontalDivider(thickness = dp1, color = ClerkMaterialTheme.computedColors.border)
          CustomRowView(
            customRow =
              UserProfileCustomRow(
                routeKey = "preferences",
                title = "Preferences",
                icon = UserProfileRowIcon.Vector(Icons.Default.Settings),
              ),
            onClick = {},
          )
        }
      }
    }
  }
}
