package com.clerk.ui.organizationprofile.invite

import kotlin.test.Test
import kotlin.test.assertEquals

class OrganizationInviteMembersViewTest {

  @Test
  fun `parseInviteEmailAddresses splits common separators and removes duplicates`() {
    val addresses =
      parseInviteEmailAddresses(
        "one@example.com, two@example.com; three@example.com\none@example.com\tfour@example.com"
      )

    assertEquals(
      listOf("one@example.com", "two@example.com", "three@example.com", "four@example.com"),
      addresses,
    )
  }

  @Test
  fun `parseInviteEmailAddresses ignores invalid entries`() {
    val addresses =
      parseInviteEmailAddresses("valid@example.com missing-at-symbol example.com another@valid.dev")

    assertEquals(listOf("valid@example.com", "another@valid.dev"), addresses)
  }
}
