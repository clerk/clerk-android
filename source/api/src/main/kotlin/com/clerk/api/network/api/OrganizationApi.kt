package com.clerk.api.network.api

import com.clerk.api.network.ApiParams
import com.clerk.api.network.ApiPaths
import com.clerk.api.network.ClerkPaginatedResponse
import com.clerk.api.network.model.deleted.DeletedObject
import com.clerk.api.network.model.error.ClerkErrorResponse
import com.clerk.api.network.serialization.ClerkResult
import com.clerk.api.organizations.Organization
import com.clerk.api.organizations.OrganizationDomain
import com.clerk.api.organizations.OrganizationInvitation
import com.clerk.api.organizations.OrganizationMembership
import com.clerk.api.organizations.Role
import okhttp3.MultipartBody
import retrofit2.http.DELETE
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Retrofit API interface for organization-related operations in the Clerk system.
 *
 * This interface defines all HTTP endpoints for managing organizations, domains, invitations,
 * roles, and other organization-related functionality.
 *
 * @see com.clerk.api.organizations
 */
interface OrganizationApi {

  /**
   * Retrieves roles for a specific organization.
   *
   * @param organizationId The unique identifier of the organization
   * @param limit Maximum number of roles to return (optional)
   * @param offset Number of roles to skip for pagination (optional)
   * @return A [ClerkResult] containing either a list of [Role]s on success or a
   *   [ClerkErrorResponse] on failure
   * @see com.clerk.api.organizations.getRoles
   */
  @GET(ApiPaths.Organization.ROLES)
  suspend fun getRoles(
    @Path(ApiParams.ORGANIZATION_ID) organizationId: String,
    @Query("limit") limit: Int? = null,
    @Query("offset") offset: Int? = null,
  ): ClerkResult<List<Role>, ClerkErrorResponse>

  /**
   * Creates a new organization.
   *
   * @param name The name of the organization to create
   * @return A [ClerkResult] containing either the created [Organization] on success or a
   *   [ClerkErrorResponse] on failure
   * @see com.clerk.api.organizations.create
   */
  @FormUrlEncoded
  @POST(ApiPaths.Organization.BASE)
  suspend fun createOrganization(
    @Field("name") name: String
  ): ClerkResult<Organization, ClerkErrorResponse>

  /**
   * Retrieves a specific organization by its ID.
   *
   * @param organizationId The unique identifier of the organization
   * @return A [ClerkResult] containing either the [Organization] on success or a
   *   [ClerkErrorResponse] on failure
   * @see com.clerk.api.organizations.get
   */
  @GET(ApiPaths.Organization.WITH_ID)
  suspend fun getOrganization(
    @Path(ApiParams.ORGANIZATION_ID) organizationId: String
  ): ClerkResult<Organization, ClerkErrorResponse>

  /**
   * Updates an existing organization.
   *
   * @param organizationId The unique identifier of the organization to update
   * @param name The new name for the organization (optional)
   * @param slug The new slug for the organization (optional)
   * @return A [ClerkResult] containing either the updated [Organization] on success or a
   *   [ClerkErrorResponse] on failure
   * @see com.clerk.api.organizations.update
   */
  @FormUrlEncoded
  @PATCH(ApiPaths.Organization.WITH_ID)
  suspend fun updateOrganization(
    @Path(ApiParams.ORGANIZATION_ID) organizationId: String,
    @Field("name") name: String? = null,
    @Field("slug") slug: String? = null,
  ): ClerkResult<Organization, ClerkErrorResponse>

  /**
   * Deletes an organization.
   *
   * @param organizationId The unique identifier of the organization to delete
   * @return A [ClerkResult] containing either a [DeletedObject] on success or a
   *   [ClerkErrorResponse] on failure
   * @see com.clerk.api.organizations.delete
   */
  @DELETE(ApiPaths.Organization.WITH_ID)
  suspend fun deleteOrganization(
    @Path(ApiParams.ORGANIZATION_ID) organizationId: String
  ): ClerkResult<DeletedObject, ClerkErrorResponse>

  /**
   * Updates the logo for an organization.
   *
   * @param organizationId The unique identifier of the organization
   * @param file The logo file to upload as a multipart body
   * @return A [ClerkResult] containing either the updated [Organization] on success or a
   *   [ClerkErrorResponse] on failure
   * @see com.clerk.api.organizations.updateLogo
   */
  @Multipart
  @PUT(ApiPaths.Organization.LOGO)
  suspend fun updateOrganizationLogo(
    @Path(ApiParams.ORGANIZATION_ID) organizationId: String,
    @Part file: MultipartBody.Part,
  ): ClerkResult<Organization, ClerkErrorResponse>

