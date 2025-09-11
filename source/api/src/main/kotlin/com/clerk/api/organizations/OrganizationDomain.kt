package com.clerk.api.organizations

import com.clerk.api.network.ClerkApi
import com.clerk.api.network.model.deleted.DeletedObject
import com.clerk.api.network.model.error.ClerkErrorResponse
import com.clerk.api.network.serialization.ClerkResult
import kotlinx.serialization.Serializable

/**
 * Represents an organization domain in the Clerk system.
 *
 * Organization domains allow organizations to automatically enroll users based on their email
 * domain and manage domain verification.
 *
 * @property id Unique identifier for this organization domain
 * @property name Name for this organization domain
 * @property organizationId The organization id of the organization this domain is for
 * @property enrollmentMode The enrollment mode for new users joining the organization
 * @property verification The verification status and details of the domain
 * @property affiliationEmailAddress The email address that was used to verify this organization
 *   domain, null if not verified
 * @property totalPendingInvitations The number of total pending invitations sent to emails that
 *   match the domain name
 * @property createdAt The date when the organization domain was created (Unix timestamp in
 *   milliseconds)
 * @property updatedAt The date when the organization domain was last updated (Unix timestamp in
 *   milliseconds)
 * @property totalPendingSuggestions The number of pending suggestions for the organization domain.
 * @property publicOrganizationData The data's public organization.
 */
@Serializable
data class OrganizationDomain(
  val id: String,
  val name: String,
  val organizationId: String,
  val enrollmentMode: String,
  val verification: Verification? = null,
  val affiliationEmailAddress: String? = null,
  val totalPendingInvitations: Int,
  val createdAt: Long,
  val updatedAt: Long,
  val publicOrganizationData: PublicOrganizationData? = null,
  val totalPendingSuggestions: Int,
) {
  /**
   * Represents the verification details for an organization domain.
   *
   * @property status The current verification status of the domain
   * @property strategy The verification strategy being used
   * @property attempts The current attempt number for verification
   * @property expireAt The expiration time for the verification attempt (Unix timestamp in
   *   milliseconds), null if no expiration
   */
  @Serializable
  data class Verification(
    val status: String,
    val strategy: String,
    val attempts: Int,
    val expireAt: Long? = null,
  )
}

private val organizationApi by lazy { ClerkApi.organization }

/**
 * Deletes this organization domain.
 *
 * @return A [ClerkResult] containing either a [DeletedObject] on success or a [ClerkErrorResponse]
 *   on failure
 */
suspend fun OrganizationDomain.delete(): ClerkResult<DeletedObject, ClerkErrorResponse> {
  return organizationApi.deleteOrganizationDomain(
    organizationId = this.organizationId,
    domainId = this.id,
  )
}

/**
 * Prepares affiliation verification for this organization domain by sending a verification email.
 *
 * @param affiliationEmailAddress The email address to send the verification code to
 * @return A [ClerkResult] containing either the updated [OrganizationDomain] on success or a
 *   [ClerkErrorResponse] on failure
 */
suspend fun OrganizationDomain.prepareAffiliationVerification(
  affiliationEmailAddress: String
): ClerkResult<OrganizationDomain, ClerkErrorResponse> {
  return organizationApi.prepareAffiliationVerification(
    organizationId = this.organizationId,
    domainId = this.id,
    affiliationEmailAddress = affiliationEmailAddress,
  )
}

/**
 * Attempts to verify the affiliation of this organization domain using a verification code.
 *
 * **Note:** You must call [prepareAffiliationVerification] first to receive the verification code
 * via email.
 *
 * @param code The verification code received via email after calling
 *   [prepareAffiliationVerification]
 * @return A [ClerkResult] containing either the updated [OrganizationDomain] on success or a
 *   [ClerkErrorResponse] on failure
 */
suspend fun OrganizationDomain.attemptAffiliationVerification(
  code: String
): ClerkResult<OrganizationDomain, ClerkErrorResponse> {
  return organizationApi.attemptAffiliationVerification(
    organizationId = this.organizationId,
    domainId = this.id,
    code = code,
  )
}

suspend fun OrganizationDomain.updateEnrollmentMode(
  enrollmentMode: String,
  deletePending: Boolean? = null,
): ClerkResult<OrganizationDomain, ClerkErrorResponse> {
  return organizationApi.updateEnrollmentMode(
    organizationId = this.organizationId,
    domainId = this.id,
    enrollmentMode = enrollmentMode,
    deletePending = deletePending,
  )
}

/**
 * When the Clerk API returns a collection of objects it uses a top level data tag in the JSON. This
 * class represents that JSON structure since we can't pass the list type to the clerk result.
 *
 * EX: Invalid: ClerkResult<List<OrganizationDomain>, ClerkErrorResponse> Valid:
 * ClerkResult<OrganizationMembershipCollection, ClerkErrorResponse>
 */
