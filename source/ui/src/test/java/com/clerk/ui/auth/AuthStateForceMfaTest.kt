package com.clerk.ui.auth

import com.clerk.api.session.Session
import com.clerk.api.session.SessionTask
import com.clerk.api.session.SessionTaskKey
import com.clerk.api.signin.SignIn
import com.clerk.api.signup.SignUp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AuthStateForceMfaTest {

  @Test
  fun `pendingSessionTaskKey returns MFA_REQUIRED when sign in completes with pending mfa task`() {
    val signIn = SignIn(id = "sign_in_123", status = SignIn.Status.COMPLETE)
    val session =
      session(status = Session.SessionStatus.PENDING, tasks = listOf(SessionTask("setup-mfa")))

    assertEquals(SessionTaskKey.MFA_REQUIRED, signIn.pendingSessionTaskKey(session))
  }

  @Test
  fun `pendingSessionTaskKey returns null when sign in is not complete`() {
    val signIn = SignIn(id = "sign_in_123", status = SignIn.Status.NEEDS_SECOND_FACTOR)
    val session =
      session(status = Session.SessionStatus.PENDING, tasks = listOf(SessionTask("mfa_required")))

    assertNull(signIn.pendingSessionTaskKey(session))
  }

  @Test
  fun `pendingSessionTaskKey returns UNKNOWN when session has unsupported task`() {
    val signIn = SignIn(id = "sign_in_123", status = SignIn.Status.COMPLETE)
    val session =
      session(
        status = Session.SessionStatus.PENDING,
        tasks = listOf(SessionTask("choose-organization")),
      )

    assertEquals(SessionTaskKey.UNKNOWN, signIn.pendingSessionTaskKey(session))
  }

  @Test
  fun `pendingSessionTaskKey returns null when session is active`() {
    val signIn = SignIn(id = "sign_in_123", status = SignIn.Status.COMPLETE)
    val session =
      session(status = Session.SessionStatus.ACTIVE, tasks = listOf(SessionTask("mfa_required")))

    assertNull(signIn.pendingSessionTaskKey(session))
  }

  @Test
  fun `pendingSessionTaskKey returns MFA_REQUIRED when sign up completes with pending mfa task`() {
    val signUp = signUp(status = SignUp.Status.COMPLETE)
    val session =
      session(status = Session.SessionStatus.PENDING, tasks = listOf(SessionTask("setup_mfa")))

    assertEquals(SessionTaskKey.MFA_REQUIRED, signUp.pendingSessionTaskKey(session))
  }

  @Test
  fun `postAuthCompletionAction routes to mfa when session is unresolved for created session id`() {
    val action = postAuthCompletionAction(taskKey = null, hasUnresolvedCreatedSession = true)

    assertEquals(PostAuthCompletionAction.ROUTE_TO_MFA, action)
  }

  @Test
  fun `postAuthCompletionAction completes auth when no task and no unresolved created session`() {
    val action = postAuthCompletionAction(taskKey = null, hasUnresolvedCreatedSession = false)

    assertEquals(PostAuthCompletionAction.COMPLETE_AUTH, action)
  }

  @Test
  fun `resolveCorrespondingSession does not fallback when created session id is present but missing`() {
    val fallback =
      session(id = "sess_fallback", status = Session.SessionStatus.ACTIVE, tasks = emptyList())
    val sessions =
      listOf(session(id = "sess_other", status = Session.SessionStatus.ACTIVE, tasks = emptyList()))

    val resolved =
      resolveCorrespondingSession(
        createdSessionId = "sess_target",
        sessions = sessions,
        fallbackSession = fallback,
      )

    assertNull(resolved)
  }

  private fun session(
    id: String = "sess_123",
    status: Session.SessionStatus,
    tasks: List<SessionTask>,
  ): Session {
    return Session(
      id = id,
      status = status,
      expireAt = 0L,
      lastActiveAt = 0L,
      createdAt = 0L,
      updatedAt = 0L,
      tasks = tasks,
    )
  }

  private fun signUp(status: SignUp.Status): SignUp {
    return SignUp(
      id = "sign_up_123",
      status = status,
      requiredFields = emptyList(),
      optionalFields = emptyList(),
      missingFields = emptyList(),
      unverifiedFields = emptyList(),
      verifications = emptyMap(),
      passwordEnabled = false,
      createdSessionId = "sess_123",
    )
  }
}
