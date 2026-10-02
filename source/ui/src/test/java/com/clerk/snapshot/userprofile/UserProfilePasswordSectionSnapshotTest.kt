package com.clerk.snapshot.userprofile

import androidx.compose.runtime.CompositionLocalProvider
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import com.clerk.api.Clerk
import com.clerk.api.ui.ClerkTheme
import com.clerk.api.user.User
import com.clerk.base.BaseSnapshotTest
import com.clerk.ui.theme.DefaultColors
import com.clerk.ui.userprofile.LocalUserProfileState
import com.clerk.ui.userprofile.UserProfileState
import com.clerk.ui.userprofile.security.password.UserProfilePasswordSectionImpl
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.After
import org.junit.Test

class UserProfilePasswordSectionSnapshotTest : BaseSnapshotTest() {

  @After
  fun tearDownUser() {
    unmockkObject(Clerk)
  }

  @Test
  fun passwordSection_Light() {
    // A user with a password renders the masked password row and "Change password"; without this
    // the section silently falls back to the "Add password" state.
    mockkObject(Clerk)
    every { Clerk.userFlow } returns
      MutableStateFlow(mockk<User>(relaxed = true) { every { passwordEnabled } returns true })

    paparazzi.snapshot {
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
    Clerk.customTheme = ClerkTheme(colors = DefaultColors.dark)
    paparazzi.snapshot {
      CompositionLocalProvider(
        LocalUserProfileState provides
          UserProfileState(backStack = mockk<NavBackStack<NavKey>>(relaxed = true))
      ) {
        UserProfilePasswordSectionImpl(onClick = {})
      }
    }
  }
}
