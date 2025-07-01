package com.clerk.network.api

import com.clerk.network.model.client.Client
import com.clerk.network.model.error.ClerkErrorResponse
import com.clerk.network.paths.Paths
import com.clerk.network.serialization.ClerkResult
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST

internal interface DeviceAttestationApi {
  @POST(Paths.ClientPath.DeviceAttestation.CHALLENGES) fun getChallenge()

  /** Verifies the given [token] for app attestation */
  @POST(Paths.ClientPath.DeviceAttestation.VERIFY)
  @FormUrlEncoded
  suspend fun verify(
    @Field("package_name") packageName: String,
    @Field("token") token: String,
    @Field("platform") platform: String = "android",
  ): ClerkResult<Client, ClerkErrorResponse>
}
