package com.clerk.api.network

import kotlinx.serialization.Serializable

/**
 * An interface that describes the response of a method that returns a paginated list of resources.
 */
@Serializable
data class ClerkPaginatedResponse<T>(
  /** A list that contains fetched data */
  val data: List<T>,
  /** The total count of resources */
  val totalCount: Int,
  /** Whether organization role updates are temporarily disabled while roles migrate. */
  val hasRoleSetMigration: Boolean? = null,
)
