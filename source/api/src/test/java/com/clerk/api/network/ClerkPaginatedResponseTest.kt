package com.clerk.api.network

import com.clerk.api.organizations.Role
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

  @Test
  fun `decodes paginated organization roles`() {
    val response =
      ClerkApi.json.decodeFromString<ClerkPaginatedResponse<Role>>(
        """
        {
          "data": [
            {
              "id": "role_member",
              "key": "org:member",
              "name": "Member",
              "description": "Member",
              "permissions": [],
              "created_at": 1,
              "updated_at": 2
            }
          ],
          "total_count": 1,
          "has_role_set_migration": false
        }
        """
          .trimIndent()
      )

    assertEquals("org:member", response.data.single().key)
    assertEquals(1, response.totalCount)
    assertEquals(false, response.hasRoleSetMigration)
  }
}
