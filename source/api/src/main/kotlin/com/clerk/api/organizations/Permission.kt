package com.clerk.api.organizations

import kotlinx.serialization.Serializable

/**
 * Represents a permission within an organization.
 *
 * @property id Unique identifier for the permission.
 * @property name Human‑readable name of the permission.
 * @property type Type category of the permission.
 * @property description Detailed description of what the permission allows.
 * @property createdAt Epoch timestamp when the permission was created.
 * @property updatedAt Epoch timestamp of the last update to the permission.
 */
@Serializable
data class Permission(
  /** Unique identifier */
  val id: String,
  /** Name of the permission */
  val name: String,
  /** Type of the permission */
  val type: String,
  /** Description of the permission */
  val description: String,
  /** Creation timestamp */
  val createdAt: Long,
  /** Last update timestamp */
  val updatedAt: Long,
)
