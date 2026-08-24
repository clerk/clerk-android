package com.clerk.snapshot.userprofile

import com.clerk.api.Clerk
import com.clerk.api.ui.ClerkTheme
import com.clerk.base.BaseSnapshotTest
import com.clerk.ui.theme.DefaultColors
import com.clerk.ui.userprofile.security.biometriccredential.UserProfileBiometricCredentialsSectionImpl
import org.junit.Test

class UserProfileBiometricCredentialsSectionSnapshotTest : BaseSnapshotTest() {

  @Test
  fun biometricCredentialsSectionEnabled_Light() {
    paparazzi.snapshot {
      UserProfileBiometricCredentialsSectionImpl(
        isEnabled = true,
        isLoading = false,
        onCheckedChange = {},
      )
    }
  }

  @Test
  fun biometricCredentialsSectionDisabled_Light() {
    paparazzi.snapshot {
      UserProfileBiometricCredentialsSectionImpl(
        isEnabled = false,
        isLoading = false,
        onCheckedChange = {},
      )
    }
  }

  @Test
  fun biometricCredentialsSectionEnabled_Dark() {
    val previousTheme = Clerk.customTheme
    try {
      Clerk.customTheme = ClerkTheme(colors = DefaultColors.dark)
      paparazzi.snapshot {
        UserProfileBiometricCredentialsSectionImpl(
          isEnabled = true,
          isLoading = false,
          onCheckedChange = {},
        )
      }
    } finally {
      Clerk.customTheme = previousTheme
    }
  }
}
