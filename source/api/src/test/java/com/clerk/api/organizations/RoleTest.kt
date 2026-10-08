package com.clerk.api.organizations

import com.clerk.api.network.ClerkApi
import com.clerk.api.network.ClerkPaginatedResponse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RoleTest {

  @Test
  fun `decodes a roles page with null role and permission descriptions`() {
    val page =
      ClerkApi.json.decodeFromString<ClerkPaginatedResponse<Role>>(
        """
        {
          "data": [
            {
              "object": "role",
              "id": "role_1",
              "key": "org:member",
              "name": "Member",
              "description": "Default member role",
              "permissions": [],
              "is_creator_eligible": false,
              "created_at": 1700000000000,
              "updated_at": 1700000000000
            },
            {
              "object": "role",
              "id": "role_2",
              "key": "org:billing",
              "name": "Billing",
              "description": null,
              "permissions": [
                {
                  "object": "permission",
                  "id": "perm_1",
                  "key": "org:invoices:read",
                  "name": "Read invoices",
                  "description": null,
                  "type": "user",
                  "created_at": 1700000000000,
                  "updated_at": 1700000000000
                }
              ],
              "is_creator_eligible": false,
              "created_at": 1700000000000,
              "updated_at": 1700000000000
            }
          ],
          "total_count": 2
        }
        """
      )

    assertEquals(listOf("org:member", "org:billing"), page.data.map { it.key })
    assertEquals("Default member role", page.data[0].description)
    assertNull(page.data[1].description)
    assertNull(page.data[1].permissions.single().description)
  }
}
