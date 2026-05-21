package com.clerk.api.network.model.environment

import com.clerk.api.network.ClerkApi
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DisplayConfigSerializationTest {

  @Test
  fun `show dev mode warning deserializes correctly`() {
    val displayConfig = ClerkApi.json.decodeFromString<DisplayConfig>(displayConfigJson(true))

    assertTrue(displayConfig.showDevModeWarning)
  }

  @Test
  fun `show dev mode warning defaults to false`() {
    val displayConfig =
      ClerkApi.json.decodeFromString<DisplayConfig>(displayConfigJson(showDevModeWarning = null))

    assertFalse(displayConfig.showDevModeWarning)
  }

  private fun displayConfigJson(showDevModeWarning: Boolean?): String {
    val showDevModeWarningJson =
      showDevModeWarning?.let { """"show_devmode_warning":$it,""" }.orEmpty()

    return """
      {
        "instance_environment_type":"development",
        "application_name":"Test App",
        "preferred_sign_in_strategy":"password",
        $showDevModeWarningJson
        "branded":true,
        "logo_image_url":"https://example.com/logo.png",
        "home_url":"/",
        "privacy_policy_url":null,
        "terms_url":null,
        "google_one_tap_client_id":null
      }
    """
      .trimIndent()
  }
}
