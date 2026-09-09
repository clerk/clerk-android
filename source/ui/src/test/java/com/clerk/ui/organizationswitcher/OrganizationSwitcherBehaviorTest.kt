package com.clerk.ui.organizationswitcher

import com.clerk.api.OrganizationMembership
import com.clerk.api.Session
import com.clerk.testing.mockSession
import io.mockk.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class OrganizationSwitcherBehaviorTest {

  @Test
  fun `activeOrganizationMembership returns membership matching active organization id`() {
    val acme = membership(id = "mem_acme", organizationId = "org_acme", name = "Acme")
    val beta = membership(id = "mem_beta", organizationId = "org_beta", name = "Beta")

    val activeMembership =
      activeOrganizationMembership(
        user = null,
        session = session(activeOrganizationId = "org_beta"),
        loadedMemberships = listOf(acme, beta),
      )

    assertEquals(beta, activeMembership)
  }

  @Test
  fun `activeOrganizationMembership returns null when session has no active organization`() {
    assertNull(
      activeOrganizationMembership(
        user = null,
        session = session(activeOrganizationId = null),
        loadedMemberships = listOf(membership()),
      )
    )
  }

  @Test
  fun `shouldShowOrganizationSwitcher requires user session and enabled organizations`() {
    assertTrue(
      shouldShowOrganizationSwitcher(hasUser = true, hasSession = true, organizationsEnabled = true)
    )
    assertFalse(
      shouldShowOrganizationSwitcher(
        hasUser = false,
        hasSession = true,
        organizationsEnabled = true,
      )
    )
    assertFalse(
      shouldShowOrganizationSwitcher(
        hasUser = true,
        hasSession = false,
        organizationsEnabled = true,
      )
    )
    assertFalse(
      shouldShowOrganizationSwitcher(
        hasUser = true,
        hasSession = true,
        organizationsEnabled = false,
      )
    )
  }

  private fun session(activeOrganizationId: String?): Session {
    val session = mockSession()
    every { session.lastActiveOrganizationId } returns activeOrganizationId
    return session
  }

  private fun membership(
    id: String = "mem_123",
    organizationId: String = "org_123",
    name: String = "Acme",
  ): OrganizationMembership {
    val membership = mockk<OrganizationMembership>(relaxed = true)
    every { membership.id } returns id
    every { membership.organization.id } returns organizationId
    every { membership.organization.name } returns name
    return membership
  }
}
