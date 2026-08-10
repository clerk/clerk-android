package com.clerk.api.sso

import com.clerk.api.Clerk
import com.clerk.api.network.ClerkApi
import com.clerk.api.network.model.environment.Environment
import com.clerk.api.network.model.environment.UserSettings
import io.mockk.every
import io.mockk.mockk
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class OAuthProviderTest {

  private val previousEnvironment = Clerk.environment

  @After
  fun tearDown() {
    Clerk.environment = previousEnvironment
  }

  @Test
  fun `fromStrategy preserves a custom OAuth strategy`() {
    val provider = OAuthProvider.fromStrategy("oauth_custom_patreon")

    assertEquals("oauth_custom_patreon", provider.strategy)
    assertEquals("CUSTOM", provider.name)
    assertNotEquals(OAuthProvider.CUSTOM, provider)
  }

  @Test
  fun `custom providers with different keys remain distinct`() {
    val patreon = OAuthProvider.custom("oauth_custom_patreon")
    val line = OAuthProvider.custom("oauth_custom_line")

    assertNotEquals(patreon, line)
  }

  @Test
  fun `custom provider reads configured name and logo by exact strategy`() {
    Clerk.environment = environmentWith(patreonConfig())
    val provider = OAuthProvider.fromStrategy("oauth_custom_patreon")

    assertEquals("Patreon", provider.providerName)
    assertEquals("https://cdn.example.com/patreon.png", provider.logoUrl)
  }

  @Test
  fun `serialization preserves built-in format and custom strategy`() {
    val builtIn = ClerkApi.json.encodeToString(OAuthProvider.serializer(), OAuthProvider.GOOGLE)
    val custom =
      ClerkApi.json.encodeToString(
        OAuthProvider.serializer(),
        OAuthProvider.custom("oauth_custom_patreon"),
      )

    assertEquals("\"GOOGLE\"", builtIn)
    assertEquals("\"oauth_custom_patreon\"", custom)
    assertEquals(
      OAuthProvider.GOOGLE,
      ClerkApi.json.decodeFromString(OAuthProvider.serializer(), builtIn),
    )
    assertEquals(
      OAuthProvider.custom("oauth_custom_patreon"),
      ClerkApi.json.decodeFromString(OAuthProvider.serializer(), custom),
    )
  }

  private fun environmentWith(vararg socialConfigs: UserSettings.SocialConfig): Environment {
    val environment = mockk<Environment>()
    val userSettings = mockk<UserSettings>()
    every { environment.userSettings } returns userSettings
    every { userSettings.social } returns socialConfigs.associateBy { it.strategy }
    return environment
  }

  private fun patreonConfig() =
    UserSettings.SocialConfig(
      enabled = true,
      required = false,
      authenticatable = true,
      strategy = "oauth_custom_patreon",
      notSelectable = false,
      name = "Patreon",
      logoUrl = "https://cdn.example.com/patreon.png",
    )
}
