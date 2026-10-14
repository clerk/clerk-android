package com.clerk.api.configuration

import com.clerk.api.network.serialization.ClerkResult
import com.clerk.api.redirect.PendingRedirect
import com.clerk.api.redirect.RedirectCoordinator
import com.clerk.api.sso.OAuthResult
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.After
import org.junit.Test

class ConfigurationManagerAuthRaceTest {
  @After
  fun tearDown() {
    RedirectCoordinator.resetForTests()
  }

  @Test
  fun `hasPendingAuthFlow returns true when SSO authentication is pending`() {
    RedirectCoordinator.begin(sso())

    assertTrue(ConfigurationManager().hasPendingAuthFlow())
  }

  @Test
  fun `hasPendingAuthFlow returns true when external account connection is pending`() {
    RedirectCoordinator.begin(
      PendingRedirect.ExternalAccountConnection(expectedState = "s", externalAccountId = "eac")
    )

    assertTrue(ConfigurationManager().hasPendingAuthFlow())
  }

  @Test
  fun `hasPendingAuthFlow returns true when hosted auth is pending`() {
    RedirectCoordinator.begin(PendingRedirect.HostedAuth("myapp://cb", "s", "v"))

    assertTrue(ConfigurationManager().hasPendingAuthFlow())
  }

  @Test
  fun `hasPendingAuthFlow returns false once the pending flow finishes`() {
    val pending = sso()
    RedirectCoordinator.begin(pending)

    RedirectCoordinator.finish(pending, ClerkResult.success(OAuthResult()))

    assertFalse(ConfigurationManager().hasPendingAuthFlow())
  }

  @Test
  fun `hasPendingAuthFlow returns false when no auth flow is pending`() {
    assertFalse(ConfigurationManager().hasPendingAuthFlow())
  }

  private fun sso() =
    PendingRedirect.Sso(
      expectedState = "s",
      transferable = true,
      redirectFlow = PendingRedirect.RedirectFlow.SIGN_IN,
      signUp = null,
    )
}
