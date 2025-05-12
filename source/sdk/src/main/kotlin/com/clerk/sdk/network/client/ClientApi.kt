package com.clerk.sdk.network.client

import com.clerk.sdk.model.client.Client
import com.clerk.sdk.model.error.ClerkAPIError
import com.clerk.sdk.network.CLIENT_BASE_URL
import com.slack.eithernet.ApiResult
import retrofit2.http.GET
import retrofit2.http.POST

private const val CLIENT_BASE_URL = "client"

/**
 * ClientApi is an interface that defines the API endpoints for the Clerk client.
 *
 * When the endpoints are called they'll come back as an ApiResult, which can be handled:
 * ```kotlin
 * when(val result = clientApi.client()) {
 *   is Success ->  result.response // handle success
 *   is Failure -> result.error // handle error, type is ClerkApiError
 *
 * }
 * ```
 */
internal object ClientServices {
  internal interface ClientApi {

    // /client
    @GET(CLIENT_BASE_URL) suspend fun client(): ApiResult<Client, ClerkAPIError>
  }

  internal interface DeviceAttestationApi {

    // /client/device_attestation
    @GET("${CLIENT_BASE_URL}/device_attestation") suspend fun deviceAttestation()

    // /client/device_attestation/challenges
    @POST("${CLIENT_BASE_URL}/device_attestation/challenges")
    suspend fun challenges(): ApiResult<Unit, ClerkAPIError>

    // /client/device_attestation/verify
    @POST("${CLIENT_BASE_URL}/device_attestation/verify")
    suspend fun verify(): ApiResult<Unit, ClerkAPIError>
  }

  internal interface SessionsApi {

    // /client/sessions
    @GET("${CLIENT_BASE_URL}/sessions") suspend fun sessions(): ApiResult<Unit, ClerkAPIError>

    // /client/sessions/verify
    @POST("${CLIENT_BASE_URL}/sessions/verify") suspend fun verify(): ApiResult<Unit, ClerkAPIError>
  }
}
