package com.clerk.api.organizations

import com.clerk.api.network.ClerkApi
import com.clerk.api.network.model.error.ClerkErrorResponse
import com.clerk.api.network.serialization.ClerkResult
import java.time.Instant

data class UserOrganizationInvitation(
  val id: String,
  val emailAddress: String,
  val publicOrganizationData: PublicOrganizationData,
  val publicMetadata: String, // JSON in Swift; use String or your JSON type
  val role: String,
  val status: String, // "pending", "accepted", "revoked"
  val createdAt: Instant,
  val updatedAt: Instant,
) {

  data class PublicOrganizationData(
    val hasImage: Boolean,
    val imageUrl: String,
    val name: String,
    val id: String,
    val slug: String? = null,
  )
}

suspend fun UserOrganizationInvitation.accept():
  ClerkResult<UserOrganizationInvitation, ClerkErrorResponse> {
  return ClerkApi.user.acceptUserOrganizationInvitation(id)
}
