package com.clerk.api.session

import com.clerk.api.network.model.token.TokenResource
import com.clerk.api.organizations.Organization
import com.clerk.api.organizations.OrganizationMembership
import com.clerk.api.user.User
import java.nio.charset.StandardCharsets
import java.util.Base64
import kotlinx.serialization.json.JsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SessionAuthorizationTest {
  @Test
  fun `checkAuthorization and has share one implementation`() {
    val session =
      session(
        orgId = "org_123",
        orgRole = "org:admin",
        orgPermissions = listOf("org:sys_memberships:read"),
        features = "o:reservations,u:dashboard",
        plans = "u:plus",
      )

    assertEquals(session.has(plan = "plus"), session.checkAuthorization(plan = "plus"))
    assertTrue(session.has(plan = "plus"))
    assertEquals(session.has(plan = "missing"), session.checkAuthorization(plan = "missing"))
    assertFalse(session.has(plan = "missing"))
  }

  @Test
  fun `parses features by scope`() {
    val session =
      session(
        orgId = "org_123",
        orgRole = "admin",
        orgPermissions = listOf("org:read"),
        features = "o:reservations,u:dashboard",
      )

    assertTrue(session.has(feature = "o:reservations"))
    assertTrue(session.has(feature = "org:reservations"))
    assertTrue(session.has(feature = "organization:reservations"))
    assertTrue(session.has(feature = "reservations"))
    assertTrue(session.has(feature = "u:dashboard"))
    assertTrue(session.has(feature = "user:dashboard"))
    assertTrue(session.has(feature = "dashboard"))
    assertFalse(session.has(feature = "lol:dashboard"))
  }

  @Test
  fun `fails when no dimension was requested`() {
    val session =
      session(
        orgId = "org_123",
        orgRole = "org:admin",
        orgPermissions = listOf("org:sys_profile:delete"),
        features = "o:premium",
        plans = "plus",
      )
    assertFalse(session.has())
  }

  @Test
  fun `fails permission and role when org context is missing`() {
    val session = session(orgId = null, features = "", plans = "")
    assertFalse(
      session.has(
        permission = "org:sys_profile:delete",
        reverification = ReverificationConfig.Strict,
      )
    )
    assertFalse(session.has(role = "org:admin", reverification = ReverificationConfig.Strict))
  }

  @Test
  fun `fails reverification when factorVerificationAge is null`() {
    val session =
      session(
        orgId = "org_123",
        orgRole = "org:admin",
        orgPermissions = listOf("org:sys_profile:delete"),
        factorVerificationAge = null,
      )
    assertFalse(
      session.has(
        permission = "org:sys_profile:delete",
        reverification = ReverificationConfig.Strict,
      )
    )
  }

  @Test
  fun `fails when factorVerificationAge payload is malformed`() {
    val session = session(factorVerificationAge = listOf(0))
    assertFalse(session.has(reverification = ReverificationConfig.StrictMfa))
  }

  @Test
  fun `requires AND across billing and org`() {
    val session =
      session(
        orgId = "org_123",
        orgRole = "org:admin",
        orgPermissions = listOf("org:sys_memberships:read"),
        features = "o:reservations",
      )
    assertFalse(session.has(permission = "org:sys_profile:delete", feature = "org:reservations"))
    assertTrue(session.has(permission = "org:sys_memberships:read", feature = "org:reservations"))
  }

  @Test
  fun `requires AND within org when role and permission are requested`() {
    val session =
      session(
        orgId = "org_123",
        orgRole = "org:admin",
        orgPermissions = listOf("org:sys_memberships:read"),
      )
    assertFalse(session.has(role = "org:admin", permission = "org:sys_profile:delete"))
    assertTrue(session.has(role = "org:admin", permission = "org:sys_memberships:read"))
    assertFalse(session.has(role = "org:member", permission = "org:sys_memberships:read"))
  }

  @Test
  fun `requires AND within billing when feature and plan are requested`() {
    val session =
      session(
        orgId = "org_123",
        orgRole = "org:admin",
        orgPermissions = listOf("org:read"),
        features = "o:reservations",
        plans = "u:plus",
      )
    assertTrue(session.has(feature = "org:reservations", plan = "u:plus"))
    assertFalse(session.has(feature = "org:reservations", plan = "u:free"))
    assertFalse(session.has(feature = "org:missing", plan = "u:plus"))
  }

  @Test
  fun `fails feature check when features claim is missing or empty`() {
    val session =
      session(
        orgId = "org_123",
        orgRole = "org:admin",
        orgPermissions = listOf("org:read"),
        features = "",
      )
    assertFalse(session.has(feature = "org:premium"))
  }

  @Test
  fun `fails when token claims are missing`() {
    val session =
      session(orgId = "org_123", orgRole = "org:admin", orgPermissions = listOf("org:read"))
        .copy(lastActiveToken = null)
    assertFalse(session.has(feature = "reservations"))
    assertFalse(session.has(plan = "plus"))
  }

  @Test
  fun `requires AND across org and billing combos`() {
    val session =
      session(
        orgId = "org_123",
        orgRole = "org:admin",
        orgPermissions = listOf("org:sys_memberships:read"),
        features = "o:reservations",
        plans = "u:plus",
      )
    assertFalse(session.has(role = "org:admin", feature = "org:missing"))
    assertFalse(session.has(role = "org:admin", plan = "u:free"))
    assertTrue(session.has(role = "org:admin", feature = "org:reservations"))
  }

  @Test
  fun `fails missing features when reverification would pass`() {
    val session =
      session(
        orgId = "org_123",
        orgRole = "org:admin",
        orgPermissions = listOf("org:sys_profile:delete"),
        features = "",
      )
    assertFalse(session.has(feature = "org:premium", reverification = ReverificationConfig.Strict))
  }

  @Test
  fun `authorizes permission plus reverification when both match`() {
    val session =
      session(
        orgId = "org_123",
        orgRole = "org:admin",
        orgPermissions = listOf("org:sys_memberships:read"),
      )
    assertTrue(
      session.has(
        permission = "org:sys_memberships:read",
        reverification = ReverificationConfig.Strict,
      )
    )
  }

  @Test
  fun `authorizes every requested dimension when all three match`() {
    val session =
      session(
        orgId = "org_123",
        orgRole = "org:admin",
        orgPermissions = listOf("org:sys_memberships:read"),
        features = "o:reservations",
      )
    assertTrue(
      session.has(
        permission = "org:sys_memberships:read",
        feature = "org:reservations",
        reverification = ReverificationConfig.Strict,
      )
    )
  }

  @Test
  fun `authorizes strict_mfa via graceful downgrade when no second factor is enrolled`() {
    val session =
      session(
        orgId = "org_123",
        orgRole = "org:admin",
        orgPermissions = listOf("org:sys_memberships:read"),
        factorVerificationAge = listOf(0, -1),
      )
    assertTrue(
      session.has(
        permission = "org:sys_memberships:read",
        reverification = ReverificationConfig.StrictMfa,
      )
    )
  }

  @Test
  fun `fails permission plus reverification when no factors are enrolled`() {
    val session =
      session(
        orgId = "org_123",
        orgRole = "org:admin",
        orgPermissions = listOf("org:sys_memberships:read"),
        factorVerificationAge = listOf(-1, -1),
      )
    assertFalse(
      session.has(
        permission = "org:sys_memberships:read",
        reverification = ReverificationConfig.Strict,
      )
    )
  }

  @Test
  fun `fails reverification when config object is incomplete or out of range`() {
    val session =
      session(
        orgId = "org_123",
        orgRole = "org:admin",
        orgPermissions = listOf("org:sys_profile:delete"),
      )
    assertFalse(
      session.has(
        reverification =
          ReverificationConfig.Custom(
            level = SessionVerification.Level.MULTI_FACTOR,
            afterMinutes = 0,
          )
      )
    )
    assertFalse(
      session.has(
        reverification =
          ReverificationConfig.Custom(
            level = SessionVerification.Level.MULTI_FACTOR,
            afterMinutes = -1,
          )
      )
    )
    assertFalse(
      session.has(
        reverification =
          ReverificationConfig.Custom(level = SessionVerification.Level.UNKNOWN, afterMinutes = 10)
      )
    )
  }

  @Test
  fun `fails closed without userId`() {
    val session = session(features = "u:dashboard", plans = "u:plus").copy(user = null)
    assertFalse(session.has(feature = "dashboard"))
    assertFalse(session.has(plan = "plus"))
    assertFalse(session.has(reverification = ReverificationConfig.Strict))
  }

  @Test
  fun `splits features by scope including merged ou and uo`() {
    val (org, user) =
      SessionAuthorization.splitByScope("o:reservations,u:dashboard,ou:support-chat,uo:billing")
    assertEquals(listOf("reservations", "support-chat", "billing"), org)
    assertEquals(listOf("dashboard", "support-chat", "billing"), user)
  }

  @Test(expected = IllegalArgumentException::class)
  fun `splitByScope throws when claim element is missing a colon`() {
    SessionAuthorization.splitByScope("reservations,dashboard")
  }

  @Test
  fun `unscoped feature matches merged user and org ids`() {
    val session =
      session(orgId = "org_123", orgRole = "org:admin", features = "o:reservations,u:dashboard")
    assertTrue(session.has(feature = "reservations"))
    assertTrue(session.has(feature = "dashboard"))
    assertFalse(session.has(feature = "missing"))
  }

  @Test
  fun `org scoped feature fails without active org claim`() {
    val session = session(orgId = null, features = "u:dashboard")
    assertFalse(session.has(feature = "o:dashboard"))
    assertTrue(session.has(feature = "u:dashboard"))
  }

  @Test
  fun `role check prefixes org`() {
    val session =
      session(
        orgId = "org_123",
        orgRole = "admin",
        orgPermissions = listOf("org:sys_memberships:read"),
      )
    assertTrue(session.has(role = "org:admin"))
    assertTrue(session.has(role = "admin"))
    assertFalse(session.has(role = "org:member"))
  }

  @Test
  fun `reads fea and pla from last active token`() {
    val session =
      session(
        orgId = "org_123",
        orgRole = "org:admin",
        features = "o:sso,u:dashboard",
        plans = "u:pro",
      )
    assertTrue(session.has(feature = "sso"))
    assertTrue(session.has(plan = "pro"))
    assertFalse(session.has(feature = "missing"))
    assertFalse(session.has(plan = "free"))
  }

  @Test
  fun `has on a cached token stays under one millisecond`() {
    val session =
      session(
        orgId = "org_123",
        orgRole = "org:admin",
        orgPermissions = listOf("org:sys_memberships:read"),
        features = "o:reservations,u:dashboard",
        plans = "u:plus",
      )
    session.has(plan = "plus")

    val samples = DoubleArray(1000)
    repeat(1000) { index ->
      val start = System.nanoTime()
      session.has(plan = "plus")
      samples[index] = (System.nanoTime() - start) / 1_000_000.0
    }
    samples.sort()
    assertTrue(samples[949] < 1.0)
  }
}

