package com.clerk.api.configuration

import com.clerk.api.sso.SSOService
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkAll
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.After
import org.junit.Test

class ConfigurationManagerSsoRaceTest {
  @After
  fun tearDown() {
    unmockkAll()
  }

  @Test
  fun `hasPendingSsoFlow returns true when authentication is pending`() {
    mockkObject(SSOService)
    every { SSOService.hasPendingAuthentication() } returns true
    every { SSOService.hasPendingExternalAccountConnection() } returns false

    val configurationManager = ConfigurationManager()

    assertTrue(configurationManager.hasPendingSsoFlow())
  }

  @Test
  fun `hasPendingSsoFlow returns true when external account connection is pending`() {
    mockkObject(SSOService)
    every { SSOService.hasPendingAuthentication() } returns false
    every { SSOService.hasPendingExternalAccountConnection() } returns true

    val configurationManager = ConfigurationManager()

    assertTrue(configurationManager.hasPendingSsoFlow())
  }

  @Test
  fun `hasPendingSsoFlow returns false when no SSO flow is pending`() {
    mockkObject(SSOService)
    every { SSOService.hasPendingAuthentication() } returns false
    every { SSOService.hasPendingExternalAccountConnection() } returns false

    val configurationManager = ConfigurationManager()

    assertFalse(configurationManager.hasPendingSsoFlow())
  }
}
