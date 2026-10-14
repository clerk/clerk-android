package com.clerk.api.auth.types

import com.clerk.api.network.ClerkApi
import com.clerk.api.network.model.client.Client
import com.clerk.api.network.model.factor.Factor
import com.clerk.api.network.model.verification.Verification
import com.clerk.api.sso.OAuthProvider
import kotlinx.serialization.builtins.ListSerializer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class StrategyTest {

  private val knownStrategies =
    mapOf(
      "phone_code" to Strategy.PhoneCode,
      "email_code" to Strategy.EmailCode,
      "email_link" to Strategy.EmailLink,
      "totp" to Strategy.Totp,
      "backup_code" to Strategy.BackupCode,
      "password" to Strategy.Password,
      "passkey" to Strategy.Passkey,
      "reset_password_email_code" to Strategy.ResetPasswordEmailCode,
      "reset_password_phone_code" to Strategy.ResetPasswordPhoneCode,
      "ticket" to Strategy.Ticket,
      "transfer" to Strategy.Transfer,
      "enterprise_sso" to Strategy.EnterpriseSso,
      "saml" to Strategy.Saml,
      "trusted_device" to Strategy.TrustedDevice,
      "google_one_tap" to Strategy.GoogleOneTap,
    )

  @Test
  fun `from maps every known wire value to its object`() {
    knownStrategies.forEach { (raw, expected) ->
      assertSame(raw, expected, Strategy.from(raw))
      assertEquals(raw, expected.value)
      assertEquals(raw, expected.toString())
    }
  }

  @Test
  fun `from maps oauth strategies to OAuth with their provider`() {
    val google = Strategy.from("oauth_google")
    assertTrue(google is Strategy.OAuth)
    assertEquals(OAuthProvider.GOOGLE, (google as Strategy.OAuth).provider)

    val custom = Strategy.from("oauth_custom_acme")
    assertTrue(custom is Strategy.OAuth)
    assertEquals("oauth_custom_acme", (custom as Strategy.OAuth).provider.strategy)
  }

  @Test
  fun `from maps oauth token strategies to OAuthToken`() {
    val token = Strategy.from("oauth_token_apple")
    assertTrue(token is Strategy.OAuthToken)
    assertEquals("oauth_token_apple", token.value)
  }

  @Test
  fun `from keeps unrecognized strategies as Unknown with the raw value`() {
    val unknown = Strategy.from("web3_metamask_signature")
    assertTrue(unknown is Strategy.Unknown)
    assertEquals("web3_metamask_signature", (unknown as Strategy.Unknown).raw)
    assertEquals(Strategy.from("web3_metamask_signature"), unknown)
    assertNotEquals(Strategy.EmailCode, unknown)
  }

  @Test
  fun `factor decodes any strategy and round-trips it unchanged`() {
    val json =
      """[{"strategy":"email_code","safe_identifier":"a@b.c"},{"strategy":"brand_new_strategy"}]"""
    val serializer = ListSerializer(Factor.serializer())

    val factors = ClerkApi.json.decodeFromString(serializer, json)

    assertEquals(Strategy.EmailCode, factors[0].strategyType)
    assertEquals(Strategy.from("brand_new_strategy"), factors[1].strategyType)
    assertTrue(factors[1].strategyType is Strategy.Unknown)
    assertEquals(
      factors,
      ClerkApi.json.decodeFromString(serializer, ClerkApi.json.encodeToString(serializer, factors)),
    )
  }

  @Test
  fun `strategy serializer round-trips raw strings`() {
    (knownStrategies.keys + listOf("oauth_github", "oauth_token_google", "unheard_of")).forEach {
      val strategy = Strategy.from(it)
      val encoded = ClerkApi.json.encodeToString(Strategy.serializer(), strategy)
      assertEquals("\"$it\"", encoded)
      assertEquals(strategy, ClerkApi.json.decodeFromString(Strategy.serializer(), encoded))
    }
  }

  @Test
  fun `typed accessors on verification and client mirror the raw fields`() {
    assertNull(Verification().strategyType)
    assertEquals(Strategy.EmailLink, Verification(strategy = "email_link").strategyType)
    assertNull(Client().lastAuthenticationStrategyType)
    assertEquals(
      Strategy.from("oauth_google"),
      Client(lastAuthenticationStrategy = "oauth_google").lastAuthenticationStrategyType,
    )
  }
}
