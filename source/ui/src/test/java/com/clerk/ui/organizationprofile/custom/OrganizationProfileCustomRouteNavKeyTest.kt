package com.clerk.ui.organizationprofile.custom

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.serialization.json.Json

class OrganizationProfileCustomRouteNavKeyTest {

  @Test
  fun `serialization roundtrip preserves route key`() {
    val original = OrganizationProfileCustomRouteNavKey(routeKey = "billing")
    val json = Json.encodeToString(OrganizationProfileCustomRouteNavKey.serializer(), original)
    val restored = Json.decodeFromString(OrganizationProfileCustomRouteNavKey.serializer(), json)

    assertEquals(original, restored)
  }
}