  /**
   * Deletes the logo for an organization.
   *
   * @param organizationId The unique identifier of the organization
   * @return A [ClerkResult] containing either the updated [Organization] on success or a
   *   [ClerkErrorResponse] on failure
   * @see com.clerk.api.organizations.deleteLogo
   */
  @DELETE(ApiPaths.Organization.LOGO)
  suspend fun deleteOrganizationLogo(
    @Path(ApiParams.ORGANIZATION_ID) organizationId: String
  ): ClerkResult<Organization, ClerkErrorResponse>

  /**
   * Creates a new domain for an organization.
   *
   * @param organizationId The unique identifier of the organization
   * @param name The domain name to create
   * @return A [ClerkResult] containing either the created [OrganizationDomain] on success or a
   *   [ClerkErrorResponse] on failure
   * @see com.clerk.api.organizations.createDomain
   */
  @FormUrlEncoded
  @POST(ApiPaths.Organization.Domain.BASE)
  suspend fun createOrganizationDomain(
    @Path(ApiParams.ORGANIZATION_ID) organizationId: String,
    @Field("name") name: String,
  ): ClerkResult<OrganizationDomain, ClerkErrorResponse>

  /**
   * Retrieves all domains for an organization with optional filtering and pagination.
   *
   * @param organizationId The unique identifier of the organization
   * @param limit Maximum number of domains to return (optional)
   * @param offset Number of domains to skip for pagination (optional)
   * @param verified Filter by verification status (optional)
   * @param enrollmentMode Filter by enrollment mode (optional)
   * @return A [ClerkResult] containing either a list of [OrganizationDomain]s on success or a
   *   [ClerkErrorResponse] on failure
   * @see com.clerk.api.organizations.getDomains
   */
  @GET(ApiPaths.Organization.Domain.BASE)
  suspend fun getAllOrganizationDomains(
    @Path(ApiParams.ORGANIZATION_ID) organizationId: String,
    @Query(ApiParams.LIMIT) limit: Int? = null,
    @Query(ApiParams.OFFSET) offset: Int? = null,
    @Query("verified") verified: Boolean? = null,
    @Query("enrollment_mode") enrollmentMode: String? = null,
  ): ClerkResult<ClerkPaginatedResponse<OrganizationDomain>, ClerkErrorResponse>

  /**
   * Retrieves a specific organization domain by its ID.
   *
   * @param organizationId The unique identifier of the organization
   * @param domainId The unique identifier of the domain
   * @return A [ClerkResult] containing either the [OrganizationDomain] on success or a
   *   [ClerkErrorResponse] on failure
   * @see com.clerk.api.organizations.getDomain
   */
  @GET(ApiPaths.Organization.Domain.WITH_ID)
  suspend fun getOrganizationDomain(
    @Path(ApiParams.ORGANIZATION_ID) organizationId: String,
    @Path(ApiParams.DOMAIN_ID) domainId: String,
  ): ClerkResult<OrganizationDomain, ClerkErrorResponse>

  /**
   * Deletes an organization domain.
   *
   * @param organizationId The unique identifier of the organization
   * @param domainId The unique identifier of the domain to delete
   * @return A [ClerkResult] containing either a [DeletedObject] on success or a
   *   [ClerkErrorResponse] on failure
   * @see com.clerk.api.organizations.deleteDomain
   */
  @DELETE(ApiPaths.Organization.Domain.WITH_ID)
  suspend fun deleteOrganizationDomain(
    @Path(ApiParams.ORGANIZATION_ID) organizationId: String,
    @Path(ApiParams.DOMAIN_ID) domainId: String,
  ): ClerkResult<DeletedObject, ClerkErrorResponse>

  /**
   * Updates the enrollment mode for an organization domain.
   *
   * @param organizationId The unique identifier of the organization
   * @param domainId The unique identifier of the domain
   * @param enrollmentMode The new enrollment mode to set
   * @param deletePending Whether to delete pending invitations (optional)
   * @return A [ClerkResult] containing either the updated [OrganizationDomain] on success or a
   *   [ClerkErrorResponse] on failure
   * @see com.clerk.api.organizations.updateEnrollmentMode
   */
  @FormUrlEncoded
  @POST(ApiPaths.Organization.Domain.UPDATE_ENROLLMENT_MODE)
  suspend fun updateEnrollmentMode(
    @Path(ApiParams.ORGANIZATION_ID) organizationId: String,
    @Path(ApiParams.DOMAIN_ID) domainId: String,
    @Field("enrollment_mode") enrollmentMode: String,
    @Field("delete_pending") deletePending: Boolean? = null,
  ): ClerkResult<OrganizationDomain, ClerkErrorResponse>

