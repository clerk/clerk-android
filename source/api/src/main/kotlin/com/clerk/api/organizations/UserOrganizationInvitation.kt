package com.clerk.api.organizations

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
  enum class Status {
    Pending,
    Accepted,
    Revoked,
  }
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
