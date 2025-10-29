package com.clerk.snapshot.userprofile

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.clerk.api.Clerk
import com.clerk.api.ui.ClerkTheme
import com.clerk.base.BaseSnapshotTest
import com.clerk.ui.theme.ClerkMaterialTheme
import com.clerk.ui.theme.DefaultColors
import com.clerk.ui.userprofile.totp.UserProfileMfaAddTotpView
import org.junit.Test

class UserProfileMfaAddTotpSnapshotTest : BaseSnapshotTest() {

  @Test
  fun userProfileMfaAddTotpViewLightMode() {
    paparazzi.snapshot {
      Box(modifier = Modifier.height(700.dp)) { ClerkMaterialTheme { UserProfileMfaAddTotpView() } }
    }
  }

  @Test
  fun userProfileMfaAddTotpViewDarkMode() {
    Clerk.customTheme = ClerkTheme(colors = DefaultColors.dark)
    paparazzi.snapshot {
      Box(modifier = Modifier.height(700.dp)) { ClerkMaterialTheme { UserProfileMfaAddTotpView() } }
    }
  }
}
