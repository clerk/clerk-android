package com.clerk.api.sdk

import com.clerk.api.network.model.environment.UserSettings
import com.clerk.api.sso.OAuthProvider
import com.clerk.api.toOAuthProvidersList
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ToOAuthProvidersListTest {

  private fun socialConfig(
    strategy: String,
    enabled: Boolean = true,
    authenticatable: Boolean = true,
  ) =
    UserSettings.SocialConfig(
      enabled = enabled,
      required = false,
      authenticatable = authenticatable,
      strategy = strategy,
      notSelectable = false,
      name = strategy.removePrefix("oauth_"),
      logoUrl = null,
    )

  @Test
  fun `returns only enabled and authenticatable providers`() {
    val providers =
      mapOf(
        "oauth_google" to socialConfig("oauth_google", enabled = true, authenticatable = true),
        "oauth_apple" to socialConfig("oauth_apple", enabled = false, authenticatable = true),
        "oauth_facebook" to socialConfig("oauth_facebook", enabled = true, authenticatable = false),
        "oauth_github" to socialConfig("oauth_github", enabled = false, authenticatable = false),
      )

    val result = providers.toOAuthProvidersList()

    assertEquals(1, result.size)
    assertEquals(OAuthProvider.GOOGLE, result[0])
  }

  @Test
  fun `returns empty list when no providers are enabled and authenticatable`() {
    val providers =
      mapOf(
        "oauth_google" to socialConfig("oauth_google", enabled = false, authenticatable = true),
        "oauth_apple" to socialConfig("oauth_apple", enabled = true, authenticatable = false),
      )

    val result = providers.toOAuthProvidersList()

    assertTrue(result.isEmpty())
  }

  @Test
  fun `returns empty list for empty map`() {
    val providers = emptyMap<String, UserSettings.SocialConfig>()

    val result = providers.toOAuthProvidersList()

    assertTrue(result.isEmpty())
  }

  @Test
  fun `returns all providers when all are enabled and authenticatable`() {
    val providers =
      mapOf(
        "oauth_google" to socialConfig("oauth_google"),
        "oauth_apple" to socialConfig("oauth_apple"),
      )

    val result = providers.toOAuthProvidersList()

    assertEquals(2, result.size)
    assertTrue(result.contains(OAuthProvider.GOOGLE))
    assertTrue(result.contains(OAuthProvider.APPLE))
  }
}
