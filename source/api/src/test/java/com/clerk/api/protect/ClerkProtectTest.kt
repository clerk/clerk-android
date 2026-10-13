package com.clerk.api.protect

import com.clerk.api.network.ClerkApi
import com.clerk.api.network.model.environment.AuthConfig
import com.clerk.api.network.model.environment.DisplayConfig
import com.clerk.api.network.model.environment.Environment
import com.clerk.api.network.model.environment.UserSettings
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ClerkProtectTest {

  @After
  fun tearDown() {
    ClerkProtect.reset()
  }

  @Test
  fun `environment configures Protect with native Specter origin`() {
    ClerkProtect.initialize(
      environment =
        environment(
          protectConfig =
            buildJsonObject {
              put(
                "native",
                buildJsonObject { put("specter_origin", "https://specter.example.com") },
              )
            }
        ),
      fapiOrigin = "https://fapi.example.com/path",
    )

    assertTrue(ClerkProtect.isEnabled)
  }

  @Test
  fun `environment without Protect config disables Protect`() {
    ClerkProtect.initialize(environment = environment(), fapiOrigin = "https://fapi.example.com")

    assertFalse(ClerkProtect.isEnabled)
  }

  @Test
  fun `resource converts recursively to the Protect check model`() {
    val resource =
      ClerkApi.json.decodeFromString<ProtectCheckResource>(
        """
        {
          "status": "pending",
          "token": " challenge-token ",
          "sdk_url": "https://specter.example.com/challenge",
          "expires_at": 1720000000000,
          "ui_hints": {"reason": "device_new", "locale": "en-US"},
          "runtime": "webview"
        }
        """
          .trimIndent()
      )

    val check = resource.toProtectCheck()

    assertEquals("pending", check.status)
    assertEquals("challenge-token", check.token)
    assertEquals("https://specter.example.com/challenge", check.sdkUrl.rawValue)
    assertEquals(1720000000000, check.expiresAtMillis)
    assertEquals(mapOf("reason" to "device_new", "locale" to "en-US"), check.uiHints)
    assertEquals("webview", check.runtime)
  }

  private fun environment(
    protectConfig: kotlinx.serialization.json.JsonObject? = null
  ): Environment {
    return Environment(
      authConfig = AuthConfig(singleSessionMode = false),
      displayConfig =
        DisplayConfig(
          applicationName = "Test App",
          branded = true,
          logoImageUrl = "https://example.com/logo.png",
          homeUrl = "/",
          privacyPolicyUrl = null,
          termsUrl = null,
          googleOneTapClientId = null,
        ),
      userSettings =
        UserSettings(
          attributes = emptyMap(),
          signUp =
            UserSettings.SignUpUserSettings(
              customActionRequired = false,
              progressive = false,
              mode = "public",
              legalConsentEnabled = false,
            ),
          social = emptyMap(),
          actions = UserSettings.Actions(),
          passkeySettings = null,
        ),
      protectConfig = protectConfig,
    )
  }
}
