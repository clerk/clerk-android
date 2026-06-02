package com.clerk.ui.auth

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthViewDismissableTest {

  @Test
  fun `shows dismiss button when auth is dismissable and dismiss callback is provided`() {
    assertTrue(shouldShowAuthDismissButton(isDismissable = true, onDismiss = {}))
  }

  @Test
  fun `hides dismiss button when auth is not dismissable`() {
    assertFalse(shouldShowAuthDismissButton(isDismissable = false, onDismiss = {}))
  }

  @Test
  fun `hides dismiss button without dismiss callback`() {
    assertFalse(shouldShowAuthDismissButton(isDismissable = true, onDismiss = null))
  }
}
