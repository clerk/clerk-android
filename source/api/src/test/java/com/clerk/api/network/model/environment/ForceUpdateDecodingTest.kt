package com.clerk.api.network.model.environment

import com.clerk.api.network.ClerkApi
import kotlinx.serialization.decodeFromString
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ForceUpdateDecodingTest {
  @Test
  fun `missing android policies decode as empty list`() {
    val decoded =
      ClerkApi.json.decodeFromString<ForceUpdate>(
        """
        {
          "ios": [
            {
              "bundle_id": "com.example.app",
              "minimum_version": "2.0.0",
              "update_url": "https://apps.apple.com/app/id123"
            }
          ]
        }
        """
      )

    assertEquals(1, decoded.iosPolicies.size)
    assertTrue(decoded.androidPolicies.isEmpty())
  }

  @Test
  fun `missing ios policies decode as empty list`() {
    val decoded =
      ClerkApi.json.decodeFromString<ForceUpdate>(
        """
        {
          "android": [
            {
              "package_name": "com.example.app",
              "minimum_version": "2.0.0",
              "update_url": "https://play.google.com/store/apps/details?id=com.example.app"
            }
          ]
        }
        """
      )

    assertTrue(decoded.iosPolicies.isEmpty())
    assertEquals(1, decoded.androidPolicies.size)
  }

  @Test
  fun `empty force update object decodes to empty policy lists`() {
    val decoded =
      ClerkApi.json.decodeFromString<ForceUpdate>(
        """
        {}
        """
      )

    assertTrue(decoded.iosPolicies.isEmpty())
    assertTrue(decoded.androidPolicies.isEmpty())
  }
}
