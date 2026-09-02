package com.clerk.api.billing

import kotlinx.serialization.Serializable

/**
 * A Feature included on a Billing Plan.
 *
 * @property id The unique identifier for the Feature.
 * @property name The display name of the Feature.
 * @property description A short description of what the Feature provides, or `null` if not
 *   provided.
 * @property slug A unique, URL-friendly identifier for the Feature.
 * @property avatarUrl The URL of the Feature's avatar image, or `null` if not set.
 */
@Serializable
data class Feature(
  val id: String,
  val name: String,
  val description: String? = null,
  val slug: String,
  val avatarUrl: String? = null,
)
