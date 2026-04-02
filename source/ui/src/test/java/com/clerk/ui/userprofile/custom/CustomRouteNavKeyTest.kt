package com.clerk.ui.userprofile.custom

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.serialization.json.Json

class CustomRouteNavKeyTest {

  @Test
  fun `serialization roundtrip preserves route key`() {
    val original = CustomRouteNavKey(routeKey = "billing")
    val json = Json.encodeToString(CustomRouteNavKey.serializer(), original)
    val restored = Json.decodeFromString(CustomRouteNavKey.serializer(), json)

    assertEquals(original, restored)
  }

  @Test
  fun `deserialization restores the correct route key`() {
    val key = CustomRouteNavKey(routeKey = "preferences")
    val json = Json.encodeToString(CustomRouteNavKey.serializer(), key)
    val decoded = Json.decodeFromString(CustomRouteNavKey.serializer(), json)

    assertEquals("preferences", decoded.routeKey)
  }
}
