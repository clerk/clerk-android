package com.clerk.ui.organizationprofile

import kotlin.test.Test
import kotlin.test.assertEquals

class OrganizationProfileViewDismissibleTest {

  @Test
  fun `root back dismisses when organization profile is dismissible`() {
    val events = mutableListOf<String>()

    handleOrganizationProfileBack(
      isAtRoot = true,
      isDismissible = true,
      onDismiss = { events += "dismiss" },
      onNavigateBack = { events += "navigate-back" },
    )

    assertEquals(listOf("dismiss"), events)
  }

  @Test
  fun `root back does nothing when organization profile is not dismissible`() {
    val events = mutableListOf<String>()

    handleOrganizationProfileBack(
      isAtRoot = true,
      isDismissible = false,
      onDismiss = { events += "dismiss" },
      onNavigateBack = { events += "navigate-back" },
    )

    assertEquals(emptyList<String>(), events)
  }

  @Test
  fun `nested back navigates back even when organization profile is not dismissible`() {
    val events = mutableListOf<String>()

    handleOrganizationProfileBack(
      isAtRoot = false,
      isDismissible = false,
      onDismiss = { events += "dismiss" },
      onNavigateBack = { events += "navigate-back" },
    )

    assertEquals(listOf("navigate-back"), events)
  }
}
