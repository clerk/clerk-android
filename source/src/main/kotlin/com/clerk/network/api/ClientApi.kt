package com.clerk.network.api

import com.clerk.network.model.client.Client
import com.clerk.network.model.error.ClerkErrorResponse
import com.clerk.network.paths.Paths
import com.clerk.network.serialization.ClerkResult
import retrofit2.http.GET

internal interface ClientApi {
  @GET(Paths.ClientPath.CLIENT) suspend fun get(): ClerkResult<Client, ClerkErrorResponse>
}
