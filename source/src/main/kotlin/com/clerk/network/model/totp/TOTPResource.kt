package com.clerk.network.model.totp

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TOTPResource(
  val id: String,
  val secret: String,
  val uri: String,
  val verified: Boolean,
  @SerialName("created_at") val createdAt: Long,
  @SerialName("updated_at") val updatedAt: Long,
)
