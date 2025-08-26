package com.clerk.api.organizations

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class OrganizationInvitation(
  /** The unique identifier for this organization invitation. */
  val id: String,
  /** The email address the invitation has been sent to. */
  val emailAddress: String,
  /** The organization ID of the organization this invitation is for. */
  val organizationId: String,
  /**
   * Metadata that can be read from the Frontend API and Backend API and can be set only from the
   * Backend API.
   */
  val publicMetadata: JsonElement,
  /**
   * The role of the user in the organization.
   *
   * Clerk provides the default roles org:admin and org:member. However, you can create custom roles
   * as well.
   */
  val role: String,
  /** The status of the invitation. */
  val status: String,
  /** The timestamp when the invitation was created. */
  val createdAt: Long,
  /** The timestamp when the invitation was last updated. */
  val updatedAt: Long,
)
