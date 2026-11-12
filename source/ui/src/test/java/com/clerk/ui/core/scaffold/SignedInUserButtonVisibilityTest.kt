package com.clerk.ui.core.scaffold

import com.clerk.api.session.Session
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SignedInUserButtonVisibilityTest {

  @Test
  fun showsForAPendingSession() {
    assertTrue(
      shouldShowSignedInUserButton(
        showSignedInUserButton = true,
        sessionStatus = Session.SessionStatus.PENDING,
      )
    )
  }

  @Test
  fun hidesForAnActiveSession() {
    assertFalse(
      shouldShowSignedInUserButton(
        showSignedInUserButton = true,
        sessionStatus = Session.SessionStatus.ACTIVE,
      )
    )
  }

  @Test
  fun hidesWithoutASession() {
    assertFalse(shouldShowSignedInUserButton(showSignedInUserButton = true, sessionStatus = null))
  }

  @Test
  fun hidesWhenTheScreenTurnsItOff() {
    assertFalse(
      shouldShowSignedInUserButton(
        showSignedInUserButton = false,
        sessionStatus = Session.SessionStatus.PENDING,
      )
    )
  }
}
