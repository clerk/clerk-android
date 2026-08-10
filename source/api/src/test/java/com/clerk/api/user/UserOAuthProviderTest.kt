package com.clerk.api.user

import com.clerk.api.Clerk
import com.clerk.api.externalaccount.ExternalAccount
import com.clerk.api.network.model.environment.Environment
import com.clerk.api.network.model.environment.UserSettings
import com.clerk.api.sso.OAuthProvider
import io.mockk.every
import io.mockk.mockk
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test

class UserOAuthProviderTest {

  private val previousEnvironment = Clerk.environment

  @After
  fun tearDown() {
    Clerk.environment = previousEnvironment
  }

  @Test
  fun `create external account params preserve custom strategy`() {
    val params =
      User.CreateExternalAccountParams(provider = OAuthProvider.custom("oauth_custom_patreon"))

    assertEquals("oauth_custom_patreon", params.toMap()["strategy"])
  }

  @Test
  fun `connecting one custom provider does not hide another`() {
    val patreon = socialConfig("oauth_custom_patreon", "Patreon")
    val line = socialConfig("oauth_custom_line", "LINE")
    Clerk.environment = environmentWith(patreon, line)

    val connectedAccount = mockk<ExternalAccount>()
    every { connectedAccount.provider } returns patreon.strategy.removePrefix("oauth_")
    val user = mockk<User>()
    every { user.verifiedExternalAccounts } returns listOf(connectedAccount)

    assertEquals(listOf(OAuthProvider.custom(line.strategy)), user.unconnectedProviders)
  }

  private fun environmentWith(vararg socialConfigs: UserSettings.SocialConfig): Environment {
    val environment = mockk<Environment>()
    val userSettings = mockk<UserSettings>()
    every { environment.userSettings } returns userSettings
    every { userSettings.social } returns socialConfigs.associateBy { it.strategy }
    return environment
  }

  private fun socialConfig(strategy: String, name: String) =
    UserSettings.SocialConfig(
      enabled = true,
      required = false,
      authenticatable = true,
      strategy = strategy,
      notSelectable = false,
      name = name,
    )
}
