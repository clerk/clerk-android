package com.clerk.api.forceupdate

import com.clerk.api.network.model.environment.Environment
import com.clerk.api.network.model.environment.ForceUpdate
import io.mockk.every
import io.mockk.mockk
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ForceUpdateStatusResolverTest {
  @Test
  fun `outdated version is unsupported`() {
    val status =
      ForceUpdateStatusResolver.fromEnvironment(
        environment = environmentWithPolicy("2.0.0", "https://play.google.com/store/apps/details?id=com.example.app"),
        packageName = "com.example.app",
        currentVersion = "1.9.9",
      )

    assertFalse(status.isSupported)
    assertEquals("2.0.0", status.minimumVersion)
  }

  @Test
  fun `invalid current version fails open`() {
    val status =
      ForceUpdateStatusResolver.fromEnvironment(
        environment = environmentWithPolicy("2.0.0"),
        packageName = "com.example.app",
        currentVersion = "2.0.0-beta",
    )

    assertTrue(status.isSupported)
  }

  @Test
  fun `unknown package fails open`() {
    val status =
      ForceUpdateStatusResolver.fromEnvironment(
        environment = environmentWithPolicy("2.0.0"),
        packageName = "com.unknown.app",
        currentVersion = "1.0.0",
    )

    assertTrue(status.isSupported)
  }

  @Test
  fun `unsupported version meta maps to unsupported status`() {
    val status =
      ForceUpdateStatusResolver.fromUnsupportedVersionMeta(
        meta =
          buildJsonObject {
            put("platform", "android")
            put("app_identifier", "com.example.app")
            put("current_version", "1.0.0")
            put("minimum_version", "2.0.0")
            put("update_url", "https://play.google.com/store/apps/details?id=com.example.app")
          },
        packageName = "com.example.app",
      )

    assertFalse(status?.isSupported ?: true)
    assertEquals("2.0.0", status?.minimumVersion)
  }

  @Test
  fun `unsupported version meta ignores mismatched package`() {
    val status =
      ForceUpdateStatusResolver.fromUnsupportedVersionMeta(
        meta =
          buildJsonObject {
            put("platform", "android")
            put("app_identifier", "com.other.app")
          },
        packageName = "com.example.app",
      )

    assertNull(status)
  }

  private fun environmentWithPolicy(minimumVersion: String, updateUrl: String? = null): Environment {
    val environment = mockk<Environment>()
    every { environment.forceUpdate } returns
      ForceUpdate(
        androidPolicies =
          listOf(
            ForceUpdate.AndroidPolicy(
              packageName = "com.example.app",
              minimumVersion = minimumVersion,
              updateUrl = updateUrl,
            )
          ),
      )
    return environment
  }
}
