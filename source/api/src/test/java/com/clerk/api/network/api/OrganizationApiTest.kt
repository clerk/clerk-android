package com.clerk.api.network.api

import com.clerk.api.network.ApiParams
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

@Suppress("DEPRECATION")
class OrganizationApiTest {

  @Test
  fun `revokeOrganizationInvitation posts to the invitation revoke path`() {
    val post =
      requireNotNull(method("revokeOrganizationInvitation").getAnnotation(POST::class.java))

    assertEquals("organizations/{organization_id}/invitations/{invitation_id}/revoke", post.value)
  }

  @Test
  fun `getMembershipRequests gets the membership requests path`() {
    val get = requireNotNull(method("getMembershipRequests").getAnnotation(GET::class.java))

    assertEquals("organizations/{organization_id}/membership_requests", get.value)
  }

  @Test
  fun `organization mutating endpoints include clerk session id query`() {
    val mutatingMethods =
      listOf(
        "updateOrganization",
        "deleteOrganization",
        "updateOrganizationLogo",
        "deleteOrganizationLogo",
        "createOrganizationDomain",
        "deleteOrganizationDomain",
        "updateEnrollmentMode",
        "prepareAffiliationVerification",
        "attemptAffiliationVerification",
        "createMembership",
        "updateMembership",
        "removeMember",
        "createInvitation",
        "bulkCreateInvitations",
        "revokeOrganizationInvitation",
        "acceptMembershipRequest",
        "rejectMembershipRequest",
      )

    mutatingMethods.forEach { methodName ->
      assertTrue(
        "$methodName should include _clerk_session_id",
        method(methodName).hasQuery(ApiParams.CLERK_SESSION_ID),
      )
    }
  }

  private fun method(name: String): java.lang.reflect.Method {
    return OrganizationApi::class.java.methods.single { it.name == name }
  }

  private fun java.lang.reflect.Method.hasQuery(value: String): Boolean {
    return parameterAnnotations.any { annotations ->
      annotations.filterIsInstance<Query>().any { it.value == value }
    }
  }
}
