package com.clerk.api.network.api

import com.clerk.api.network.ApiPaths
import com.clerk.api.network.model.client.Client
import com.clerk.api.network.model.error.ClerkErrorResponse
import com.clerk.api.network.serialization.ClerkResult
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST

internal interface DeviceAttestationApi {
  @POST(ApiPaths.Client.DeviceAttestation.CHALLENGES) fun getChallenge()

  /** Verifies the given [token] for app attestation */
  @POST(ApiPaths.Client.DeviceAttestation.VERIFY)
  @FormUrlEncoded
  suspend fun verify(
    @Field("package_name") packageName: String,
    @Field("token") token: String,
    @Field("platform") platform: String = "android",
  ): ClerkResult<Client, ClerkErrorResponse>
}
