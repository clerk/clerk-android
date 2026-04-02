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

  @Test
  fun `effectiveCustomRows with empty input returns empty regardless of destination`() {
    val withDest = effectiveCustomRows(emptyList(), hasDestination = true)
    val withoutDest = effectiveCustomRows(emptyList(), hasDestination = false)

    assertTrue(withDest.isEmpty())
    assertTrue(withoutDest.isEmpty())
  }

  @Test
  fun `gated rows produce no custom entries in rendered output`() {
    val gated = effectiveCustomRows(sampleRows, hasDestination = false)
    val rows =
      buildRenderedRows(
        builtInRows = listOf(UserProfileRow.ManageAccount, UserProfileRow.Security),
        section = UserProfileSection.Profile,
        customRows = gated,
      )

    assertTrue(rows.all { it is UserProfileListRow.BuiltIn })
  }
}
