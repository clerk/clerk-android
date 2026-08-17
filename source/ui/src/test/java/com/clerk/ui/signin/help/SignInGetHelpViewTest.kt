package com.clerk.ui.signin.help

import android.content.Intent
import org.junit.Assert.assertEquals
import org.junit.Test

class SignInGetHelpViewTest {

  @Test
  fun `support email intent addresses configured recipient`() {
    val intent = supportEmailIntent("help@example.com")

    assertEquals(Intent.ACTION_SENDTO, intent.action)
    assertEquals("mailto", intent.data?.scheme)
    assertEquals("help@example.com", intent.data?.schemeSpecificPart)
  }
}
