package com.clerk.api.sdk

import com.clerk.api.Clerk
import com.clerk.api.network.model.client.Client
import com.clerk.api.organizations.Organization
import com.clerk.api.organizations.OrganizationMembership
import com.clerk.api.session.Session
import com.clerk.api.user.User
import kotlinx.serialization.json.JsonObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ClerkOrganizationTest {

  @After
  fun tearDown() {
    Clerk.updateClient(Client())
    Clerk.clearSessionAndUserState()
  }

  @Test
  fun `organizationMembership returns membership matching active organization id`() {
    val activeOrganization = organization(id = "org_active", name = "Acme")
    val otherOrganization = organization(id = "org_other", name = "Other")
    val activeMembership = membership(id = "orgmem_active", organization = activeOrganization)
    val otherMembership = membership(id = "orgmem_other", organization = otherOrganization)
    val session =
      session(
        lastActiveOrganizationId = activeOrganization.id,
        user = user(memberships = listOf(otherMembership, activeMembership)),
      )

    Clerk.updateClient(Client(sessions = listOf(session), lastActiveSessionId = session.id))

    assertEquals(activeMembership, Clerk.organizationMembership)
    assertEquals(activeOrganization, Clerk.organization)
  }

  @Test
  fun `organizationMembership returns null when no active organization id exists`() {
    val membership = membership(organization = organization(id = "org_active", name = "Acme"))
    val session =
      session(lastActiveOrganizationId = null, user = user(memberships = listOf(membership)))

    Clerk.updateClient(Client(sessions = listOf(session), lastActiveSessionId = session.id))

    assertNull(Clerk.organizationMembership)
    assertNull(Clerk.organization)
  }

  private fun session(lastActiveOrganizationId: String?, user: User): Session {
    return Session(
      id = "sess_123",
      status = Session.SessionStatus.ACTIVE,
      expireAt = 10_000,
      lastActiveAt = 1_000,
      lastActiveOrganizationId = lastActiveOrganizationId,
      user = user,
      createdAt = 1_000,
      updatedAt = 1_000,
    )
  }

  @Suppress("DEPRECATION")
  private fun user(memberships: List<OrganizationMembership>): User {
    return User(
      id = "user_123",
      hasImage = false,
      imageUrl = "https://example.com/user.png",
      organizationMemberships = memberships,
      passkeys = emptyList(),
      passwordEnabled = true,
      phoneNumbers = emptyList(),
      totpEnabled = false,
      twoFactorEnabled = false,
      updatedAt = 1_000,
    )
  }

  private fun membership(
    id: String = "orgmem_123",
    organization: Organization,
  ): OrganizationMembership {
    return OrganizationMembership(
      id = id,
      publicMetadata = JsonObject(emptyMap()),
      role = "org:admin",
      roleName = "Admin",
      organization = organization,
      createdAt = 1_000,
      updatedAt = 1_000,
    )
  }

  private fun organization(id: String, name: String): Organization {
    return Organization(
      id = id,
      name = name,
      slug = name.lowercase(),
      imageUrl = "https://example.com/$id.png",
      maxAllowedMemberships = 5,
      adminDeleteEnabled = true,
      createdAt = 1_000,
      updatedAt = 1_000,
      publicMetadata = JsonObject(emptyMap()),
    )
  }
}
