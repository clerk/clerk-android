package com.clerk.ui.userprofile.account

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class UserProfileDismissButtonTest {

  @Test
  fun `dismissible profile shows the close button`() {
    assertTrue(shouldShowUserProfileCloseButton(isDismissible = true))
  }

  @Test
  fun `non dismissible profile hides the close button`() {
    assertFalse(shouldShowUserProfileCloseButton(isDismissible = false))
  }
}
