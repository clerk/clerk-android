package com.clerk.api.network.model.roles

import com.clerk.api.network.model.permission.Permission
import kotlinx.serialization.Serializable

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
