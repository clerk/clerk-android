package com.clerk.api.organizations

import kotlinx.serialization.Serializable

@Serializable
data class OrganizationDomain(
  /** Unique identifier for this organization domain */
  val id: String,
  /** Name for this organization domain */
  val name: String,
  /** The organization id of the organization this domain is for. */
  val organizationId: String,
  /** The enrollment mode for new users joining the organization */
  val enrollmentMode: String,
  /** The verification status of the domain */
  val verification: Verification,
  /** The email address that was used to verify this organization domain. */
  val affiliationEmailAddress: String? = null,
  /** The number of total pending invitations sent to emails that match the domain name. */
  val totalPendingInvitations: Int,
  /** The date when the organization domain was created */
  val createdAt: Long,
  /** The date when the organization was last updated */
  val updatedAt: Long,
) {
  @Serializable
  data class Verification(
    val status: String,
    val strategy: String,
    val attempt: Int,
    val expireAt: Long? = null,
  )
}
