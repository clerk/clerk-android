package com.clerk.ui.auth

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthViewDismissibleTest {

  @Test
  fun `shows dismiss button when auth is dismissible`() {
    assertTrue(shouldShowAuthDismissButton(isDismissible = true))
  }

  @Test
  fun `hides dismiss button when auth is not dismissible`() {
    assertFalse(shouldShowAuthDismissButton(isDismissible = false))
  }
}
