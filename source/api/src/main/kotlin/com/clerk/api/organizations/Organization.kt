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
 */
@Serializable
data class Organization(
  /** The unique identifier of the related organization. */
  val id: String,
  /** The name of the related organization. */
  val name: String,
  /** The organization slug. If supplied, it must be unique for the instance. */
  val slug: String?,
  /** Holds the organization logo or default logo. Compatible with Clerk's Image Optimization. */
  val imageUrl: String,
  /** Whether the organization has an uploaded an image. */
  val hasImage: Boolean = false,
  /** The number of members the associated organization contains. */
  val membersCount: Int? = null,
  /** The number of pending invitations to users to join the organization. */
  val pendingInvitationsCount: Int? = null,
  /** The maximum number of memberships allowed for the organization. */
  val maxAllowedMemberships: Int,
  /** Whether the admin of an organization can delete it. */
  val adminDeleteEnabled: Boolean,
  /** The timestamp the organization was created at. */
  val createdAt: Long,
  /** The timestamp the organization was last updated at. */
  val updatedAt: Long,
  /**
   * Metadata that can be read from the Frontend API and Backend API and can be set only from the
   * Backend API
   */
  val publicMetadata: JsonElement,
)

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

suspend fun Organization.delete(): ClerkResult<DeletedObject, ClerkErrorResponse> {
  return ClerkApi.organization.deleteOrganization(organizationId = this.id)
}

suspend fun Organization.updateLogo(file: File): ClerkResult<Organization, ClerkErrorResponse> {
  val body = ImageService().createMultipartBody(file)
  return ClerkApi.organization.updateOrganizationLogo(organizationId = this.id, file = body)
}
