package com.clerk.snapshot.footer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import com.clerk.base.BaseSnapshotTest
import com.clerk.ui.core.dimens.dp8
import com.clerk.ui.core.footer.ClerkFooterView
import com.clerk.ui.theme.ClerkMaterialTheme
import org.junit.Test

class ClerkFooterSnapshotTest : BaseSnapshotTest() {

  @Test
  fun footerShowsDevelopmentModeNoticeWhenEnabled() {
    paparazzi.snapshot {
      ClerkMaterialTheme {
        Box(modifier = Modifier.background(ClerkMaterialTheme.colors.background).padding(dp8)) {
          ClerkFooterView(showDevModeWarning = true, isBranded = true)
        }
      }
    }
  }

  @Test
  fun footerHidesDevelopmentModeNoticeWhenHelperReturnsFalse() {
    paparazzi.snapshot {
      ClerkMaterialTheme {
        Box(modifier = Modifier.background(ClerkMaterialTheme.colors.background).padding(dp8)) {
          ClerkFooterView(showDevModeWarning = false, isBranded = true)
        }
      }
    }
  }
}
