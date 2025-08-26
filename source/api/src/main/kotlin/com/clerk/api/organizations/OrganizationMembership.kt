package com.clerk.api.organizations

import com.clerk.api.network.model.userdata.PublicUserData
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
