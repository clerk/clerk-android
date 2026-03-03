package com.clerk.api.sso

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RedirectConfigurationTest {
  @Test
  fun `default email-link redirect uses proxy port when configured`() {
    assertEquals(
      "clerk://com.clerk.workbench.oauth:8443",
      RedirectConfiguration.emailLinkRedirectUrl(
        applicationId = "com.clerk.workbench",
        proxyUrl = "https://rapid-earwig-10.clerk.accounts.lclclerk.com:8443",
      ),
    )
  }

  @Test
  fun `default email-link redirect omits standard https port`() {
    assertEquals(
      "clerk://com.clerk.workbench.oauth",
      RedirectConfiguration.emailLinkRedirectUrl(
        applicationId = "com.clerk.workbench",
        proxyUrl = "https://rapid-earwig-10.clerk.accounts.lclclerk.com:443",
      ),
    )
  }
}
