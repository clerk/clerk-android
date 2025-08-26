package com.clerk.api.organizations

import com.clerk.api.Clerk
import com.clerk.api.network.ClerkApi
import com.clerk.api.network.model.error.ClerkErrorResponse
import com.clerk.api.network.serialization.ClerkResult
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * The UserOrganizationInvitation object is the model around a user's invitation to an organization.
 */
@Serializable
data class UserOrganizationInvitation(
  /** The unique identifier for this organization invitation. */
  val id: String,
  /** The email address the invitation has been sent to */
  val emailAddress: String,
  /** Public data of the organization. */
  val publicOrganizationData: PublicOrganizationData,
  val publicMetadata: JsonElement,
  /**
   * Represents the user's role in an organization. It will be string unless the developer has
   * provided their own types through ClerkAuthorization.
   *
   * Clerk provides the default roles org:admin and org:member. However, you can create custom roles
   * as well.
   */
  val role: String,
  val status: Status,
  val updatedAt: Long,
  val createdAt: Long,
) {
  @Serializable
  enum class Status {
    Pending,
    Accepted,
    Revoked,
  }
}

/**
 * Accepts this user organization invitation.
 *
 * @param invitationId The identifier of the invitation to accept.
 */
suspend fun UserOrganizationInvitation.accept(
  invitationId: String
): ClerkResult<OrganizationInvitation, ClerkErrorResponse> {
  return ClerkApi.organization.acceptUserOrganizationInvitation(
    invitationId = invitationId,
    sessionId = Clerk.session?.id,
  )
}

@Serializable
data class PublicOrganizationData(
  val id: String,
  /** Whether the organization has an image */
  val hasImage: Boolean,
  /** Holds the organization logo. */
  val imageUrl: String?,
  /** Name of the organization */
  val name: String,
  /** Slug of the organization */
  val slug: String?,
)
