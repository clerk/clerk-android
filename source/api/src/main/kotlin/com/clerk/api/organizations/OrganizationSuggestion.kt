package com.clerk.api.organizations

import com.clerk.api.Clerk
import com.clerk.api.network.ClerkApi
import kotlinx.serialization.Serializable

/** An interface representing an organization suggestion. */
@Serializable
data class OrganizationSuggestion(
  /** The unique identifier of the suggestion. */
  val id: String,
  /** The public data of the organization. */
  val publicOrganizationData: PublicOrganizationData,
  /** The status of the suggestion. */
  val status: String,
  /** The timestamp when the suggestion was created. */
  val createdAt: Long,
  /** The timestamp when the suggestion was last updated. */
  val updatedAt: Long,
)

/**
 * Accepts this organization suggestion.
 *
 * @param invitationId The identifier of the invitation to accept.
 */
suspend fun OrganizationSuggestion.accept(invitationId: String) {
  ClerkApi.organization.acceptOrganizationSuggestion(
    suggestionId = invitationId,
    sessionId = Clerk.session?.id,
  )
}
