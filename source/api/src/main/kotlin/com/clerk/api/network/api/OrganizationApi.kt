package com.clerk.api.network.api

import com.clerk.api.network.model.error.ClerkErrorResponse
import com.clerk.api.network.paths.Paths
import com.clerk.api.network.serialization.ClerkResult
import com.clerk.api.organizations.Role
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface OrganizationApi {

  @GET(Paths.Organizations.ROLES)
  fun roles(
    @Path("organization_id") organizationId: String,
    @Query("limit") limit: Int? = null,
    @Query("offset") offset: Int? = null,
  ): ClerkResult<List<Role>, ClerkErrorResponse>
}
