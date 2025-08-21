package com.clerk.api.network.api

import com.clerk.api.network.model.error.ClerkErrorResponse
import com.clerk.api.network.model.roles.Role
import com.clerk.api.network.paths.Organizations
import com.clerk.api.network.serialization.ClerkResult
import retrofit2.http.GET

interface OrganizationApi {

  @GET(Organizations.Roles.GET_ROLES) fun getRoles(): ClerkResult<List<Role>, ClerkErrorResponse>
}
