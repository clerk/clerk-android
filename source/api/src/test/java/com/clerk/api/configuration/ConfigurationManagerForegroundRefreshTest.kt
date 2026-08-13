package com.clerk.api.configuration

import com.clerk.api.ClerkConfigurationOptions
import com.clerk.api.FrameworkIntegrationApi
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.Test

@OptIn(FrameworkIntegrationApi::class)
class ConfigurationManagerForegroundRefreshTest {
  @Test
  fun `foreground refresh runs when no options are provided`() {
    assertTrue(ConfigurationManager().shouldRefreshOnForeground(null))
  }

  @Test
  fun `foreground refresh runs by default`() {
    assertTrue(ConfigurationManager().shouldRefreshOnForeground(ClerkConfigurationOptions()))
  }

  @Test
  fun `foreground refresh is skipped when disabled via withForegroundRefreshDisabled`() {
    assertFalse(
      ConfigurationManager()
        .shouldRefreshOnForeground(ClerkConfigurationOptions().withForegroundRefreshDisabled())
    )
  }

  @Test
  fun `withCustomHeaders preserves a disabled foreground refresh`() {
    val options =
      ClerkConfigurationOptions()
        .withForegroundRefreshDisabled()
        .withCustomHeaders(mapOf("x-test" to "value"))

    assertFalse(ConfigurationManager().shouldRefreshOnForeground(options))
    assertTrue(options.customHeaders.containsKey("x-test"))
  }
}
