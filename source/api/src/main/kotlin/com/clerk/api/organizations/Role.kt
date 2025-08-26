package com.clerk.api.organizations

import kotlinx.serialization.Serializable

/**
 * Represents a role within an organization.
 *
 * @property id Unique identifier of the role.
 * @property key Machine‑friendly key for the role.
 * @property name Human‑readable name of the role.
 * @property description Description of the role's purpose.
 * @property permissions List of permissions assigned to the role.
 * @property createdAt Timestamp of when the role was created (epoch milliseconds).
 * @property updatedAt Timestamp of the last update to the role (epoch milliseconds).
 */
@Serializable
data class Role(
  val id: String,
  val key: String,
  val name: String,
  val description: String,
  val permissions: List<Permission>,
  val createdAt: Long,
  val updatedAt: Long,
)
