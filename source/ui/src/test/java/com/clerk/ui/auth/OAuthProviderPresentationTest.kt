package com.clerk.ui.auth

import androidx.compose.material3.Text
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.clerk.api.*
import com.clerk.testing.mockClerk
import com.clerk.ui.core.composition.ClerkProvider
import com.clerk.ui.core.extensions.logoUrl
import com.clerk.ui.core.extensions.providerName
import com.clerk.ui.userprofile.unconnectedProviders
import io.mockk.*
import kotlinx.serialization.json.JsonPrimitive
import org.junit.After
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class OAuthProviderPresentationTest {
  @get:Rule val compose = createComposeRule()
  private val clerk = mockClerk()

  @After fun cleanup() = unmockkAll()

  @Test
  fun signInRequiresEnabledAndAuthenticatable() {
    configure(
      config("oauth_google"),
      config("oauth_apple", enabled = false),
      config("oauth_facebook", authenticatable = false),
      config("oauth_github", enabled = false, authenticatable = false),
    )
    assertEquals(listOf(OAuthProvider.Google), choices())
  }

  @Test
  fun signInHasNoChoicesWhenNoProviderQualifies() {
    configure(
      config("oauth_google", enabled = false),
      config("oauth_apple", authenticatable = false),
    )
    assertTrue(choices().isEmpty())
  }

  @Test
  fun emptyConfigurationHasNoChoices() {
    configure()
    assertTrue(choices().isEmpty())
    assertTrue(clerk.unconnectedProviders.isEmpty())
  }

  @Test
  fun allEligibleBuiltInProvidersArePreserved() {
    configure(config("oauth_google"), config("oauth_apple"))
    assertEquals(listOf(OAuthProvider.Google, OAuthProvider.Apple), choices())
  }

  @Test
  fun distinctCustomProvidersRetainOrderAndRawKeys() {
    configure(config("oauth_custom_patreon"), config("oauth_custom_line"))
    assertEquals(
      listOf(
        OAuthProvider.Unrecognized("custom_patreon"),
        OAuthProvider.Unrecognized("custom_line"),
      ),
      choices(),
    )
    assertNotEquals(choices()[0], choices()[1])
    assertNotEquals(OAuthProvider.Unrecognized("custom"), choices()[0])
  }

  @Test
  fun generatedProviderSerializationUsesCanonicalValues() {
    val runtime = clerk.context.requireRuntime()
    for ((value, provider) in
      listOf(
        "google" to OAuthProvider.Google,
        "custom_patreon" to OAuthProvider.Unrecognized("custom_patreon"),
      )) {
      assertEquals(JsonPrimitive(value), provider.toJson())
      assertEquals(provider, OAuthProvider.fromJson(JsonPrimitive(value), runtime))
    }
  }

  @Test
  fun generatedStrategySerializationPreservesCustomPrefix() {
    val value = "oauth_custom_patreon"
    val strategy = CreateExternalAccountParamsStrategy.Unrecognized(value)
    assertEquals(JsonPrimitive(value), strategy.toJson())
    assertEquals(
      strategy,
      CreateExternalAccountParamsStrategy.fromJson(
        JsonPrimitive(value),
        clerk.context.requireRuntime(),
      ),
    )
  }

  @Test
  fun connectedCustomProviderDoesNotHideAnotherOrRequireSignInEligibility() {
    configure(
      config("oauth_custom_patreon"),
      config("oauth_custom_line", authenticatable = false),
      config("oauth_google", enabled = false),
    )
    val account =
      mockk<ExternalAccount> {
        every { provider } returns OAuthProvider.Unrecognized("custom_patreon")
      }
    val user = mockk<User> { every { verifiedExternalAccounts } returns listOf(account) }
    every { clerk.user } returns user
    assertEquals(listOf(OAuthProvider.Unrecognized("custom_line")), clerk.unconnectedProviders)
  }

  @Test
  fun configuredDisplayNameAndLogoUseExactCustomStrategy() {
    configure(
      config("oauth_custom_line", name = "LINE", logo = "https://cdn.example.com/line.png"),
      config(
        "oauth_custom_patreon",
        name = "Patreon",
        logo = "https://cdn.example.com/patreon.png",
      ),
    )
    compose.setContent {
      ClerkProvider(clerk) {
        val patreon = OAuthProvider.Unrecognized("custom_patreon")
        val line = OAuthProvider.Unrecognized("custom_line")
        Text("${patreon.providerName}|${patreon.logoUrl}")
        Text("${line.providerName}|${line.logoUrl}")
      }
    }
    compose.onNodeWithText("Patreon|https://cdn.example.com/patreon.png").assertExists()
    compose.onNodeWithText("LINE|https://cdn.example.com/line.png").assertExists()
  }

  private fun choices() = AuthStartViewHelper(clerk).authenticatableSocialProviders

  private fun configure(vararg providers: OAuthProviderSettings) {
    every { clerk.environment.userSettings.social } returns
      providers.associateBy { it.strategy.rawValue }
  }

  private fun config(
    value: String,
    enabled: Boolean = true,
    authenticatable: Boolean = true,
    name: String = value,
    logo: String? = null,
  ) =
    mockk<OAuthProviderSettings> {
      every { this@mockk.enabled } returns enabled
      every { this@mockk.authenticatable } returns authenticatable
      every { strategy } returns OAuthStrategy.Unrecognized(value)
      every { this@mockk.name } returns name
      every { logoUrl } returns logo
    }
}
