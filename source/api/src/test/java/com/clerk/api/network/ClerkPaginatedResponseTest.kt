package com.clerk.api.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ClerkPaginatedResponseTest {

  @Test
  fun `decodes role set migration flag when present`() {
    val response =
      ClerkApi.json.decodeFromString<ClerkPaginatedResponse<String>>(
        """
        {
          "data": ["item"],
          "total_count": 1,
          "has_role_set_migration": true
        }
        """
          .trimIndent()
      )

    assertEquals(listOf("item"), response.data)
    assertEquals(1, response.totalCount)
    assertEquals(true, response.hasRoleSetMigration)
  }

  @Test
  fun `hasRoleSetMigration is null when omitted`() {
    val response =
      ClerkApi.json.decodeFromString<ClerkPaginatedResponse<String>>(
        """
        {
          "data": [],
          "total_count": 0
        }
        """
          .trimIndent()
      )

    assertEquals(emptyList<String>(), response.data)
    assertEquals(0, response.totalCount)
    assertNull(response.hasRoleSetMigration)
  }
}
