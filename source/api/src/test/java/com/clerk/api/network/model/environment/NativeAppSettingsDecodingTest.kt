package com.clerk.api.network.model.environment

import com.clerk.api.appversion.appVersionTestEnvironment
import com.clerk.api.network.ClerkApi
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Test

class NativeAppSettingsDecodingTest {
  @Test
  fun `reads the Android minimum versions and ignores other platforms`() {
    val environment =
      decode(
        """
        {
          "minimum_supported_version": {
            "ios": [{ "bundle_id": "com.example.ios", "minimum_version": "2.0", "update_url": "" }],
            "android": [
              {
                "package_name": "com.example.app",
                "minimum_version": "42",
                "update_url": "https://play.google.com/store/apps/details?id=com.example.app"
              }
            ]
          }
        }
        """
      )

    assertEquals(
      listOf(
        NativeAppSettings.AndroidPolicy(
          packageName = "com.example.app",
          minimumVersion = "42",
          updateUrl = "https://play.google.com/store/apps/details?id=com.example.app",
        )
      ),
      environment.nativeAppSettings.minimumSupportedVersion.android,
    )
  }

  @Test
  fun `skips an unreadable minimum version and keeps the others`() {
    val environment =
      decode(
        """
        {
          "minimum_supported_version": {
            "android": [
              { "minimum_version": "42" },
              "not a policy",
              { "package_name": "com.example.app", "minimum_version": "42" }
            ]
          }
        }
        """
      )

    assertEquals(
      listOf(NativeAppSettings.AndroidPolicy("com.example.app", "42")),
      environment.nativeAppSettings.minimumSupportedVersion.android,
    )
  }

  @Test
  fun `unreadable native app settings don't fail the environment`() {
    listOf(
        """"not settings"""",
        """{ "minimum_supported_version": [] }""",
        """{ "minimum_supported_version": { "android": {} } }""",
      )
      .forEach { settings ->
        val environment = decode(settings)

        assertEquals(settings, "Acme", environment.displayConfig.applicationName)
        assertEquals(settings, NativeAppSettings(), environment.nativeAppSettings)
      }
  }

  @Test
  fun `missing native app settings default to no minimum versions`() {
    val json = baseEnvironmentJson().filterKeys { it != "native_app_settings" }

    val environment =
      ClerkApi.json.decodeFromJsonElement(Environment.serializer(), JsonObject(json))

    assertEquals(NativeAppSettings(), environment.nativeAppSettings)
  }

  private fun decode(nativeAppSettings: String): Environment {
    val json =
      baseEnvironmentJson() +
        ("native_app_settings" to ClerkApi.json.parseToJsonElement(nativeAppSettings))
    return ClerkApi.json.decodeFromJsonElement(Environment.serializer(), JsonObject(json))
  }

  private fun baseEnvironmentJson(): Map<String, JsonElement> =
    ClerkApi.json
      .encodeToJsonElement(Environment.serializer(), appVersionTestEnvironment())
      .jsonObject
}
