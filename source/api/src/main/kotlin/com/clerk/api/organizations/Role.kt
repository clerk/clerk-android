package com.clerk.api.organizations

data class Role(
  val id: String,
  val key: String,
  val name: String,
  val description: String,
  val permissions: List<Permission>,
  val createdAt: Long,
  val updatedAt: Long,
)
