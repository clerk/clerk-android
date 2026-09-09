package com.clerk.snapshot.userprofile

import com.clerk.base.BaseSnapshotTest
import com.clerk.ui.theme.ClerkTheme
import com.clerk.ui.theme.DefaultColors
import com.clerk.ui.userprofile.security.biometriccredential.UserProfileBiometricCredentialsSectionImpl
import org.junit.Test

class UserProfileBiometricCredentialsSectionSnapshotTest : BaseSnapshotTest() {

  @Test
  fun biometricCredentialsSectionEnabled_Light() {
    snapshot {
      UserProfileBiometricCredentialsSectionImpl(
        isEnabled = true,
        isLoading = false,
        onCheckedChange = {},
      )
    }
  }

  @Test
  fun biometricCredentialsSectionDisabled_Light() {
    snapshot {
      UserProfileBiometricCredentialsSectionImpl(
        isEnabled = false,
        isLoading = false,
        onCheckedChange = {},
      )
    }
  }

  @Test
  fun biometricCredentialsSectionEnabled_Dark() {
    val previousTheme = snapshotTheme
    try {
      snapshotTheme = ClerkTheme(colors = DefaultColors.dark)
      snapshot {
        UserProfileBiometricCredentialsSectionImpl(
          isEnabled = true,
          isLoading = false,
          onCheckedChange = {},
        )
      }
    } finally {
      snapshotTheme = previousTheme
    }
  }
}
