package com.clerk.api.organizations

import com.clerk.api.image.ImageService
import com.clerk.api.network.ClerkApi
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

suspend fun Organization.createDomain(
  name: String
): ClerkResult<OrganizationDomain, ClerkErrorResponse> {
  return ClerkApi.organization.createOrganizationDomain(organizationId = this.id, name = name)
}

suspend fun Organization.getDomains(
  limit: Int = 20,
  offset: Int = 0,
  enrollmentMode: String? = null,
): ClerkResult<List<OrganizationDomain>, ClerkErrorResponse> {
  return ClerkApi.organization.getAllOrganizationDomains(
    organizationId = this.id,
    limit = limit,
    offset = offset,
    enrollmentMode = enrollmentMode,
  )
}

suspend fun Organization.getDomain(
  domainId: String
): ClerkResult<OrganizationDomain, ClerkErrorResponse> {
  return ClerkApi.organization.getOrganizationDomain(organizationId = this.id, domainId = domainId)
}

suspend fun Organization.deleteDomain(
  domainId: String
): ClerkResult<DeletedObject, ClerkErrorResponse> {
  return ClerkApi.organization.deleteOrganizationDomain(
    organizationId = this.id,
    domainId = domainId,
  )
}
