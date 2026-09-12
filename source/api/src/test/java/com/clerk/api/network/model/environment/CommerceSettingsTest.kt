package com.clerk.api.network.model.environment

import com.clerk.api.network.ClerkApi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CommerceSettingsTest {

  @Test
  fun `decodes enabled user billing`() {
    val settings =
      ClerkApi.json.decodeFromString<CommerceSettings>(
        """
        {
          "billing": {
            "stripe_publishable_key": "pk_test_123",
            "user": { "enabled": true, "has_paid_plans": true },
            "organization": { "enabled": false, "has_paid_plans": false }
          }
        }
        """
          .trimIndent()
      )

    assertTrue(settings.billing.user.enabled)
    assertTrue(settings.billing.user.hasPaidPlans)
    assertFalse(settings.billing.organization.enabled)
    assertEquals("pk_test_123", settings.billing.stripePublishableKey)
  }

  @Test
  fun `decodes enabled organization billing`() {
    val settings =
      ClerkApi.json.decodeFromString<CommerceSettings>(
        """
        {
          "billing": {
            "stripe_publishable_key": "pk_live_abc",
            "user": { "enabled": false, "has_paid_plans": false },
            "organization": { "enabled": true, "has_paid_plans": true }
          }
        }
        """
          .trimIndent()
      )

    assertTrue(settings.billing.organization.enabled)
    assertTrue(settings.billing.organization.hasPaidPlans)
    assertFalse(settings.billing.user.enabled)
    assertEquals("pk_live_abc", settings.billing.stripePublishableKey)
  }

  @Test
  fun `decodes missing commerce settings key with defaults`() {
    val environment =
      ClerkApi.json.decodeFromString<Environment>(environmentJson(commerceSettings = null))

    assertFalse(environment.commerceSettings.billing.user.enabled)
    assertFalse(environment.commerceSettings.billing.user.hasPaidPlans)
    assertFalse(environment.commerceSettings.billing.organization.enabled)
    assertFalse(environment.commerceSettings.billing.organization.hasPaidPlans)
    assertNull(environment.commerceSettings.billing.stripePublishableKey)
  }

  @Test
  fun `decodes null stripe publishable key`() {
    val settings =
      ClerkApi.json.decodeFromString<CommerceSettings>(
        """
        {
          "billing": {
            "stripe_publishable_key": null,
            "user": { "enabled": true, "has_paid_plans": false },
            "organization": { "enabled": false, "has_paid_plans": false }
          }
        }
        """
          .trimIndent()
      )

    assertNull(settings.billing.stripePublishableKey)
    assertTrue(settings.billing.user.enabled)
  }

  @Test
  fun `ignores leftover unknown commerce settings keys`() {
    val settings =
      ClerkApi.json.decodeFromString<CommerceSettings>(
        """
        {
          "id": "commerce_settings_1",
          "object": "commerce_settings",
          "billing": {
            "stripe_publishable_key": "pk_test_leftover",
            "user": { "enabled": true, "has_paid_plans": false },
            "organization": { "enabled": true, "has_paid_plans": false },
            "future_flag": true
          }
        }
        """
          .trimIndent()
      )

    assertTrue(settings.billing.user.enabled)
    assertTrue(settings.billing.organization.enabled)
    assertEquals("pk_test_leftover", settings.billing.stripePublishableKey)
  }

  private fun environmentJson(commerceSettings: String?): String {
    val commerceJson = commerceSettings?.let { """, "commerce_settings": $it""" }.orEmpty()
    return """
      {
        "auth_config": { "single_session_mode": false },
        "display_config": {
          "application_name": "Test App",
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
        }
        $commerceJson
      }
    """
      .trimIndent()
  }
}
