package com.clerk.api.protect

import com.clerk.api.network.ClerkApi
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class ProtectCheckResourceTest {

  @Test
  fun `resource preserves the complete opaque payload`() {
    val payload =
      """
      {
        "status": "pending",
        "token": "challenge-token",
        "sdk_url": "https://example.com/protect.js",
        "expires_at": 1720000000000,
        "ui_hints": {"theme": "dark"},
        "future_field": {"nested": [1, true, null]}
      }
      """
        .trimIndent()

    val resource = ClerkApi.json.decodeFromString<ProtectCheckResource>(payload)
    val encoded = ClerkApi.json.encodeToString(ProtectCheckResource.serializer(), resource)

    assertEquals(ClerkApi.json.parseToJsonElement(payload).jsonObject, resource.raw)
    assertEquals(
      ClerkApi.json.parseToJsonElement(payload),
      ClerkApi.json.parseToJsonElement(encoded),
    )
  }

  @Test
  fun `resource has structural equality`() {
    val first =
      ClerkApi.json.decodeFromString<ProtectCheckResource>(
        """{"status":"pending","token":"token"}"""
      )
    val second =
      ClerkApi.json.decodeFromString<ProtectCheckResource>(
        """{"status":"pending","token":"token"}"""
      )

    assertEquals(first, second)
    assertEquals(first.hashCode(), second.hashCode())
  }

  @Test
  fun `resource redacts its payload from string output`() {
    val resource =
      ClerkApi.json.decodeFromString<ProtectCheckResource>(
        """{"token":"sensitive-token","sdk_url":"https://example.com/protect.js"}"""
      )

    assertEquals("ProtectCheckResource([REDACTED])", resource.toString())
    assertFalse(resource.toString().contains("sensitive-token"))
    assertFalse(resource.toString().contains("protect.js"))
  }
}
