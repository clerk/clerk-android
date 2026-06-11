package com.clerk.ui.auth

import androidx.navigation3.runtime.NavBackStack
import com.clerk.api.session.Session
import com.clerk.api.session.SessionTask
import com.clerk.api.signin.SignIn
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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

  private fun createAuthState(): AuthState {
    return AuthState(
      backStack = NavBackStack(AuthDestination.AuthStart),
      sharedPreferences = preferences,
    )
  }
}
