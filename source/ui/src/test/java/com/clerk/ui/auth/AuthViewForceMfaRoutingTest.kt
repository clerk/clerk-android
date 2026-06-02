package com.clerk.ui.auth

import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import com.clerk.api.network.model.factor.Factor
import com.clerk.api.session.SessionTaskKey
import com.clerk.ui.core.common.StrategyKeys
import org.junit.Assert.assertEquals
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

  @Test
  fun `routes to reset password session task when task is pending`() {
    val shouldRoute =
      shouldRouteToPendingSessionTask(
        taskKey = SessionTaskKey.RESET_PASSWORD,
        top = AuthDestination.AuthStart,
      )

    assertTrue(shouldRoute)
  }

  @Test
  fun `routes to choose organization session task when task is pending`() {
    val shouldRoute =
      shouldRouteToPendingSessionTask(
        taskKey = SessionTaskKey.CHOOSE_ORGANIZATION,
        top = AuthDestination.AuthStart,
      )

    assertTrue(shouldRoute)
  }

  @Test
  fun `does not reroute when already on reset password session task destination`() {
    val shouldRoute =
      shouldRouteToPendingSessionTask(
        taskKey = SessionTaskKey.RESET_PASSWORD,
        top = AuthDestination.SessionTaskResetPassword,
      )

    assertFalse(shouldRoute)
  }

  @Test
  fun `does not reroute from create organization while choose organization task is pending`() {
    val shouldRoute =
      shouldRouteToPendingSessionTask(
        taskKey = SessionTaskKey.CHOOSE_ORGANIZATION,
        top = AuthDestination.SessionTaskCreateOrganization(),
      )

    assertFalse(shouldRoute)
  }

  @Test
  fun `create organization is a session task destination`() {
    assertTrue(AuthDestination.SessionTaskCreateOrganization().isSessionTaskDestination())
  }

  @Test
  fun `regular auth route is not a session task destination`() {
    assertFalse(AuthDestination.SignInGetHelp.isSessionTaskDestination())
  }

  @Test
  fun `forgot password factor selection pushes the selected first factor`() {
    val passwordFactor = Factor(strategy = StrategyKeys.PASSWORD)
    val emailCodeFactor =
      Factor(
        strategy = StrategyKeys.EMAIL_CODE,
        emailAddressId = "email_123",
        safeIdentifier = "sam@clerk.dev",
      )
    val backStack = NavBackStack<NavKey>(AuthDestination.AuthStart)
    backStack.add(AuthDestination.SignInFactorOne(passwordFactor))
    backStack.add(AuthDestination.SignInForgotPassword)

    navigateToForgotPasswordFactor(backStack, emailCodeFactor)

    assertEquals(AuthDestination.SignInFactorOne(emailCodeFactor), backStack.last())
    assertEquals(AuthDestination.SignInForgotPassword, backStack[backStack.size - 2])
  }
}
