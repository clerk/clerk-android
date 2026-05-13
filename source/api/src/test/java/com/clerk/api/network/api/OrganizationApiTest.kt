package com.clerk.api.network.api

import com.clerk.api.network.ApiParams
import com.clerk.api.network.ApiPaths
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

class OrganizationApiTest {

  @Test
  fun `revokeOrganizationInvitation uses invitation revoke route`() {
    val post =
      requireNotNull(method("revokeOrganizationInvitation").getAnnotation(POST::class.java))

    assertEquals(ApiPaths.Organization.Invitations.REVOKE, post.value)
  }

  @Test
  fun `getMembershipRequests uses membership requests route`() {
    val get = requireNotNull(method("getMembershipRequests").getAnnotation(GET::class.java))

    assertEquals(ApiPaths.Organization.MembershipRequests.BASE, get.value)
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