@Suppress("DEPRECATION")
private fun session(
  userId: String = "user_123",
  orgId: String? = null,
  orgRole: String? = null,
  orgPermissions: List<String>? = null,
  features: String? = null,
  plans: String? = null,
  factorVerificationAge: List<Int>? = listOf(0, 0),
): Session {
  return Session(
    id = "sess_123",
    status = Session.SessionStatus.ACTIVE,
    expireAt = 0L,
    lastActiveAt = 0L,
    lastActiveOrganizationId = orgId,
    user = user(id = userId, orgId = orgId, role = orgRole, permissions = orgPermissions),
    factorVerificationAge = factorVerificationAge,
    createdAt = 0L,
    updatedAt = 0L,
    lastActiveToken =
      if (features != null || plans != null) {
        TokenResource(jwt = jwtWithClaims(fea = features, pla = plans))
      } else {
        null
      },
  )
}

@Suppress("DEPRECATION")
private fun user(
  id: String,
  orgId: String?,
  role: String?,
  permissions: List<String>?,
): User {
  return User(
    id = id,
    hasImage = false,
    imageUrl = "https://example.com/user.png",
    organizationMemberships =
      if (orgId != null) {
        listOf(membership(orgId = orgId, role = role ?: "org:member", permissions = permissions))
      } else {
        emptyList()
      },
    passkeys = emptyList(),
    passwordEnabled = true,
    phoneNumbers = emptyList(),
    totpEnabled = false,
    twoFactorEnabled = false,
    updatedAt = 1_000,
  )
}

