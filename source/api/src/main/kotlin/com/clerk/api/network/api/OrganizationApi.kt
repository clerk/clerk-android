package com.clerk.api.network.api

import com.clerk.api.network.model.error.ClerkErrorResponse
import com.clerk.api.network.paths.Paths
import com.clerk.api.network.serialization.ClerkResult
import com.clerk.api.organizations.Organization
import com.clerk.api.organizations.OrganizationInvitation
import com.clerk.api.organizations.OrganizationSuggestion
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

interface OrganizationApi {

  @GET(Paths.Organizations.ROLES)
  suspend fun getRoles(
    @Path(Paths.Organizations.ORGANIZATION_ID) organizationId: String,
    @Query("limit") limit: Int? = null,
    @Query("offset") offset: Int? = null,
  ): ClerkResult<List<Role>, ClerkErrorResponse>

  @POST(Paths.UserPath.ACCEPT_ORGANIZATION_INVITATION)
  suspend fun acceptUserOrganizationInvitation(
    @Path("invitation_id") invitationId: String,
    @Query("_clerk_session_id") sessionId: String? = null,
  ): ClerkResult<OrganizationInvitation, ClerkErrorResponse>

  @POST(Paths.UserPath.ACCEPT_ORGANIZATION_SUGGESTION)
  suspend fun acceptOrganizationSuggestion(
    @Path("suggestion_id") suggestionId: String,
    @Query("_clerk_session_id") sessionId: String? = null,
  ): ClerkResult<OrganizationSuggestion, ClerkErrorResponse>

  @FormUrlEncoded
  @POST(Paths.Organizations.ORGANIZATIONS)
  suspend fun createOrganization(
    @Field("name") name: String
  ): ClerkResult<Organization, ClerkErrorResponse>

  @GET(Paths.Organizations.WithId.ORGANIZATIONS_WITH_ID)
  fun getOrganization(
    @Path(Paths.Organizations.ORGANIZATION_ID) organizationId: String
  ): ClerkResult<Organization, ClerkErrorResponse>

  @FormUrlEncoded
  @PATCH(Paths.Organizations.WithId.ORGANIZATIONS_WITH_ID)
  suspend fun updateOrganization(
    @Path(Paths.Organizations.ORGANIZATION_ID) organizationId: String,
    @Field("name") name: String? = null,
    @Field("slug") slug: String? = null,
  ): ClerkResult<Organization, ClerkErrorResponse>

  @DELETE(Paths.Organizations.WithId.ORGANIZATIONS_WITH_ID)
  suspend fun deleteOrganization(
    @Path(Paths.Organizations.ORGANIZATION_ID) organizationId: String
  ): ClerkResult<Organization, ClerkErrorResponse>

  @Multipart
  @PUT(Paths.Organizations.WithId.LOGO)
  fun updateOrganizationLogo(
    @Path(Paths.Organizations.ORGANIZATION_ID) organizationId: String,
    @Part file: MultipartBody.Part,
  ): ClerkResult<Organization, ClerkErrorResponse>

  @DELETE(Paths.Organizations.WithId.LOGO)
  fun deleteOrganizationLogo(
    @Path(Paths.Organizations.ORGANIZATION_ID) organizationId: String
  ): ClerkResult<Organization, ClerkErrorResponse>
}
