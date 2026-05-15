package com.clerk.api.organizations

import com.clerk.api.network.ClerkApi
import com.clerk.api.network.model.deleted.DeletedObject
import com.clerk.api.network.model.error.ClerkErrorResponse
import com.clerk.api.network.model.userdata.PublicUserData
import com.clerk.api.network.serialization.ClerkResult
import com.clerk.api.user.currentSessionId
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/** Clerk-provided organization system permission keys. */
@Serializable
enum class OrganizationSystemPermission(val value: String) {
  @SerialName("org:sys_profile:manage") MANAGE_PROFILE("org:sys_profile:manage"),
  @SerialName("org:sys_profile:delete") DELETE_PROFILE("org:sys_profile:delete"),
  @SerialName("org:sys_memberships:read") READ_MEMBERSHIPS("org:sys_memberships:read"),
  @SerialName("org:sys_memberships:manage") MANAGE_MEMBERSHIPS("org:sys_memberships:manage"),
  @SerialName("org:sys_domains:read") READ_DOMAINS("org:sys_domains:read"),
  @SerialName("org:sys_domains:manage") MANAGE_DOMAINS("org:sys_domains:manage"),
  @SerialName("org:sys_billing:read") READ_BILLING("org:sys_billing:read"),
  @SerialName("org:sys_billing:manage") MANAGE_BILLING("org:sys_billing:manage"),
  @SerialName("org:sys_api_keys:read") READ_API_KEYS("org:sys_api_keys:read"),
  @SerialName("org:sys_api_keys:manage") MANAGE_API_KEYS("org:sys_api_keys:manage");

  val key: String
    get() = value
}

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
) {
  /** Returns whether this membership includes the provided organization system permission. */
  fun hasPermission(permission: OrganizationSystemPermission): Boolean {
    return hasPermission(permission.value)
  }

  /** Returns whether this membership includes the provided organization permission key. */
  fun hasPermission(permission: String): Boolean {
    return permissions?.contains(permission) == true
  }

  val canManageProfile: Boolean
    get() = hasPermission(OrganizationSystemPermission.MANAGE_PROFILE)

  val canDeleteOrganization: Boolean
    get() = hasPermission(OrganizationSystemPermission.DELETE_PROFILE)

  val canReadMemberships: Boolean
    get() = hasPermission(OrganizationSystemPermission.READ_MEMBERSHIPS)

  val canManageMemberships: Boolean
    get() = hasPermission(OrganizationSystemPermission.MANAGE_MEMBERSHIPS)

  val canReadDomains: Boolean
    get() = hasPermission(OrganizationSystemPermission.READ_DOMAINS)

  val canManageDomains: Boolean
    get() = hasPermission(OrganizationSystemPermission.MANAGE_DOMAINS)

  val canReadBilling: Boolean
    get() = hasPermission(OrganizationSystemPermission.READ_BILLING)

  val canManageBilling: Boolean
    get() = hasPermission(OrganizationSystemPermission.MANAGE_BILLING)

  val canReadApiKeys: Boolean
    get() = hasPermission(OrganizationSystemPermission.READ_API_KEYS)

  val canManageApiKeys: Boolean
    get() = hasPermission(OrganizationSystemPermission.MANAGE_API_KEYS)
}

/**
 * Updates the role of a member within the organization.
 *
 * @param userId The unique identifier of the user whose membership role should be updated
 * @param role The new role to assign to the user (e.g., "admin", "basic_member")
 * @return A [ClerkResult] containing the updated [OrganizationMembership] on success, or a
 *   [ClerkErrorResponse] on failure
 */
suspend fun OrganizationMembership.updateMembership(
  userId: String,
  role: String,
): ClerkResult<OrganizationMembership, ClerkErrorResponse> {
  return ClerkApi.organization.updateMembership(
    organizationId = this.organization.id,
    userId = userId,
    role = role,
    sessionId = currentSessionId(),
  )
}

/**
 * Deletes the organization membership for the current user.
 *
 * @return A [ClerkResult] containing a [DeletedObject] on success, or a [ClerkErrorResponse] on
 *   failure
 */
suspend fun OrganizationMembership.delete(): ClerkResult<DeletedObject, ClerkErrorResponse> {
  return ClerkApi.user.deleteMembership(this.organization.id)
}
