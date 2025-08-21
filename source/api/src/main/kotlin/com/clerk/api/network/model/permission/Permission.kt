package com.clerk.api.network.model.permission

import kotlinx.serialization.Serializable

@Serializable
data class Permission(
  val id: String,
  val key: String,
  val name: String,
  val type: String,
  val description: String,
  val createdAt: Long,
  val updatedAt: Long,
)
