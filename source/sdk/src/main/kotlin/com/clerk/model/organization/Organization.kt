package com.clerk.model.organization

import kotlinx.datetime.Instant
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
  val slug: String? = null,

  /** Holds the organization logo or default logo. Compatible with Clerk's Image Optimization. */
  val imageUrl: String,

  /**
   * A getter boolean to check if the organization has an uploaded image. Returns false if Clerk is
   * displaying an avatar for the organization.
   */
  val hasImage: Boolean,

  /** The number of members the associated organization contains. */
  val membersCount: Int? = null,

  /** The number of pending invitations to users to join the organization. */
  val pendingInvitationsCount: Int? = null,

  /** The maximum number of memberships allowed for the organization. */
  val maxAllowedMemberships: Int,

  /** A getter boolean to check if the admin of the organization can delete it. */
  val adminDeleteEnabled: Boolean,

  /** The date when the organization was created. */
  val createdAt: Instant,

  /** The date when the organization was last updated. */
  val updatedAt: Instant,

  /**
   * Metadata that can be read from the Frontend API and Backend API and can be set only from the
   * Backend API.
   */
  val publicMetadata: JsonElement? = null,
)
