package com.clerk.api.sdk

import com.clerk.api.Clerk
import com.clerk.api.network.model.environment.AuthConfig
import com.clerk.api.network.model.environment.DisplayConfig
import com.clerk.api.network.model.environment.Environment
import com.clerk.api.network.model.environment.InstanceEnvironmentType
import com.clerk.api.network.model.environment.UserSettings
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ClerkDevelopmentModeWarningTest {

  @Test
  fun `shouldShowDevelopmentModeWarning returns true for development with warning enabled`() {
    Clerk.updateEnvironment(
      testEnvironment(
        instanceEnvironmentType = InstanceEnvironmentType.DEVELOPMENT,
        showDevModeWarning = true,
      )
    )

    assertTrue(Clerk.shouldShowDevelopmentModeWarning)
  }

  @Test
  fun `shouldShowDevelopmentModeWarning returns false for production`() {
    Clerk.updateEnvironment(
      testEnvironment(
        instanceEnvironmentType = InstanceEnvironmentType.PRODUCTION,
        showDevModeWarning = true,
      )
    )

    assertFalse(Clerk.shouldShowDevelopmentModeWarning)
  }

  @Test
  fun `shouldShowDevelopmentModeWarning returns false when warning disabled`() {
    Clerk.updateEnvironment(
      testEnvironment(
        instanceEnvironmentType = InstanceEnvironmentType.DEVELOPMENT,
        showDevModeWarning = false,
      )
    )

    assertFalse(Clerk.shouldShowDevelopmentModeWarning)
  }

  private fun testEnvironment(
    instanceEnvironmentType: InstanceEnvironmentType,
    showDevModeWarning: Boolean,
  ): Environment {
    return Environment(
      authConfig = AuthConfig(singleSessionMode = false),
      displayConfig =
        DisplayConfig(
          instanceEnvironmentType = instanceEnvironmentType,
          applicationName = "Test App",
          showDevModeWarning = showDevModeWarning,
          branded = true,
          logoImageUrl = "https://example.com/logo.png",
          homeUrl = "/",
          privacyPolicyUrl = null,
          termsUrl = null,
          googleOneTapClientId = null,
        ),
      userSettings =
        UserSettings(
          attributes = emptyMap(),
          signUp =
            UserSettings.SignUpUserSettings(
              customActionRequired = false,
              progressive = false,
              mode = "public",
              legalConsentEnabled = false,
            ),
          social = emptyMap(),
          actions = UserSettings.Actions(),
          passkeySettings = null,
        ),
    )
  }
}
