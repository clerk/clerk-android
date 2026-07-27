package com.clerk.api.configuration

import com.clerk.api.hostedauth.HostedAuthService
import com.clerk.api.sso.SSOService
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkAll
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.After
import org.junit.Before
import org.junit.Test

class ConfigurationManagerAuthRaceTest {
  @Before
  fun setUp() {
    mockkObject(SSOService)
    mockkObject(HostedAuthService)
    every { SSOService.hasPendingAuthentication() } returns false
    every { SSOService.hasPendingExternalAccountConnection() } returns false
    every { HostedAuthService.hasPendingAuthentication() } returns false
  }

  @After
  fun tearDown() {
    unmockkAll()
  }

  @Test
  fun `hasPendingAuthFlow returns true when SSO authentication is pending`() {
    every { SSOService.hasPendingAuthentication() } returns true

    val configurationManager = ConfigurationManager()

    assertTrue(configurationManager.hasPendingAuthFlow())
  }

  @Test
  fun `hasPendingAuthFlow returns true when external account connection is pending`() {
    every { SSOService.hasPendingExternalAccountConnection() } returns true

    val configurationManager = ConfigurationManager()

    assertTrue(configurationManager.hasPendingAuthFlow())
  }

  @Test
  fun `hasPendingAuthFlow returns true when hosted auth is pending`() {
    every { HostedAuthService.hasPendingAuthentication() } returns true

    val configurationManager = ConfigurationManager()

    assertTrue(configurationManager.hasPendingAuthFlow())
  }

  @Test
  fun `hasPendingAuthFlow returns false when no auth flow is pending`() {
    val configurationManager = ConfigurationManager()

    assertFalse(configurationManager.hasPendingAuthFlow())
  }
}
