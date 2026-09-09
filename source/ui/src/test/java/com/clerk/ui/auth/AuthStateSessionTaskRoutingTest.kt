package com.clerk.ui.auth

import androidx.navigation3.runtime.NavBackStack
import com.clerk.api.*
import com.clerk.testing.*
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class AuthStateSessionTaskRoutingTest {
  @Test
  fun completeSignInRoutesToResetPasswordBeforeAuthCompletion() = runTest {
    checkTask(SessionTaskKey.ResetPassword, AuthDestination.SessionTaskResetPassword)
  }

  @Test
  fun completeSignInRoutesToChooseOrganizationBeforeAuthCompletion() = runTest {
    checkTask(SessionTaskKey.ChooseOrganization, AuthDestination.SessionTaskChooseOrganization)
  }

  private suspend fun checkTask(
    task: SessionTaskKey,
    destination: androidx.navigation3.runtime.NavKey,
  ) {
    val clerk = mockClerk()
    val session = mockSession(status = SessionStatus.Pending, task = task)
    every { clerk.session } returns session
    every { clerk.sessions } returns listOf(session)
    val authState =
      AuthState(
        clerk,
        backStack = NavBackStack(AuthDestination.AuthStart),
        sharedPreferences = InMemorySharedPreferences(),
      )
    var completed = false
    assertTrue(authState.setToStepForStatus(mockSignIn()) { completed = true })
    assertEquals(destination, authState.backStack.last())
    assertFalse(completed)
  }
}
