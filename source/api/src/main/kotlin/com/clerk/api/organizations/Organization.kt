package com.clerk.api.organizations

import com.clerk.api.Clerk
import com.clerk.api.image.ImageService
import com.clerk.api.network.ClerkApi
import com.clerk.api.network.ClerkPaginatedResponse
import com.clerk.api.network.model.deleted.DeletedObject
import com.clerk.api.network.model.error.ClerkErrorResponse
import com.clerk.api.network.serialization.ClerkResult
import java.io.File
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * The Organization object holds information about an organization, as well as methods for managing
 * it.
 *
 * Organizations in Clerk represent a collection of users and provide a structured way to manage
 * permissions, roles, and memberships within your application. This data class contains all the
 * essential information about an organization and provides companion methods for creation and
 * retrieval.
 *
 * @property id The unique identifier of the related organization.
 * @property name The name of the related organization.
 * @property slug The organization slug. If supplied, it must be unique for the instance.
 * @property imageUrl Holds the organization logo or default logo. Compatible with Clerk's Image
 *   Optimization.
 * @property hasImage Whether the organization has an uploaded image.
 * @property membersCount The number of members the associated organization contains.
 * @property pendingInvitationsCount The number of pending invitations to users to join the
 *   organization.
 * @property maxAllowedMemberships The maximum number of memberships allowed for the organization.
 * @property adminDeleteEnabled Whether the admin of an organization can delete it.
 * @property createdAt The timestamp the organization was created at (Unix timestamp in
 *   milliseconds).
 * @property updatedAt The timestamp the organization was last updated at (Unix timestamp in
 *   milliseconds).
 * @property publicMetadata Metadata that can be read from the Frontend API and Backend API and can
 *   be set only from the Backend API.
 */
@Serializable
data class Organization(
  val id: String,
  val name: String,
  val slug: String?,
  val imageUrl: String,
  val hasImage: Boolean = false,
  val membersCount: Int? = null,
  val pendingInvitationsCount: Int? = null,
  val maxAllowedMemberships: Int,
  val adminDeleteEnabled: Boolean,
  val createdAt: Long,
  val updatedAt: Long,
  val publicMetadata: JsonElement,
) {
  companion object {
    /**
     * Creates a new organization with the specified name.
     *
     * @param name The name for the new organization.
     * @return A [ClerkResult] containing the created [Organization] on success, or a
     *   [ClerkErrorResponse] on failure.
     */
    suspend fun create(name: String): ClerkResult<Organization, ClerkErrorResponse> {
      return ClerkApi.organization.createOrganization(name = name)
    }

    /**
     * Retrieves an organization by its unique identifier.
     *
     * @param id The unique identifier of the organization to retrieve.
     * @return A [ClerkResult] containing the [Organization] on success, or a [ClerkErrorResponse]
     *   on failure.
     */
    suspend fun get(id: String): ClerkResult<Organization, ClerkErrorResponse> {
      return ClerkApi.organization.getOrganization(id)
    }
  }
}

/**
 * Updates the organization's name and/or slug.
 *
 * @param name The new name for the organization. If null, the current name is preserved.
 * @param slug The new slug for the organization. If null, the current slug is preserved.
 * @return A [ClerkResult] containing the updated [Organization] on success, or a
 *   [ClerkErrorResponse] on failure.
 */
suspend fun Organization.update(
  name: String? = null,
  slug: String? = null,
): ClerkResult<Organization, ClerkErrorResponse> {
  return ClerkApi.organization.updateOrganization(
    organizationId = this.id,
    name = name,
    slug = slug,
  )
}

/**
 * Deletes this organization.
 *
 * Note: This operation is irreversible and will remove all associated data including members,
 * roles, and invitations. The operation will only succeed if [adminDeleteEnabled] is true.
 *
 * @return A [ClerkResult] containing a [DeletedObject] confirmation on success, or a
 *   [ClerkErrorResponse] on failure.
 */
suspend fun Organization.delete(): ClerkResult<DeletedObject, ClerkErrorResponse> {
  return ClerkApi.organization.deleteOrganization(organizationId = this.id)
}

