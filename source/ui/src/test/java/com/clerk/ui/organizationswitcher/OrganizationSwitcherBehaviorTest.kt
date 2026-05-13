package com.clerk.ui.organizationswitcher

import com.clerk.api.organizations.Organization
import com.clerk.api.organizations.OrganizationMembership
import com.clerk.api.session.Session
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.serialization.json.JsonNull

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
  fun `shouldShowOrganizationSwitcher requires user session and memberships`() {
    assertTrue(
      shouldShowOrganizationSwitcher(hasUser = true, hasSession = true, hasMemberships = true)
    )
    assertFalse(
      shouldShowOrganizationSwitcher(hasUser = false, hasSession = true, hasMemberships = true)
    )
    assertFalse(
      shouldShowOrganizationSwitcher(hasUser = true, hasSession = false, hasMemberships = true)
    )
    assertFalse(
      shouldShowOrganizationSwitcher(hasUser = true, hasSession = true, hasMemberships = false)
    )
  }

  @Test
  fun `organizationSwitcherMemberships places active organization first then sorts by name`() {
    val zed = membership(organizationId = "org_zed", name = "Zed")
    val acme = membership(organizationId = "org_acme", name = "Acme")
    val beta = membership(organizationId = "org_beta", name = "Beta")

    val sorted = organizationSwitcherMemberships(listOf(zed, acme, beta), "org_beta")

    assertEquals(listOf(beta, acme, zed), sorted)
  }

  private fun session(activeOrganizationId: String?): Session {
    return Session(
      id = "sess_123",
      expireAt = 1,
      lastActiveAt = 1,
      lastActiveOrganizationId = activeOrganizationId,
      createdAt = 1,
      updatedAt = 1,
    )
  }

  private fun membership(
    id: String = "mem_123",
    organizationId: String = "org_123",
    name: String = "Acme",
  ): OrganizationMembership {
    return OrganizationMembership(
      id = id,
      publicMetadata = JsonNull,
      role = "org:admin",
      roleName = "Admin",
      organization =
        Organization(
          id = organizationId,
          name = name,
          slug = null,
          imageUrl = "",
          maxAllowedMemberships = 0,
          adminDeleteEnabled = true,
          createdAt = 1,
          updatedAt = 1,
          publicMetadata = JsonNull,
        ),
      createdAt = 1,
      updatedAt = 1,
    )
  }
}
