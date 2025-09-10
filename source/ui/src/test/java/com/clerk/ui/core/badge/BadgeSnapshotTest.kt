package com.clerk.ui.core.badge

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.clerk.api.Clerk
import com.clerk.api.ui.ClerkTheme
import com.clerk.base.BaseSnapshotTest
import com.clerk.ui.core.common.dimens.dp12
import com.clerk.ui.core.common.dimens.dp8
import com.clerk.ui.theme.ClerkMaterialTheme
import com.clerk.ui.theme.DefaultColors
import org.junit.Test

class BadgeSnapshotTest : BaseSnapshotTest() {

  @Test
  fun badgeSnapshotTestLight() {
    paparazzi.snapshot {
      ClerkMaterialTheme {
        Column(
          modifier =
            Modifier.padding(dp8)
              .background(color = ClerkMaterialTheme.colors.background)
              .padding(dp8),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(dp12, alignment = Alignment.CenterVertically),
        ) {
          Badge(text = "Badge Label")
          Badge(text = "Badge Label", badgeType = ClerkBadgeType.Secondary)
          Badge(text = "Badge Label", badgeType = ClerkBadgeType.Positive)
          Badge(text = "Badge Label", badgeType = ClerkBadgeType.Negative)
          Badge(text = "Badge Label", badgeType = ClerkBadgeType.Warning)
        }
      }
    }
  }

  @Test
  fun badgeSnapshotTestDark() {
    Clerk.customTheme = ClerkTheme(DefaultColors.dark)
    paparazzi.snapshot {
      ClerkMaterialTheme {
        Column(
          modifier =
            Modifier.padding(dp8)
              .background(color = ClerkMaterialTheme.colors.background)
              .padding(dp8),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(dp12, alignment = Alignment.CenterVertically),
        ) {
          Badge(text = "Badge Label")
          Badge(text = "Badge Label", badgeType = ClerkBadgeType.Secondary)
          Badge(text = "Badge Label", badgeType = ClerkBadgeType.Positive)
          Badge(text = "Badge Label", badgeType = ClerkBadgeType.Negative)
          Badge(text = "Badge Label", badgeType = ClerkBadgeType.Warning)
        }
      }
    }
  }
}
