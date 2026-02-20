package com.clerk.ui.userbutton

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class UserButtonBehaviorTest {

  @Test
  fun `userButtonClickAction routes to auth when session requires forced mfa`() {
    val action = userButtonClickAction(requiresForcedMfa = true, routeToAuthWhenForcedMfa = true)

    assertEquals(UserButtonClickAction.ROUTE_TO_AUTH, action)
  }

  @Test
  fun `userButtonClickAction opens profile when session has no outstanding tasks`() {
    val action = userButtonClickAction(requiresForcedMfa = false, routeToAuthWhenForcedMfa = true)

    assertEquals(UserButtonClickAction.OPEN_PROFILE, action)
  }

  @Test
  fun `userButtonClickAction opens profile when forced mfa routing is disabled`() {
    val action = userButtonClickAction(requiresForcedMfa = true, routeToAuthWhenForcedMfa = false)

    assertEquals(UserButtonClickAction.OPEN_PROFILE, action)
  }

  @Test
  fun `shouldShowUserButton uses session user when pending sessions are allowed`() {
    assertTrue(
      shouldShowUserButton(
        hasSessionUser = true,
        hasActiveUser = false,
        treatPendingAsSignedOut = false,
      )
    )
  }

  @Test
  fun `shouldShowUserButton hides when only session user exists and pending is treated as signed out`() {
    assertFalse(
      shouldShowUserButton(
        hasSessionUser = true,
        hasActiveUser = false,
        treatPendingAsSignedOut = true,
      )
    )
  }
}
