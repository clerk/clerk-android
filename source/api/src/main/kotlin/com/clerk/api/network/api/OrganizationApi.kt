package com.clerk.api.network.api

import com.clerk.api.network.paths.Organizations
import retrofit2.http.GET

interface OrganizationApi {

  @GET(Organizations.Roles.GET_ROLES) fun getRoles(): List<Role>
}
