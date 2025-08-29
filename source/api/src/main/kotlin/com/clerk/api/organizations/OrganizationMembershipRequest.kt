package com.clerk.api.organizations

import com.clerk.api.network.ClerkApi
import com.clerk.api.network.model.error.ClerkErrorResponse
import com.clerk.api.network.model.userdata.PublicUserData
import com.clerk.api.network.serialization.ClerkResult
import kotlinx.serialization.Serializable

/**
 * The model that describes the request of a user to join an organization.
 *
 * This data class represents a membership request that a user has made to join a specific
 * organization. It contains all the necessary information about the request including its status,
 * timing, and associated user data.
 */
@Serializable
data class OrganizationMembershipRequest(
  /** Unique identifier for this membership request */
  val id: String,
  /** Organization Id of the organization this request is for. */
  val organizationId: String,
  /** Public information about the user that this request belongs to. */
  val publicUserData: PublicUserData? = null,
  /** The status of the request. */
  val status: String,
  /** Timestamp the request was created. */
  val createdAt: Long,
  /** Timestamp the request was last updated. */
  val updatedAt: Long,
)

/**
 * Accepts this organization membership request.
 *
 * This function will accept the membership request, allowing the user to join the organization. The
 * request status will be updated accordingly.
 *
 * @return A [ClerkResult] containing either the updated [OrganizationMembershipRequest] on success,
 *   or a [ClerkErrorResponse] on failure.
 */
suspend fun OrganizationMembershipRequest.accept():
  ClerkResult<OrganizationMembershipRequest, ClerkErrorResponse> {
  return ClerkApi.organization.acceptMembershipRequest(
    organizationId = this.organizationId,
    membershipRequestId = this.id,
  )
}

/**
 * Rejects this organization membership request.
 *
 * This function will reject the membership request, denying the user access to join the
 * organization. The request status will be updated accordingly.
 *
 * @return A [ClerkResult] containing either the updated [OrganizationMembershipRequest] on success,
 *   or a [ClerkErrorResponse] on failure.
 */
suspend fun OrganizationMembershipRequest.reject():
  ClerkResult<OrganizationMembershipRequest, ClerkErrorResponse> {
  return ClerkApi.organization.rejectMembershipRequest(
    organizationId = this.organizationId,
    membershipRequestId = this.id,
  )
}
