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
  fun `shouldShowUserButton shows when session exists and pending sessions are allowed`() {
    assertTrue(
      shouldShowUserButton(
        hasSession = true,
        hasActiveUser = false,
        treatPendingAsSignedOut = false,
      )
    )
  }

  @Test
  fun `shouldShowUserButton hides when no session exists and pending sessions are allowed`() {
    assertFalse(
      shouldShowUserButton(
        hasSession = false,
        hasActiveUser = true,
        treatPendingAsSignedOut = false,
      )
    )
  }

  @Test
  fun `shouldShowUserButton shows when active user exists and pending is treated as signed out`() {
    assertTrue(
      shouldShowUserButton(hasSession = false, hasActiveUser = true, treatPendingAsSignedOut = true)
    )
  }

  @Test
  fun `shouldShowUserButton hides when only session user exists and pending is treated as signed out`() {
    assertFalse(
      shouldShowUserButton(hasSession = true, hasActiveUser = false, treatPendingAsSignedOut = true)
    )
  }
}
