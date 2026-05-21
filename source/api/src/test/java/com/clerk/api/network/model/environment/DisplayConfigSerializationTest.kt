package com.clerk.api.network.model.environment

import com.clerk.api.network.ClerkApi
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DisplayConfigSerializationTest {

  @Test
  fun `decodes show dev mode warning from display config`() {
    val displayConfig =
      ClerkApi.json.decodeFromString<DisplayConfig>(
        displayConfigJson(showDevModeWarning = true, instanceEnvironmentType = "development")
      )

    assertTrue(displayConfig.showDevModeWarning)
  }

  @Test
  fun `defaults missing show dev mode warning to false`() {
    val displayConfig =
      ClerkApi.json.decodeFromString<DisplayConfig>(
        displayConfigJson(showDevModeWarning = null, instanceEnvironmentType = "development")
      )

    assertFalse(displayConfig.showDevModeWarning)
  }

  @Test
  fun `shows dev mode warning only for development instances`() {
    val developmentConfig =
      ClerkApi.json.decodeFromString<DisplayConfig>(
        displayConfigJson(showDevModeWarning = true, instanceEnvironmentType = "development")
      )
    val productionConfig =
      ClerkApi.json.decodeFromString<DisplayConfig>(
        displayConfigJson(showDevModeWarning = true, instanceEnvironmentType = "production")
      )
    val disabledConfig =
      ClerkApi.json.decodeFromString<DisplayConfig>(
        displayConfigJson(showDevModeWarning = false, instanceEnvironmentType = "development")
      )

    assertTrue(developmentConfig.shouldShowDevModeWarning)
    assertFalse(productionConfig.shouldShowDevModeWarning)
    assertFalse(disabledConfig.shouldShowDevModeWarning)
  }

  private fun displayConfigJson(
    showDevModeWarning: Boolean?,
    instanceEnvironmentType: String,
  ): String {
    val showDevModeWarningField =
      showDevModeWarning?.let { """, "show_devmode_warning": $it""" }.orEmpty()
    return """
      {
        "instance_environment_type": "$instanceEnvironmentType",
        "application_name": "Test App",
        "preferred_sign_in_strategy": "password",
        "branded": true,
        "logo_image_url": "https://example.com/logo.png",
        "home_url": "https://example.com",
        "privacy_policy_url": null,
        "terms_url": null,
        "google_one_tap_client_id": null$showDevModeWarningField
      }
      """
      .trimIndent()
  }
}
