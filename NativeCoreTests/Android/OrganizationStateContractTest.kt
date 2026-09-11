package com.clerk.api

import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.util.Base64
import kotlinx.coroutines.*
import kotlinx.serialization.json.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OrganizationStateContractTest {
  @Test fun membershipPreservesAllPermissionKeys() = verify("permissions-present")
  @Test fun nullMembershipPermissionsBecomeEmpty() = verify("permissions-null")
  @Test fun omittedMembershipPermissionsBecomeEmpty() = verify("permissions-omitted")
  @Test fun activeOrganizationSelectsMatchingMembership() = verify("active")
  @Test fun noActiveOrganizationPreservesMemberships() = verify("no-active")
  @Test fun unknownActiveOrganizationDoesNotSelectAnotherMembership() = verify("missing-active")
  @Test fun manualDomainPreservesVerifiedStatus() = verify("manual_invitation")
  @Test fun automaticInvitationDomainPreservesUnverifiedStatus() = verify("automatic_invitation")
  @Test fun automaticSuggestionDomainPreservesAbsentVerification() = verify("automatic_suggestion")
  @Test fun enterpriseDomainPreservesExpiredStatus() = verify("enterprise_sso")
  @Test fun domainPreservesUnknownModeAndVerificationStatus() = verify("future_mode")

  private fun verify(scenario: String) = runBlocking {
    withTimeout(15000) {
      withContext(Dispatchers.Main.immediate) {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val base = PackagedFixtures(instrumentation.context)
        val permissionKeys = listOf("custom:permission", "org:sys_profile:manage", "org:sys_profile:delete",
          "org:sys_memberships:read", "org:sys_memberships:manage", "org:sys_domains:read",
          "org:sys_domains:manage", "org:sys_billing:read", "org:sys_billing:manage",
          "org:sys_api_keys:read", "org:sys_api_keys:manage")
        val activeId = when (scenario) { "no-active" -> null; "missing-active" -> "org_missing"; else -> "org_active" }
        val membershipPayloads = listOf("other", "active").map { name ->
          buildJsonObject {
            put("object", "organization_membership"); put("id", "orgmem_$name")
            put("role", "org:admin"); put("role_name", "Admin")
            if (scenario != "permissions-omitted") put("permissions", if (scenario == "permissions-null") JsonNull else JsonArray(permissionKeys.map(::JsonPrimitive)))
            put("public_metadata", buildJsonObject {})
            put("created_at", 1713200000000L); put("updated_at", 1713200000000L)
            put("organization", buildJsonObject {
              put("object", "organization"); put("id", "org_$name"); put("name", name); put("slug", name)
              put("image_url", ""); put("has_image", false); put("public_metadata", buildJsonObject {})
              put("created_at", 1713200000000L); put("updated_at", 1713200000000L)
            })
          }
        }
        val client = base.fixtures.getValue("authenticatedClient").jsonObject
        val session = client.getValue("sessions").jsonArray[0].jsonObject
        val user = JsonObject(session.getValue("user").jsonObject + ("organization_memberships" to JsonArray(membershipPayloads)))
        base.clientResponse = JsonObject(client + ("sessions" to JsonArray(listOf(JsonObject(session + mapOf("user" to user, "last_active_organization_id" to (activeId?.let(::JsonPrimitive) ?: JsonNull)))))))
        val domainModes = mapOf("manual_invitation" to OrganizationEnrollmentMode.ManualInvitation,
          "automatic_invitation" to OrganizationEnrollmentMode.AutomaticInvitation,
          "automatic_suggestion" to OrganizationEnrollmentMode.AutomaticSuggestion,
          "enterprise_sso" to OrganizationEnrollmentMode.EnterpriseSso,
          "future_mode" to OrganizationEnrollmentMode.Unrecognized("future_mode"))
        val domainStatuses = mapOf("manual_invitation" to OrganizationDomainVerificationStatus.Verified,
          "automatic_invitation" to OrganizationDomainVerificationStatus.Unverified,
          "automatic_suggestion" to null, "enterprise_sso" to OrganizationDomainVerificationStatus.Expired,
          "future_mode" to OrganizationDomainVerificationStatus.Unrecognized("future_status"))
        var reads = 0
        val host = object : NativeCapabilities {
          override val supported = base.supported
          override suspend fun perform(capability: String, arguments: JsonElement): JsonElement {
            val args = arguments.jsonObject
            if (capability == "http" && Uri.parse(args.getValue("url").requireString()).path == "/v1/organizations/org_active/domains") {
              reads++
              check(args["method"] == JsonPrimitive("GET"))
              check(Uri.parse(args.getValue("url").requireString()).getQueryParameter("_clerk_session_id") == "sess_native")
              val payload = buildJsonObject {
                put("object", "organization_domain"); put("id", "orgdom_state"); put("organization_id", "org_active")
                put("name", "example.com"); put("enrollment_mode", scenario)
                put("verification", domainStatuses[scenario]?.let { status -> buildJsonObject {
                  put("status", status.rawValue); put("strategy", "email_code"); put("attempts", 0); put("expires_at", 1713200000000L)
                }} ?: JsonNull)
                put("affiliation_email_address", JsonNull); put("total_pending_invitations", 0); put("total_pending_suggestions", 0)
                put("created_at", 1713200000000L); put("updated_at", 1713200000000L)
              }
              return buildJsonObject {
                put("status", 200); put("headers", buildJsonObject {})
                put("body", buildJsonObject { put("response", buildJsonObject { put("data", JsonArray(listOf(payload))); put("total_count", 1) }) }.toString())
              }
            }
            return base.perform(capability, arguments)
          }
        }
        val clerk = Clerk.connect(instrumentation.targetContext, ClerkConfiguration("pk_test_" + Base64.getEncoder().encodeToString("native-core.clerk.accounts.dev$".toByteArray()), "clerk-test://sso-callback"), host)
        try {
          val currentUser = checkNotNull(clerk.user)
          val currentSession = checkNotNull(clerk.session)
          val memberships = currentUser.organizationMemberships
          check(memberships.map { it.organization.id } == listOf("org_other", "org_active"))
          check(currentSession.lastActiveOrganizationId == activeId)
          if (scenario in domainModes) {
            val result = checkNotNull(clerk.organization).getDomains()
            check(result.totalCount == 1.0 && result.data.size == 1)
            val domain = result.data.single()
            check(domain.enrollmentMode == domainModes[scenario] && domain.enrollmentMode.rawValue == scenario)
            check(domain.verification?.status == domainStatuses[scenario])
            check(domain.affiliationVerification?.status == domainStatuses[scenario])
            check(domain.organizationId == "org_active" && reads == 1)
          } else {
            check(reads == 0)
            if (scenario.startsWith("permissions-")) {
              val expected = if (scenario == "permissions-present") permissionKeys else emptyList()
              memberships.forEach { check(it.permissions == expected && it.role == "org:admin" && it.roleName == "Admin") }
            }
            if (activeId == "org_active") check(clerk.organization === memberships[1].organization)
            else check(clerk.organization == null)
          }
          check(clerk.user === currentUser && clerk.session === currentSession)
        } finally { clerk.close() }
      }
    }
  }
}
