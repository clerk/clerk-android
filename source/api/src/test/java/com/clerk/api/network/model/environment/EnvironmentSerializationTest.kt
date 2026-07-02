package com.clerk.api.network.model.environment

import com.clerk.api.network.ClerkApi
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EnvironmentSerializationTest {

  @Test
  fun `feature flags default to disabled when omitted`() {
    val environment = ClerkApi.json.decodeFromString<Environment>(environmentJson())

    assertFalse(environment.featureFlags.androidTestFlag)
  }

  @Test
  fun `feature flags decode android test flag`() {
    val environment =
      ClerkApi.json.decodeFromString<Environment>(
        environmentJson(
          featureFlagsJson =
            """
            "feature_flags": {
              "android_test_flag": true
            },
            """
              .trimIndent()
        )
      )

    assertTrue(environment.featureFlags.androidTestFlag)
  }

  @Test
  fun `feature flags default missing fields to disabled`() {
    val environment =
      ClerkApi.json.decodeFromString<Environment>(
        environmentJson(
          featureFlagsJson =
            """
            "feature_flags": {},
            """
              .trimIndent()
        )
      )

    assertFalse(environment.featureFlags.androidTestFlag)
  }

  private fun environmentJson(featureFlagsJson: String = ""): String {
    return """
      {
        "auth_config": {
          "single_session_mode": false
        },
        "display_config": {
          "instance_environment_type": "development",
          "application_name": "Test App",
          "preferred_sign_in_strategy": "password",
          "branded": true,
          "logo_image_url": "https://example.com/logo.png",
          "home_url": "/",
          "privacy_policy_url": null,
          "terms_url": null,
          "google_one_tap_client_id": null
        },
        "user_settings": {
          "attributes": {},
          "sign_up": {
            "custom_action_required": false,
            "progressive": false,
            "mode": "public",
            "legal_consent_enabled": false
          },
          "social": {},
          "actions": {},
          "passkey_settings": null
        },
        $featureFlagsJson
        "organization_settings": {}
      }
    """
      .trimIndent()
  }
}
