package com.clerk.ui.core.scaffold

import com.clerk.api.session.Session
import com.clerk.api.user.User
import io.mockk.mockk
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SignedInUserButtonVisibilityTest {

  private val user = mockk<User>()

  @Test
  fun showsForAPendingSession() {
    assertTrue(
      shouldShowSignedInUserButton(
        showSignedInUserButton = true,
        user = user,
        session = session(Session.SessionStatus.PENDING),
      )
    )
  }

  @Test
  fun hidesForAnActiveSession() {
    assertFalse(
      shouldShowSignedInUserButton(
        showSignedInUserButton = true,
        user = user,
        session = session(Session.SessionStatus.ACTIVE),
      )
    )
  }

  @Test
  fun hidesWithoutASession() {
    assertFalse(
      shouldShowSignedInUserButton(showSignedInUserButton = true, user = user, session = null)
    )
  }

  @Test
  fun hidesWithoutAUser() {
    assertFalse(
      shouldShowSignedInUserButton(
        showSignedInUserButton = true,
        user = null,
        session = session(Session.SessionStatus.PENDING),
      )
    )
  }

  @Test
  fun hidesWhenTheScreenTurnsItOff() {
    assertFalse(
      shouldShowSignedInUserButton(
        showSignedInUserButton = false,
        user = user,
        session = session(Session.SessionStatus.PENDING),
      )
    )
  }

  private fun session(status: Session.SessionStatus) =
    Session(
      id = "sess_123",
      status = status,
      expireAt = 0L,
      lastActiveAt = 0L,
      createdAt = 0L,
      updatedAt = 0L,
    )
}
