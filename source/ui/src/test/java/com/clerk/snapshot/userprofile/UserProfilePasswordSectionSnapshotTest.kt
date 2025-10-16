package com.clerk.snapshot.userprofile

import com.clerk.api.Clerk
import com.clerk.api.ui.ClerkTheme
import com.clerk.base.BaseSnapshotTest
import com.clerk.ui.theme.DefaultColors
import com.clerk.ui.userprofile.security.password.UserProfilePasswordSectionImpl
import org.junit.Test

class UserProfilePasswordSectionSnapshotTest : BaseSnapshotTest() {

  @Test
  fun passwordSection_Light() {
    paparazzi.snapshot { UserProfilePasswordSectionImpl(isPasswordEnabled = true) {} }
  }

  @Test
  fun passwordSectionAddPassword_Light() {
    paparazzi.snapshot { UserProfilePasswordSectionImpl(isPasswordEnabled = false) {} }
  }

  @Test
  fun passwordSection_Dark() {
    Clerk.customTheme = ClerkTheme(colors = DefaultColors.dark)
    paparazzi.snapshot { UserProfilePasswordSectionImpl(isPasswordEnabled = true) {} }
  }

  @Test
  fun passwordSectionAddPassword_Dark() {
    Clerk.customTheme = ClerkTheme(colors = DefaultColors.dark)
    paparazzi.snapshot { UserProfilePasswordSectionImpl(isPasswordEnabled = false) {} }
  }
}
