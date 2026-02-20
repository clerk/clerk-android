package com.clerk.api.session

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionTaskTest {

  @Test
  fun `parsedKey maps mfa required keys`() {
    assertEquals(SessionTaskKey.MFA_REQUIRED, SessionTask("mfa_required").parsedKey)
    assertEquals(SessionTaskKey.MFA_REQUIRED, SessionTask("mfa-required").parsedKey)
    assertEquals(SessionTaskKey.MFA_REQUIRED, SessionTask("setup_mfa").parsedKey)
    assertEquals(SessionTaskKey.MFA_REQUIRED, SessionTask("setup-mfa").parsedKey)
  }

  @Test
  fun `requiresForcedMfa is true for pending session with mfa task`() {
    val session =
      session(status = Session.SessionStatus.PENDING, tasks = listOf(SessionTask("mfa_required")))

    assertTrue(session.requiresForcedMfa)
    assertTrue(session.hasMfaRequiredTask)
  }

  @Test
  fun `requiresForcedMfa is false for active session with mfa task`() {
    val session =
      session(status = Session.SessionStatus.ACTIVE, tasks = listOf(SessionTask("mfa_required")))

    assertFalse(session.requiresForcedMfa)
    assertTrue(session.hasMfaRequiredTask)
  }

  @Test
  fun `requiresForcedMfa is false when pending session has no mfa task`() {
    val session =
      session(
        status = Session.SessionStatus.PENDING,
        tasks = listOf(SessionTask("choose-organization")),
      )

    assertFalse(session.requiresForcedMfa)
    assertFalse(session.hasMfaRequiredTask)
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
