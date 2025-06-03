package com.clerk.model.userdata

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Public information about a user that can be shared with other users. */
@Serializable
data class PublicUserData(
  /** The user's first name. */
  @SerialName("first_name") val firstName: String? = null,

  /** The user's last name. */
  @SerialName("last_name") val lastName: String? = null,

  /** The user's profile image URL. */
  @SerialName("image_url") val imageUrl: String? = null,

  /** A boolean indicating whether the user has a profile image. */
  @SerialName("has_image") val hasImage: Boolean,

  /** The user's identifier (e.g., email address or phone number). */
  val identifier: String,

  /** The unique identifier of the user. */
  @SerialName("user_id") val userId: String? = null,
)
