package com.clerk.api.network.api

import com.clerk.api.network.ApiPaths
import com.clerk.api.network.model.environment.Environment
import com.clerk.api.network.model.error.ClerkErrorResponse
import com.clerk.api.network.serialization.ClerkResult
import retrofit2.http.GET

internal interface EnvironmentApi {
  @GET(ApiPaths.ENVIRONMENT) suspend fun get(): ClerkResult<Environment, ClerkErrorResponse>
}
