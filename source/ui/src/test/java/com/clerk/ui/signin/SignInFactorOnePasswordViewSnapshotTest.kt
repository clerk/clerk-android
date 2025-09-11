package com.clerk.ui.signin

import com.clerk.api.Clerk
import com.clerk.api.ui.ClerkTheme
import com.clerk.base.BaseSnapshotTest
import com.clerk.ui.theme.DefaultColors
import org.junit.Test

class SignInFactorOnePasswordViewSnapshotTest : BaseSnapshotTest() {

  @Test
  fun testSignInFactorOnePasswordView() {
    paparazzi.snapshot {
      Clerk.customTheme = ClerkTheme(colors = DefaultColors.clerk)
      SignInFactorOnePasswordView(onContinue = {}, email = "sam@clerk.dev")
    }
  }
}
