package com.clerk.network.model.backupcodes

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class BackupCodeResource(
  @SerialName("object") val objectType: String,
  val id: String,
  val codes: List<String>,
  @SerialName("created_at") val createdAt: Long,
  @SerialName("updated_at") val updatedAt: Long,
)
