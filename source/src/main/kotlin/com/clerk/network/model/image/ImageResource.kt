package com.clerk.network.model.image

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Represents information about an image. */
@Serializable
data class ImageResource(
  @SerialName("object") val objectType: String,
  /** The unique identifier of the image. */
  val id: String,
  /** The name of the image. */
  val name: String? = null,
  /** The publicly accessible URL for the image. */
  @SerialName("public_url") val publicUrl: String? = null,
  /** The date when the image was created. */
  @SerialName("created_at") val createdAt: Long? = null,
  /** The date when the image was last updated. */
  @SerialName("updated_at") val updatedAt: Long? = null,
)
