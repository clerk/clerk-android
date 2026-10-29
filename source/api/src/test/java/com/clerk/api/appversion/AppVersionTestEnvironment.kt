package com.clerk.api.appversion

import com.clerk.api.network.model.environment.AuthConfig
import com.clerk.api.network.model.environment.DisplayConfig
import com.clerk.api.network.model.environment.Environment
import com.clerk.api.network.model.environment.NativeAppSettings
import com.clerk.api.network.model.environment.UserSettings

internal const val TEST_PACKAGE_NAME = "com.example.app"
internal const val TEST_PLAY_URL = "https://play.google.com/store/apps/details?id=com.example.app"

internal fun androidPolicy(
  minimumVersion: String,
  packageName: String = TEST_PACKAGE_NAME,
  updateUrl: String? = TEST_PLAY_URL,
) = NativeAppSettings.AndroidPolicy(packageName, minimumVersion, updateUrl)

internal fun appVersionTestEnvironment(
  vararg policies: NativeAppSettings.AndroidPolicy,
  applicationName: String = "Acme",
): Environment =
  Environment(
    authConfig = AuthConfig(singleSessionMode = false),
    displayConfig =
      DisplayConfig(
        applicationName = applicationName,
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
    nativeAppSettings =
      NativeAppSettings(NativeAppSettings.MinimumSupportedVersion(android = policies.toList())),
  )
