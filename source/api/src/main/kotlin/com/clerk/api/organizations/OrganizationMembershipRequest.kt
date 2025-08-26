package com.clerk.api.organizations

import com.clerk.api.network.model.userdata.PublicUserData
import kotlinx.serialization.Serializable

/** The model that describes the request of a user to join an organization. */
@Serializable
data class OrganizationMembershipRequest(
  /** Unique identifier for this membership request */
  val id: String,
  /** Organization Id of the organization this request is for. */
  val organizationId: String,
  /** Public information about the user that this request belongs to. */
  val publicUserData: PublicUserData? = null,
  /** The status of the request. */
  val status: String,
  /** Timestamp the request was created. */
  val createdAt: Long,
  /** Timestamp the request was last updated. */
  val updatedAt: Long,
)