/**
 * Updates the organization's logo image.
 *
 * The uploaded image will be processed and optimized by Clerk's Image Optimization service.
 * Supported formats include JPEG, PNG, and WebP.
 *
 * @param file The image file to upload as the organization logo.
 * @return A [ClerkResult] containing the updated [Organization] with the new logo on success, or a
 *   [ClerkErrorResponse] on failure.
 */
suspend fun Organization.updateLogo(file: File): ClerkResult<Organization, ClerkErrorResponse> {
  val body = ImageService().createMultipartBody(file)
  return ClerkApi.organization.updateOrganizationLogo(organizationId = this.id, file = body)
}

/**
 * Deletes the organization's current logo image.
 *
 * After deletion, the organization will use the default logo. This operation sets [hasImage] to
 * false.
 *
 * @return A [ClerkResult] containing the updated [Organization] without a logo on success, or a
 *   [ClerkErrorResponse] on failure.
 */
suspend fun Organization.deleteLogo(): ClerkResult<Organization, ClerkErrorResponse> {
  return ClerkApi.organization.deleteOrganizationLogo(this.id)
}

/**
 * Retrieves the list of roles available within this organization.
 *
 * @param offset The number of roles to skip when paginating through results. Default is 0.
 * @param limit The maximum number of roles to return per request. Default is 20.
 * @return A [ClerkResult] containing a list of [Role] objects on success, or a [ClerkErrorResponse]
 *   on failure.
 */
suspend fun Organization.getRoles(
  offset: Int = 0,
  limit: Int = 20,
): ClerkResult<List<Role>, ClerkErrorResponse> {
  return ClerkApi.organization.getRoles(organizationId = this.id, limit = limit, offset = offset)
}

/**
 * Creates a new domain for this organization.
 *
 * Domains allow organization members to join automatically when they sign up with an email address
 * that matches the domain. This can help streamline the onboarding process for organization
 * members.
 *
 * @param name The domain name to create (e.g., "example.com").
 * @return A [ClerkResult] containing the created [OrganizationDomain] on success, or a
 *   [ClerkErrorResponse] on failure.
 */
suspend fun Organization.createDomain(
  name: String
): ClerkResult<OrganizationDomain, ClerkErrorResponse> {
  return ClerkApi.organization.createOrganizationDomain(organizationId = this.id, name = name)
}

/**
 * Retrieves all domains associated with this organization.
 *
 * @param limit The maximum number of domains to return per request. Default is 20.
 * @param offset The number of domains to skip when paginating through results. Default is 0.
 * @param enrollmentMode Filter domains by enrollment mode. Can be "manual_invitation" or
 *   "automatic_invitation". If null, all domains are returned.
 * @return A [ClerkResult] containing a paginated response of [OrganizationDomain] objects on
 *   success, or a [ClerkErrorResponse] on failure.
 */
suspend fun Organization.getDomains(
  limit: Int = 20,
  offset: Int = 0,
  enrollmentMode: String? = null,
): ClerkResult<ClerkPaginatedResponse<OrganizationDomain>, ClerkErrorResponse> {
  return ClerkApi.organization.getAllOrganizationDomains(
    organizationId = this.id,
    limit = limit,
    offset = offset,
    enrollmentMode = enrollmentMode,
  )
}

/**
 * Retrieves a specific domain by its unique identifier.
 *
 * @param domainId The unique identifier of the domain to retrieve.
 * @return A [ClerkResult] containing the [OrganizationDomain] on success, or a [ClerkErrorResponse]
 *   on failure.
 */
suspend fun Organization.getDomain(
  domainId: String
): ClerkResult<OrganizationDomain, ClerkErrorResponse> {
  return ClerkApi.organization.getOrganizationDomain(organizationId = this.id, domainId = domainId)
}

/**
 * Deletes a domain from this organization.
 *
 * Note: This operation is irreversible. Once deleted, users with email addresses matching this
 * domain will no longer be able to automatically join the organization.
 *
 * @param domainId The unique identifier of the domain to delete.
 * @return A [ClerkResult] containing a [DeletedObject] confirmation on success, or a
 *   [ClerkErrorResponse] on failure.
 */
