package com.clerk.api.organizations

import com.clerk.api.network.ClerkApi
import com.clerk.api.network.model.error.ClerkErrorResponse
import com.clerk.api.network.model.userdata.PublicUserData
import com.clerk.api.network.serialization.ClerkResult
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * The [OrganizationMembership] object is the model around an organization membership entity and
 * describes the relationship between users and organizations.
 */
@Serializable
data class OrganizationMembership(
  /** The unique identifier for this organization membership */
  val id: String,
  /**
   * Metadata that can be read from the Frontend API and Backend API and can be set only from the
   * Backend API
   */
  val publicMetadata: JsonElement,
  /** The role of the current user in the organization */
  val role: String,
  /** The formatted role name associated with this organization membership. */
  val roleName: String,
  /** The permissions associated with the role. */
  val permissions: List<String>? = null,
  /** The public user data associated with this organization membership */
  val publicUserData: PublicUserData? = null,
  /** The organization associated with this organization membership */
  val organization: Organization,
  /** The timestamp when the organization membership was created */
  val createdAt: Long,
  /** The timestamp when the organization membership was last updated */
  val updatedAt: Long,
)

/**
 * Updates the role of a member within the organization.
 *
 * @param userId The unique identifier of the user whose membership role should be updated
 * @param role The new role to assign to the user (e.g., "admin", "basic_member")
 * @return A [ClerkResult] containing the updated [Organization] on success, or a
 *   [ClerkErrorResponse] on failure
 */
suspend fun OrganizationMembership.updateMembership(
  userId: String,
  role: String,
): ClerkResult<Organization, ClerkErrorResponse> {
  return ClerkApi.organization.updateMembership(
    organizationId = this.organization.id,
    userId = userId,
    role = role,
  )
}

/**
 * Removes a member from the organization.
 *
 * @param userId The unique identifier of the user to remove from the organization
 * @return A [ClerkResult] containing the removed [OrganizationMembership] on success, or a
 *   [ClerkErrorResponse] on failure
 */
suspend fun Organization.removeMember(
  userId: String
): ClerkResult<OrganizationMembership, ClerkErrorResponse> {
  return ClerkApi.organization.removeMember(organizationId = this.id, userId = userId)
}
