package com.clerk.ui.userprofile

import kotlin.test.Test
import kotlin.test.assertEquals

class UserProfileViewDismissibleTest {

  @Test
  fun `root back dismisses when user profile is dismissible`() {
    val events = mutableListOf<String>()

    handleUserProfileBack(
      isAtRoot = true,
      isDismissible = true,
      onDismiss = { events += "dismiss" },
      onNavigateBack = { events += "navigate-back" },
    )

    assertEquals(listOf("dismiss"), events)
  }

  @Test
  fun `root back does nothing when user profile is not dismissible`() {
    val events = mutableListOf<String>()

    handleUserProfileBack(
      isAtRoot = true,
      isDismissible = false,
      onDismiss = { events += "dismiss" },
      onNavigateBack = { events += "navigate-back" },
    )

    assertEquals(emptyList<String>(), events)
  }

  @Test
  fun `nested back navigates back even when user profile is not dismissible`() {
    val events = mutableListOf<String>()

    handleUserProfileBack(
      isAtRoot = false,
      isDismissible = false,
      onDismiss = { events += "dismiss" },
      onNavigateBack = { events += "navigate-back" },
    )

    assertEquals(listOf("navigate-back"), events)
  }
}