private fun membership(
  orgId: String,
  role: String,
  permissions: List<String>?,
): OrganizationMembership {
  return OrganizationMembership(
    id = "orgmem_123",
    publicMetadata = JsonObject(emptyMap()),
    role = role,
    roleName = "Member",
    permissions = permissions,
    organization = organization(orgId),
    createdAt = 1_000,
    updatedAt = 1_000,
  )
}

private fun organization(id: String): Organization {
  return Organization(
    id = id,
    name = "Org",
    slug = "org",
    imageUrl = "https://example.com/$id.png",
    maxAllowedMemberships = 5,
    adminDeleteEnabled = true,
    createdAt = 1_000,
    updatedAt = 1_000,
    publicMetadata = JsonObject(emptyMap()),
  )
}

private fun jwtWithClaims(fea: String?, pla: String?): String {
  val payload =
    buildList {
        fea?.let { add("\"fea\":\"$it\"") }
        pla?.let { add("\"pla\":\"$it\"") }
      }
      .joinToString(",")
  return "${encode("{\"alg\":\"none\",\"typ\":\"JWT\"}")}.${encode("{$payload}")}.sig"
}

private fun encode(value: String): String =
  Base64.getUrlEncoder().withoutPadding().encodeToString(value.toByteArray(StandardCharsets.UTF_8))
