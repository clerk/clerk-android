package com.clerk.api.network.model.environment

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EnvironmentPasskeyFirstFactorTest {

  private fun environment(passkeyEnabled: Boolean, usedForFirstFactor: Boolean): Environment {
    return Environment(
      authConfig = AuthConfig(singleSessionMode = false),
      displayConfig =
        DisplayConfig(
          applicationName = "Test App",
          branded = true,
          logoImageUrl = "https://example.com/logo.png",
          homeUrl = "/",
          privacyPolicyUrl = null,
          termsUrl = null,
          googleOneTapClientId = null,
        ),
      userSettings =
        UserSettings(
          attributes =
            mapOf(
              "passkey" to
                UserSettings.AttributesConfig(
                  enabled = passkeyEnabled,
                  required = false,
                  usedForFirstFactor = usedForFirstFactor,
                  firstFactors = if (usedForFirstFactor) listOf("passkey") else emptyList(),
                  usedForSecondFactor = false,
                  secondFactors = emptyList(),
                  verifications = listOf("passkey"),
                  verifyAtSignUp = false,
                )
            ),
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

  @Test
  fun `enabled when passkey is a first factor`() {
    val environment = environment(passkeyEnabled = true, usedForFirstFactor = true)

    assertTrue(environment.passkeyIsEnabled)
    assertTrue(environment.passkeyFirstFactorIsEnabled)
  }

  @Test
  fun `disabled when passkey is registration only`() {
    val environment = environment(passkeyEnabled = true, usedForFirstFactor = false)

    assertTrue(environment.passkeyIsEnabled)
    assertFalse(environment.passkeyFirstFactorIsEnabled)
  }

  @Test
  fun `disabled when passkey attribute is disabled`() {
    val environment = environment(passkeyEnabled = false, usedForFirstFactor = true)

    assertFalse(environment.passkeyIsEnabled)
    assertFalse(environment.passkeyFirstFactorIsEnabled)
  }

  @Test
  fun `disabled when passkey attribute is absent`() {
    val environment =
      environment(passkeyEnabled = true, usedForFirstFactor = true).let {
        it.copy(userSettings = it.userSettings.copy(attributes = emptyMap()))
      }

    assertFalse(environment.passkeyIsEnabled)
    assertFalse(environment.passkeyFirstFactorIsEnabled)
  }
}
