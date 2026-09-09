package com.clerk.snapshot.userprofile

import androidx.compose.runtime.CompositionLocalProvider
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import com.clerk.base.BaseSnapshotTest
import com.clerk.ui.theme.ClerkTheme
import com.clerk.ui.theme.DefaultColors
import com.clerk.ui.userprofile.LocalUserProfileState
import com.clerk.ui.userprofile.UserProfileState
import com.clerk.ui.userprofile.security.password.UserProfilePasswordSectionImpl
import io.mockk.mockk
import org.junit.Test

class UserProfilePasswordSectionSnapshotTest : BaseSnapshotTest() {

  @Test
  fun passwordSection_Light() {
    snapshot {
      CompositionLocalProvider(
        LocalUserProfileState provides
          UserProfileState(backStack = mockk<NavBackStack<NavKey>>(relaxed = true))
      ) {
        UserProfilePasswordSectionImpl(onClick = {})
      }
    }
  }

  @Test
  fun passwordSectionAddPassword_Dark() {
    snapshotTheme = ClerkTheme(colors = DefaultColors.dark)
    snapshot {
      CompositionLocalProvider(
        LocalUserProfileState provides
          UserProfileState(backStack = mockk<NavBackStack<NavKey>>(relaxed = true))
      ) {
        UserProfilePasswordSectionImpl(onClick = {})
      }
    }
  }
}
