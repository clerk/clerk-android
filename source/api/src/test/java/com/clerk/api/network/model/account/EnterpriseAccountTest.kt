package com.clerk.api.network.model.account

import com.clerk.api.network.ClerkApi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class EnterpriseAccountTest {

  @Test
  fun `decodes an enterprise connection without a logo`() {
    val account = decode(logoPublicUrl = "null")

    assertEquals("Acme OIDC", account.enterpriseConnection.name)
    assertNull(account.enterpriseConnection.logoPublicUrl)
  }

  @Test
  fun `decodes an enterprise connection logo`() {
    val account = decode(logoPublicUrl = "\"https://img.clerk.com/logo.png\"")

    assertEquals("https://img.clerk.com/logo.png", account.enterpriseConnection.logoPublicUrl)
  }

  private fun decode(logoPublicUrl: String): EnterpriseAccount =
    ClerkApi.json.decodeFromString(
      """
      {
        "id": "eac_123",
        "object": "enterprise_account",
        "protocol": "oauth",
        "provider": "oauth_custom_acme",
        "active": true,
        "email_address": "sam@acme.com",
        "first_name": "Sam",
        "last_name": null,
        "provider_user_id": "user_123",
        "last_authenticated_at": null,
        "public_metadata": {},
        "verification": null,
        "enterprise_connection_id": "ent_123",
        "enterprise_connection": {
          "id": "oauthcfg_123",
          "enterprise_connection_id": "ent_123",
          "protocol": "oauth",
          "provider": "oauth_custom_acme",
          "name": "Acme OIDC",
          "logo_public_url": $logoPublicUrl,
          "domain": "acme.com",
          "domains": ["acme.com"],
          "active": true,
          "sync_user_attributes": true,
          "disable_additional_identifications": false,
          "created_at": 1700000000000,
          "updated_at": 1700000000000,
          "allow_subdomains": false,
          "allow_idp_initiated": false
        }
      }
      """
    )
}
