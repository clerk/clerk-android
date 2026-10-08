package com.clerk.ui.auth

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.clerk.api.Clerk
import com.clerk.api.network.model.environment.UserSettings
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkAll
import kotlin.test.Test
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AuthViewBeforeClerkLoadsTest {

  @get:Rule val composeTestRule = createComposeRule()

  private val isInitialized = MutableStateFlow(false)

  @Before
  fun setUp() {
    mockkObject(Clerk)
    every { Clerk.isInitialized } returns isInitialized
    every { Clerk.applicationName } answers { "Acme Co".takeIf { isInitialized.value } }
    every { Clerk.enabledFirstFactorAttributes } answers
      {
        if (isInitialized.value) listOf("email_address") else emptyList()
      }
    every { Clerk.socialProviders } answers
      {
        if (isInitialized.value) mapOf(googleSocialConfig.strategy to googleSocialConfig)
        else emptyMap()
      }
  }

  @After
  fun tearDown() {
    unmockkAll()
  }

  @Test
  fun showsLoadedConfigurationWhenComposedBeforeClerkLoads() {
    composeTestRule.setContent {
      AuthView(persistIdentifiers = false, preferGoogleOneTap = false, isDismissible = false)
    }
    composeTestRule.onNodeWithText("Continue").assertExists()

    isInitialized.value = true

    composeTestRule.onNodeWithText("Continue to Acme Co").assertExists()
    composeTestRule.onNodeWithText("Enter your email").assertExists()
    composeTestRule.onNodeWithText("Continue with Google").assertExists()
  }

  private companion object {
    val googleSocialConfig =
      UserSettings.SocialConfig(
        enabled = true,
        required = false,
        authenticatable = true,
        strategy = "oauth_google",
        notSelectable = false,
        name = "Google",
      )
  }
}
