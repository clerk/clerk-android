package com.clerk.ui.auth

import androidx.navigation3.runtime.NavBackStack
import com.clerk.api.session.Session
import com.clerk.api.session.SessionTask
import com.clerk.api.signin.SignIn
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AuthStateSessionTaskRoutingTest {

  private lateinit var preferences: InMemorySharedPreferences

  @Before
  fun setUp() {
    preferences = InMemorySharedPreferences()
  }

  @Test
  fun `complete sign in routes to reset password session task before auth completion`() {
    val authState = createAuthState()
    val signIn =
      SignIn(id = "sign_in_123", status = SignIn.Status.COMPLETE, createdSessionId = "sess_123")
    val session =
      Session(
        id = "sess_123",
        status = Session.SessionStatus.PENDING,
        expireAt = 0L,
        lastActiveAt = 0L,
        createdAt = 0L,
        updatedAt = 0L,
        currentTask = SessionTask("reset-password"),
      )
    var authCompleted = false

    authState.setToStepForStatus(signIn, session) { authCompleted = true }

    assertEquals(AuthDestination.SessionTaskResetPassword, authState.backStack.last())
    assertFalse(authCompleted)
  }

  @Test
  fun `complete sign in routes to choose organization session task before auth completion`() {
    val authState = createAuthState()
    val signIn =
      SignIn(id = "sign_in_123", status = SignIn.Status.COMPLETE, createdSessionId = "sess_123")
    val session =
      Session(
        id = "sess_123",
        status = Session.SessionStatus.PENDING,
        expireAt = 0L,
        lastActiveAt = 0L,
        createdAt = 0L,
        updatedAt = 0L,
        currentTask = SessionTask("choose-organization"),
      )
    var authCompleted = false

    authState.setToStepForStatus(signIn, session) { authCompleted = true }

    assertEquals(AuthDestination.SessionTaskChooseOrganization, authState.backStack.last())
    assertFalse(authCompleted)
  }

  @Test
  fun `finishing a session task replaces it with the next pending task`() {
    val authState = createAuthState()
    authState.backStack.add(AuthDestination.SessionTaskMfa)
    val session =
      Session(
        id = "sess_123",
        status = Session.SessionStatus.PENDING,
        expireAt = 0L,
        lastActiveAt = 0L,
        createdAt = 0L,
        updatedAt = 0L,
        currentTask = SessionTask("reset-password"),
      )
    var authCompleted = false

    authState.handleSessionTaskCompletion(session) { authCompleted = true }

    assertEquals(
      listOf(AuthDestination.AuthStart, AuthDestination.SessionTaskResetPassword),
      authState.backStack.toList(),
    )
    assertFalse(authCompleted)
  }

  @Test
  fun `finishing the last session task completes auth without offering enrollment`() {
    val authState = createAuthState()
    authState.backStack.add(AuthDestination.SessionTaskMfa)
    var authCompleted = false

    authState.handleSessionTaskCompletion(session = null) { authCompleted = true }

    assertTrue(authCompleted)
    assertEquals(AuthDestination.SessionTaskMfa, authState.backStack.last())
  }

  @Test
  fun `showing a session task does not push it twice`() {
    val authState = createAuthState()

    repeat(2) {
      authState.navigate(AuthNavigationCommand.ShowSessionTask(AuthDestination.SessionTaskMfa)) {}
    }

    assertEquals(
      listOf(AuthDestination.AuthStart, AuthDestination.SessionTaskMfa),
      authState.backStack.toList(),
    )
  }

  private fun createAuthState(): AuthState {
    return AuthState(
      backStack = NavBackStack(AuthDestination.AuthStart),
      sharedPreferences = preferences,
    )
  }
}
