package com.clerk.snapshot.userprofile

import com.clerk.api.Clerk
import com.clerk.api.ui.ClerkTheme
import com.clerk.base.BaseSnapshotTest
import com.clerk.ui.theme.DefaultColors
import com.clerk.ui.userprofile.security.trusteddevice.UserProfileTrustedDeviceSectionImpl
import org.junit.Test

class UserProfileTrustedDeviceSectionSnapshotTest : BaseSnapshotTest() {

  @Test
  fun trustedDeviceSectionEnabled_Light() {
    paparazzi.snapshot {
      UserProfileTrustedDeviceSectionImpl(isEnabled = true, isLoading = false, onCheckedChange = {})
    }
  }

  @Test
  fun trustedDeviceSectionDisabled_Light() {
    paparazzi.snapshot {
      UserProfileTrustedDeviceSectionImpl(
        isEnabled = false,
        isLoading = false,
        onCheckedChange = {},
      )
    }
  }

  @Test
  fun trustedDeviceSectionEnabled_Dark() {
    Clerk.customTheme = ClerkTheme(colors = DefaultColors.dark)
    paparazzi.snapshot {
      UserProfileTrustedDeviceSectionImpl(isEnabled = true, isLoading = false, onCheckedChange = {})
    }
  }
}
