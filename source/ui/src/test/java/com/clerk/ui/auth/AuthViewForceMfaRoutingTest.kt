package com.clerk.ui.auth

import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import com.clerk.api.auth.types.Strategy
import com.clerk.api.network.model.factor.Factor
import com.clerk.api.session.Session
import com.clerk.api.session.SessionTask
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthViewForceMfaRoutingTest {

  @Test
  fun `routes to mfa session task when setup mfa is pending`() {
    assertEquals(
      AuthNavigationCommand.Push(AuthDestination.SessionTaskMfa),
      pendingTaskCommand(task = "setup-mfa", top = AuthDestination.AuthStart),
    )
  }

  @Test
  fun `does not route when already on session task destination`() {
    assertEquals(
      AuthNavigationCommand.None,
      pendingTaskCommand(task = "setup-mfa", top = AuthDestination.SessionTaskMfa),
    )
  }

  @Test
  fun `does not route when no task is pending`() {
    assertEquals(
      AuthNavigationCommand.None,
      authNavigationCommand(AuthRoutingInput.PendingSessionTask(null, object : NavKey {})),
    )
  }

  @Test
  fun `routes to reset password session task when task is pending`() {
    assertEquals(
      AuthNavigationCommand.Push(AuthDestination.SessionTaskResetPassword),
      pendingTaskCommand(task = "reset-password", top = AuthDestination.AuthStart),
    )
  }

  @Test
  fun `routes to choose organization session task when task is pending`() {
    assertEquals(
      AuthNavigationCommand.Push(AuthDestination.SessionTaskChooseOrganization),
      pendingTaskCommand(task = "choose-organization", top = AuthDestination.AuthStart),
    )
  }

  @Test
  fun `does not reroute when already on reset password session task destination`() {
    assertEquals(
      AuthNavigationCommand.None,
      pendingTaskCommand(task = "reset-password", top = AuthDestination.SessionTaskResetPassword),
    )
  }

  @Test
  fun `does not reroute from create organization while choose organization task is pending`() {
    assertEquals(
      AuthNavigationCommand.None,
      pendingTaskCommand(
        task = "choose-organization",
        top = AuthDestination.SessionTaskCreateOrganization(),
      ),
    )
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
    val passwordFactor = Factor(strategy = Strategy.Password.value)
    val emailCodeFactor =
      Factor(
        strategy = Strategy.EmailCode.value,
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

  private fun pendingTaskCommand(task: String, top: NavKey): AuthNavigationCommand =
    authNavigationCommand(
      AuthRoutingInput.PendingSessionTask(
        session =
          Session(
            id = "sess_123",
            status = Session.SessionStatus.PENDING,
            expireAt = 0L,
            lastActiveAt = 0L,
            createdAt = 0L,
            updatedAt = 0L,
            currentTask = SessionTask(task),
          ),
        top = top,
      )
    )
}
