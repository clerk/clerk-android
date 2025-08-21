package com.clerk.api.organizations

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class Organization(
  val id: String,
  val name: String,
  val slug: String?,
  val imageUrl: String,
  val hasImage: Boolean,
  val membersCount: Int?,
  val pendingInvitationsCount: Int?,
  val maxAllowedMemberships: Int,
  val adminDeleteEnabled: Boolean,
  val createdAt: Long,
  val updatedAt: Long,
  val publicMetadata: JsonElement,
)
