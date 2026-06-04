package com.clerk.ui.userprofile.account

import com.clerk.ui.userprofile.UserProfileDismissButtonStyle
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class UserProfileDismissButtonStyleTest {

  @Test
  fun `back style shows the back button`() {
    assertTrue(
      shouldShowUserProfileBackButton(
        isDismissible = true,
        dismissButtonStyle = UserProfileDismissButtonStyle.Back,
      )
    )
    assertFalse(
      shouldShowUserProfileCloseButton(
        isDismissible = true,
        dismissButtonStyle = UserProfileDismissButtonStyle.Back,
      )
    )
  }

  @Test
  fun `close style shows the close button`() {
    assertTrue(
      shouldShowUserProfileCloseButton(
        isDismissible = true,
        dismissButtonStyle = UserProfileDismissButtonStyle.Close,
      )
    )
    assertFalse(
      shouldShowUserProfileBackButton(
        isDismissible = true,
        dismissButtonStyle = UserProfileDismissButtonStyle.Close,
      )
    )
  }

  @Test
  fun `non dismissible profile hides both dismiss buttons`() {
    assertFalse(
      shouldShowUserProfileBackButton(
        isDismissible = false,
        dismissButtonStyle = UserProfileDismissButtonStyle.Back,
      )
    )
    assertFalse(
      shouldShowUserProfileCloseButton(
        isDismissible = false,
        dismissButtonStyle = UserProfileDismissButtonStyle.Close,
      )
    )
  }
}
