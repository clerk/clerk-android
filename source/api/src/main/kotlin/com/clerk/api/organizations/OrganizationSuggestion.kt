package com.clerk.api.organizations

/** An interface representing an organization suggestion. */
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
