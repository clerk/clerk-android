package com.clerk.ui.auth

import androidx.navigation3.runtime.NavKey
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthViewForceMfaRoutingTest {

  @Test
  fun `routes when session requires forced mfa and current destination is not session task`() {
    val shouldRoute =
      shouldRouteToSessionTaskMfa(requiresForcedMfa = true, top = AuthDestination.AuthStart)

    assertTrue(shouldRoute)
  }

  @Test
  fun `does not route when already on session task destination`() {
    val shouldRoute =
      shouldRouteToSessionTaskMfa(requiresForcedMfa = true, top = AuthDestination.SessionTaskMfa)

    assertFalse(shouldRoute)
  }

  @Test
  fun `does not route when session does not require forced mfa`() {
    val shouldRoute =
      shouldRouteToSessionTaskMfa(requiresForcedMfa = false, top = object : NavKey {})

    assertFalse(shouldRoute)
  }
}
