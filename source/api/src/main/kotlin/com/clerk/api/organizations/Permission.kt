package com.clerk.api.organizations

import kotlinx.serialization.Serializable

@Serializable
data class Permission(
  val id: String,
  val name: String,
  val type: String,
  val description: String,
  val createdAt: Long,
  val updatedAt: Long,
)