suspend fun Organization.deleteDomain(
  domainId: String
): ClerkResult<DeletedObject, ClerkErrorResponse> {
  return ClerkApi.organization.deleteOrganizationDomain(
    organizationId = this.id,
    domainId = domainId,
  )
}

/**
 * Retrieves the memberships for this organization.
 *
 * This method returns all memberships associated with the organization, including information about
 * the users and their roles within the organization.
 *
 * @param query Optional search query to filter memberships by user name or email.
 * @param role Filter memberships by role. Only memberships with the specified role will be
 *   returned.
 * @param limit The maximum number of memberships to return per request. Default is 20.
 * @param offset The number of memberships to skip when paginating through results. Default is 0.
 * @return A [ClerkResult] containing a paginated response of [OrganizationMembership] objects on
 *   success, or a [ClerkErrorResponse] on failure.
 */
suspend fun Organization.getOrganizationMemberships(
  query: String? = null,
  role: String? = null,
  limit: Int = 20,
  offset: Int = 0,
): ClerkResult<ClerkPaginatedResponse<OrganizationMembership>, ClerkErrorResponse> {
  return ClerkApi.organization.getMembers(
    sessionId = Clerk.session?.id,
    limit = limit,
    offset = offset,
    organizationId = id,
    query = query,
    role = role,
  )
}

/**
 * Creates a new membership for this organization.
 *
 * This method adds a user to the organization with the specified role. The user will gain access to
 * the organization's resources based on their assigned role permissions.
 *
 * @param role The role to assign to the new member. If null, the default role will be assigned.
 * @param userId The unique identifier of the user to add to the organization. If null, the current
 *   user will be added.
 * @return A [ClerkResult] containing the created [OrganizationMembership] on success, or a
 *   [ClerkErrorResponse] on failure.
 */
suspend fun Organization.createMembership(
  role: String,
  userId: String,
): ClerkResult<OrganizationMembership, ClerkErrorResponse> {
  return ClerkApi.organization.createMembership(
    organizationId = this.id,
    role = role,
    userId = userId,
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

/**
 * Create an invitation for a user to join an organization. *
 *
 * @param emailAddress The email address the invitation will be sent to.
 * @param role The role that will be assigned to the user after joining. This can be one of the
 *   predefined roles "org:admin", "org:member" or a custom role.
 */
suspend fun Organization.createInvitation(
  emailAddress: String,
  role: String,
): ClerkResult<OrganizationInvitation, ClerkErrorResponse> {
  return ClerkApi.organization.createInvitation(
    organizationId = this.id,
    emailAddress = emailAddress,
    role = role,
  )
}

/**
 * Retrieve all invitations for an organization. The current user must have permissions to manage
 * the members of the organization.
 *
 * @param limit
 * @param offset
 * @param status
 */
suspend fun Organization.getInvitations(
  limit: Int = 20,
  offset: Int = 0,
  status: OrganizationInvitation.Status,
): ClerkResult<ClerkPaginatedResponse<OrganizationInvitation>, ClerkErrorResponse> {
  return ClerkApi.organization.getAllInvitations(
    organizationId = this.id,
    limit = limit,
    offset = offset,
    status = status.name,
  )
}

/**
 * Bulk create an invitation for a user to join an organization.
 *
 * The current user must have permissions to manage the members of the organization.
 *
 * @param emailAddresses An array of email addresses the invitations will be sent to.
 * @param role The role that will be assigned to each of the users after joining. This can be one of
 *   the predefined roles (org:admin, org:basic_member) or a custom role.
 */
suspend fun Organization.bulkCreateInvitations(
  emailAddresses: List<String>,
  role: String,
): ClerkResult<List<OrganizationInvitation>, ClerkErrorResponse> {
  return ClerkApi.organization.bulkCreateInvitations(
    organizationId = this.id,
    emailAddresses = emailAddresses,
    role = role,
  )
}
