package com.clerk.network.api

import com.clerk.network.model.environment.Environment
import com.clerk.network.model.error.ClerkErrorResponse
import com.clerk.network.paths.Paths
import com.clerk.network.serialization.ClerkResult
import retrofit2.http.GET

internal interface EnvironmentApi {
  @GET(Paths.ENVIRONMENT) suspend fun get(): ClerkResult<Environment, ClerkErrorResponse>
}
