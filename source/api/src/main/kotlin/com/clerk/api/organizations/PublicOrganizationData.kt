package com.clerk.api.organizations

import kotlinx.serialization.Serializable

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
