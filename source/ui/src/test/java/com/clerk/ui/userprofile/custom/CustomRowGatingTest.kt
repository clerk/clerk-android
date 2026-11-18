package com.clerk.ui.userprofile.custom

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CustomRowGatingTest {

  private val sampleRows =
    listOf(
      UserProfileCustomRow(
        routeKey = "billing",
        title = "Billing",
        icon = UserProfileRowIcon.Resource(0),
        placement = UserProfileCustomRowPlacement.After(UserProfileRow.Security),
      ),
      UserProfileCustomRow(
        routeKey = "prefs",
        title = "Preferences",
        icon = UserProfileRowIcon.Resource(0),
        placement = UserProfileCustomRowPlacement.Before(UserProfileRow.SignOut),
      ),
    )

  @Test
  fun `effectiveCustomRows returns rows when destination is provided`() {
    val result = effectiveCustomRows(sampleRows, hasDestination = true)

    assertEquals(sampleRows, result)
  }

  @Test
  fun `effectiveCustomRows returns empty list when destination is missing`() {
    val result = effectiveCustomRows(sampleRows, hasDestination = false)

    assertTrue(result.isEmpty())
  }
}
