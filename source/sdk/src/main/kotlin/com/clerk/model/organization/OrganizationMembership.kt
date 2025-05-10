package com.clerk.model.organization

import com.clerk.model.userdata.PublicUserData
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * The `OrganizationMembership` object is the model around an organization membership entity and
 * describes the relationship between users and organizations.
 */
@Serializable
data class OrganizationMembership(
  /** The unique identifier for this organization membership. */
  val id: String,

  /**
   * Metadata that can be read from the Frontend API and Backend API and can be set only from the
   * Backend API.
   */
  val publicMetadata: JsonElement,

  /** The role of the current user in the organization. */
  val role: String,

  /** The permissions associated with the role. */
  val permissions: List<String>? = null,

  /** Public information about the user that this membership belongs to. */
  val publicUserData: PublicUserData? = null,

  /** The `Organization` object the membership belongs to. */
  val organization: Organization,

  /** The date when the membership was created. */
  val createdAt: Instant,

  /** The date when the membership was last updated. */
  val updatedAt: Instant,
)
