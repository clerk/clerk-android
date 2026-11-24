package com.clerk.api.signup

import com.clerk.api.network.ClerkApi
import kotlin.test.Test
import kotlin.test.assertEquals

class SignUpAbandonAtTest {

  @Test
  fun `abandonedAt holds the abandon_at value the Frontend API sends`() {
    val signUp = ClerkApi.json.decodeFromString(SignUp.serializer(), NEW_SIGN_UP_RESPONSE)

    assertEquals(1_700_000_000_000, signUp.abandonedAt)
  }

  private companion object {
    val NEW_SIGN_UP_RESPONSE =
      """
      {
        "object": "sign_up_attempt",
        "id": "sua_FIXTURE",
        "status": "missing_requirements",
        "required_fields": ["email_address", "phone_number", "username", "password"],
        "optional_fields": ["first_name", "last_name", "oauth_github", "oauth_google"],
        "missing_fields": [],
        "unverified_fields": ["phone_number", "email_address"],
        "verifications": {
          "email_address": null,
          "phone_number": null,
          "web3_wallet": null,
          "external_account": null
        },
        "username": "fixtureuser",
        "email_address": "fixture+clerk_test@example.com",
        "phone_number": "+14155550100",
        "web3_wallet": null,
        "password_enabled": true,
        "first_name": null,
        "last_name": null,
        "unsafe_metadata": {},
        "public_metadata": {},
        "custom_action": false,
        "external_id": null,
        "created_session_id": null,
        "created_user_id": null,
        "abandon_at": 1700000000000,
        "legal_accepted_at": null,
        "locale": null,
        "timezone": "Etc/UTC"
      }
      """
        .trimIndent()
  }
}