  /**
   * Prepares affiliation verification for an organization domain by sending a verification email.
   *
   * @param organizationId The unique identifier of the organization
   * @param domainId The unique identifier of the domain
   * @param affiliationEmailAddress The email address to send the verification code to
   * @return A [ClerkResult] containing either the updated [OrganizationDomain] on success or a
   *   [ClerkErrorResponse] on failure
   * @see com.clerk.api.organizations.prepareAffiliationVerification
   */
  @FormUrlEncoded
  @POST(ApiPaths.Organization.Domain.PREPARE_AFFILIATION)
  suspend fun prepareAffiliationVerification(
    @Path(ApiParams.ORGANIZATION_ID) organizationId: String,
    @Path(ApiParams.DOMAIN_ID) domainId: String,
    @Field("affiliation_email_address") affiliationEmailAddress: String,
  ): ClerkResult<OrganizationDomain, ClerkErrorResponse>

  /**
   * Attempts to verify the affiliation of an organization domain using a verification code.
   *
   * **Note:** You must call [prepareAffiliationVerification] first to receive the verification code
   * via email.
   *
   * @param organizationId The unique identifier of the organization
   * @param domainId The unique identifier of the domain
   * @param code The verification code received via email after calling
   *   [prepareAffiliationVerification]
   * @return A [ClerkResult] containing either the updated [OrganizationDomain] on success or a
   *   [ClerkErrorResponse] on failure
   * @see com.clerk.api.organizations.attemptAffiliationVerification
   */
  @FormUrlEncoded
  @POST(ApiPaths.Organization.Domain.ATTEMPT_AFFILIATION)
  suspend fun attemptAffiliationVerification(
    @Path(ApiParams.ORGANIZATION_ID) organizationId: String,
    @Path(ApiParams.DOMAIN_ID) domainId: String,
    @Field("code") code: String,
  ): ClerkResult<OrganizationDomain, ClerkErrorResponse>

  /**
   * Creates a new membership for a user in an organization.
   *
   * @param organizationId The unique identifier of the organization
   * @param role The role to assign to the user in the organization (optional)
   * @param userId The unique identifier of the user to add as a member
   * @return A [ClerkResult] containing either the created [OrganizationMembership] on success or a
   *   [ClerkErrorResponse] on failure
   * @see com.clerk.api.organizations.createMembership
   */
  @FormUrlEncoded
  @POST(ApiPaths.Organization.MEMBERSHIPS)
  suspend fun createMembership(
    @Path(ApiParams.ORGANIZATION_ID) organizationId: String,
    @Field(ApiParams.ROLE) role: String?,
    @Field(ApiParams.USER_ID) userId: String?,
  ): ClerkResult<OrganizationMembership, ClerkErrorResponse>

  /**
   * Retrieves memberships for an organization with optional filtering and pagination.
   *
   * @param organizationId The unique identifier of the organization
   * @param limit Maximum number of memberships to return (optional)
   * @param offset Number of memberships to skip for pagination (optional)
   * @param paginated Whether to return paginated results (defaults to true)
   * @param sessionId Optional session ID for the operation
   * @param role Filter memberships by role (optional)
   * @param query Search query to filter memberships (optional)
   * @return A [ClerkResult] containing either a list of [OrganizationMembership]s on success or a
   *   [ClerkErrorResponse] on failure
   * @see com.clerk.api.organizations.getMemberships
   */
  @GET(ApiPaths.Organization.MEMBERSHIPS)
  suspend fun getMembers(
    @Path(ApiParams.ORGANIZATION_ID) organizationId: String,
    @Query(ApiParams.LIMIT) limit: Int?,
    @Query(ApiParams.OFFSET) offset: Int?,
    @Query("paginated") paginated: Boolean = true,
    @Query(ApiParams.CLERK_SESSION_ID) sessionId: String? = null,
    @Query(ApiParams.ROLE) role: String? = null,
    @Query("query") query: String? = null,
  ): ClerkResult<ClerkPaginatedResponse<OrganizationMembership>, ClerkErrorResponse>

