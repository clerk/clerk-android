package com.clerk.api.network.model.environment

import com.clerk.api.network.ClerkApi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class UserSettingsSerializationTest {

  @Test
  fun `testAttributesConfig_deserialize_withImmutable_present`() {
    val immutableTrue = decodeUserSettings(immutable = "true")
    val immutableFalse = decodeUserSettings(immutable = "false")

    assertEquals(true, immutableTrue.attributes.getValue("email_address").immutable)
    assertEquals(false, immutableFalse.attributes.getValue("email_address").immutable)
  }

  @Test
  fun `testAttributesConfig_deserialize_withoutImmutable`() {
    val userSettings = decodeUserSettings(immutable = null)

    assertNull(userSettings.attributes.getValue("email_address").immutable)
  }

  @Test
  fun `testAttributesConfig_other_fields_still_deserialize_whenImmutable_changes`() {
    val immutableTrue = decodeUserSettings(immutable = "true").attributes.getValue("email_address")
    val immutableFalse =
      decodeUserSettings(immutable = "false").attributes.getValue("email_address")
    val immutableMissing = decodeUserSettings(immutable = null).attributes.getValue("email_address")

    assertTrue(immutableTrue.enabled)
    assertTrue(immutableTrue.required)
    assertFalse(immutableTrue.usedForSecondFactor)
    assertEquals(listOf("email_code"), immutableTrue.firstFactors)
    assertEquals(listOf("email_code"), immutableTrue.verifications)

    assertTrue(immutableFalse.enabled)
    assertTrue(immutableMissing.enabled)
    assertEquals(listOf("email_code"), immutableFalse.firstFactors)
    assertEquals(listOf("email_code"), immutableMissing.firstFactors)
  }

  private fun decodeUserSettings(immutable: String?): UserSettings {
    val immutableField = immutable?.let { """, "immutable": $it""" }.orEmpty()
    val json =
      """
      {
        "attributes": {
          "email_address": {
            "enabled": true,
            "required": true$immutableField,
            "used_for_first_factor": true,
            "first_factors": ["email_code"],
            "used_for_second_factor": false,
            "second_factors": [],
            "verifications": ["email_code"],
            "verify_at_sign_up": true
          }
        },
        "sign_up": {
          "custom_action_required": false,
          "progressive": false,
          "mode": "public",
          "legal_consent_enabled": false
        },
        "social": {},
        "actions": {
          "delete_self": false,
          "create_organization": false
        },
        "passkey_settings": null
      }
      """.trimIndent()
    return ClerkApi.json.decodeFromString<UserSettings>(json)
  }
}
