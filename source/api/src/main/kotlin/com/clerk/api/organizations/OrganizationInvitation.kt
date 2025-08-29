package com.clerk.api.organizations

import com.clerk.api.network.ClerkApi
import com.clerk.api.network.model.error.ClerkErrorResponse
import com.clerk.api.network.serialization.ClerkResult
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class OrganizationInvitation(
  /** The unique identifier for this organization invitation. */
  val id: String,
  /** The email address the invitation has been sent to. */
  val emailAddress: String,
  /** The organization ID of the organization this invitation is for. */
  val organizationId: String? = null,
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
  val status: Status,
  /** The timestamp when the invitation was created. */
  val createdAt: Long,
  /** The timestamp when the invitation was last updated. */
  val updatedAt: Long,
  val publicOrganizationData: PublicOrganizationData? = null,
) {
  enum class Status {
    @SerialName("pending") Pending,
    @SerialName("accepted") Accepted,
    @SerialName("revoked") Revoked,
    @SerialName("invalid") Invalid,
    @SerialName("completed") Completed,
  }
}

suspend fun OrganizationInvitation.revoke():
  ClerkResult<OrganizationInvitation, ClerkErrorResponse> {
  return ClerkApi.organization.revokeOrganizationInvitation(
    organizationId = this.organizationId!!,
    invitationId = this.id,
  )
}
