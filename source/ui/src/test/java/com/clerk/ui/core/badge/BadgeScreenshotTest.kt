package com.clerk.ui.core.badge

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.clerk.api.Clerk
import com.clerk.api.ui.ClerkTheme
import com.clerk.base.BaseScreenshotTest
import com.clerk.ui.core.dimens.dp12
import com.clerk.ui.core.dimens.dp8
import com.clerk.ui.theme.ClerkMaterialTheme
import com.clerk.ui.theme.DefaultColors
import org.junit.Test

class BadgeScreenshotTest : BaseScreenshotTest() {

  @Test
  fun badgeScreenShotTestLight() {
    paparazzi.snapshot {
      ClerkMaterialTheme {
        Column(
          modifier =
            Modifier.padding(dp8)
              .background(color = MaterialTheme.colorScheme.background)
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
  fun badgeScreenShotTestDark() {
    Clerk.customTheme = ClerkTheme(DefaultColors.dark)
    paparazzi.snapshot {
      ClerkMaterialTheme {
        Column(
          modifier =
            Modifier.padding(dp8)
              .background(color = MaterialTheme.colorScheme.background)
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
