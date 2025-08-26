package com.clerk.api.network.api

import com.clerk.api.network.model.error.ClerkErrorResponse
import com.clerk.api.network.paths.Paths
import com.clerk.api.network.serialization.ClerkResult
import com.clerk.api.organizations.OrganizationInvitation
import com.clerk.api.organizations.OrganizationSuggestion
import com.clerk.api.organizations.Role
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface OrganizationApi {

  @GET(Paths.Organizations.ROLES)
  suspend fun getRoles(
    @Path("organization_id") organizationId: String,
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
  suspend fun createOrganization(@Field("name") name: String)
}