  /**
   * Updates the role of an existing member in an organization.
   *
   * @param organizationId The unique identifier of the organization
   * @param userId The unique identifier of the user whose membership to update
   * @param role The new role to assign to the user
   * @return A [ClerkResult] containing either the updated [OrganizationMembership] on success or a
   *   [ClerkErrorResponse] on failure
   * @see com.clerk.api.organizations.updateMembership
   */
  @FormUrlEncoded
  @PATCH(ApiPaths.Organization.MEMBERSHIP_WITH_USER_ID)
  suspend fun updateMembership(
    @Path(ApiParams.ORGANIZATION_ID) organizationId: String,
    @Path(ApiParams.USER_ID) userId: String,
    @Field(ApiParams.ROLE) role: String,
  ): ClerkResult<OrganizationMembership, ClerkErrorResponse>

  /**
   * Removes a member from an organization.
   *
   * @param organizationId The unique identifier of the organization
   * @param userId The unique identifier of the user to remove from the organization
   * @return A [ClerkResult] containing either the removed [OrganizationMembership] on success or a
   *   [ClerkErrorResponse] on failure
   * @see com.clerk.api.organizations.removeMember
   */
  @DELETE(ApiPaths.Organization.MEMBERSHIP_WITH_USER_ID)
  suspend fun removeMember(
    @Path(ApiParams.ORGANIZATION_ID) organizationId: String,
    @Path(ApiParams.USER_ID) userId: String,
  ): ClerkResult<OrganizationMembership, ClerkErrorResponse>

  /**
   * Create an invitation for a user to join an organization.
   *
   * @param organizationId The id of the organization for which the invitation will be created
   * @param emailAddress The email address the invitation will be sent to.
   * @param role The role that will be assigned to the user after joining. This can be one of the
   *   predefined roles "org:admin", "org:member" or a custom role.
   */
  @FormUrlEncoded
  @POST(ApiPaths.Organization.Invitations.BASE)
  suspend fun createInvitation(
    @Path(ApiParams.ORGANIZATION_ID) organizationId: String,
    @Field("email_address") emailAddress: String,
    @Field(ApiParams.ROLE) role: String,
  ): ClerkResult<OrganizationInvitation, ClerkErrorResponse>

  /**
   * Retrieve all invitations for an organization. The current user must have permissions to manage
   * the members of the organization.
   *
   * @param organizationId The id of the organization for which the invitation will be retrieved.
   * @param limit
   * @param offset
   * @param status
   */
  @GET(ApiPaths.Organization.Invitations.BASE)
  suspend fun getAllInvitations(
    @Path(ApiParams.ORGANIZATION_ID) organizationId: String,
    @Query(ApiParams.LIMIT) limit: Int? = null,
    @Query(ApiParams.OFFSET) offset: Int? = null,
    @Query("status") status: String? = null,
  ): ClerkResult<ClerkPaginatedResponse<OrganizationInvitation>, ClerkErrorResponse>

  /**
   * Bulk create an invitation for a user to join an organization.
   *
   * The current user must have permissions to manage the members of the organization.
   *
   * @param organizationId The id of the organization for which the invitations will be created.
   * @param emailAddresses An array of email addresses the invitations will be sent to.
   * @param role The role that will be assigned to each of the users after joining. This can be one
   *   of the predefined roles (org:admin, org:basic_member) or a custom role.
   */
  @FormUrlEncoded
  @POST(ApiPaths.Organization.Invitations.BULK_CREATE)
  suspend fun bulkCreateInvitations(
    @Path(ApiParams.ORGANIZATION_ID) organizationId: String,
    @Field("email_address") emailAddresses: List<String>,
    @Field("role") role: String,
  ): ClerkResult<List<OrganizationInvitation>, ClerkErrorResponse>

  /**
   * Revoke a pending organization invitation.
   *
   * The current user must have permissions to manage the members of the organization.
   *
   * @param organizationId The id of the organization for which the invitations will be retrieved.
   * @param invitationId The id of the invitation to be revoked.
   */
  @POST(ApiPaths.Organization.Invitations.REVOKE)
  suspend fun revokeOrganizationInvitation(
    @Path(ApiParams.ORGANIZATION_ID) organizationId: String,
    @Path(ApiParams.INVITATION_ID) invitationId: String,
  ): ClerkResult<OrganizationInvitation, ClerkErrorResponse>
}
