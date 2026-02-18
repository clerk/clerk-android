package com.clerk.ui.auth

import com.clerk.api.session.Session
import com.clerk.api.session.SessionTask
import com.clerk.api.signin.SignIn
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthStateForceMfaTest {

  @Test
  fun `requiresForcedMfaStep is true when sign in complete and session requires forced mfa`() {
    val signIn = SignIn(id = "sign_in_123", status = SignIn.Status.COMPLETE)
    val session =
      session(status = Session.SessionStatus.PENDING, tasks = listOf(SessionTask("mfa_required")))

    assertTrue(signIn.requiresForcedMfaStep(session))
  }

  @Test
  fun `requiresForcedMfaStep is false when sign in not complete`() {
    val signIn = SignIn(id = "sign_in_123", status = SignIn.Status.NEEDS_SECOND_FACTOR)
    val session =
      session(status = Session.SessionStatus.PENDING, tasks = listOf(SessionTask("mfa_required")))

    assertFalse(signIn.requiresForcedMfaStep(session))
  }

  @Test
  fun `requiresForcedMfaStep is false when session is pending without mfa task`() {
    val signIn = SignIn(id = "sign_in_123", status = SignIn.Status.COMPLETE)
    val session =
      session(
        status = Session.SessionStatus.PENDING,
        tasks = listOf(SessionTask("choose-organization")),
      )

    assertFalse(signIn.requiresForcedMfaStep(session))
  }

  @Test
  fun `requiresForcedMfaStep is false when session is active`() {
    val signIn = SignIn(id = "sign_in_123", status = SignIn.Status.COMPLETE)
    val session =
      session(status = Session.SessionStatus.ACTIVE, tasks = listOf(SessionTask("mfa_required")))

    assertFalse(signIn.requiresForcedMfaStep(session))
  }

  private fun session(status: Session.SessionStatus, tasks: List<SessionTask>): Session {
    return Session(
      id = "sess_123",
      status = status,
      expireAt = 0L,
      lastActiveAt = 0L,
      createdAt = 0L,
      updatedAt = 0L,
      tasks = tasks,
    )
  }
}
