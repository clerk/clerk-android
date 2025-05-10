package com.clerk.model.userdata

import kotlinx.serialization.Serializable

/** Public information about a user that can be shared with other users. */
@Serializable
data class PublicUserData(
  /** The user's first name. */
  val firstName: String? = null,

  /** The user's last name. */
  val lastName: String? = null,

  /** The user's profile image URL. */
  val imageUrl: String? = null,

  /** A boolean indicating whether the user has a profile image. */
  val hasImage: Boolean,

  /** The user's identifier (e.g., email address or phone number). */
  val identifier: String,

  /** The unique identifier of the user. */
  val userId: String,
)
