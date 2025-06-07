package com.clerk.network.model.deleted

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** The DeletedObject class represents an item that has been deleted from the database. */
@Serializable
data class DeletedObject(
  /** The object type that has been deleted. */
  @SerialName("object") val objectType: String? = null,
  /** The ID of the deleted item. */
  val id: String? = null,
  /** A boolean checking if the item has been deleted or not. */
  val deleted: Boolean? = null,
)
