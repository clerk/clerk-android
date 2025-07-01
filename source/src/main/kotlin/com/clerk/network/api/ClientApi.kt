package com.clerk.network.api

import com.clerk.network.model.client.Client
import com.clerk.network.model.error.ClerkErrorResponse
import com.clerk.network.paths.Paths
import com.clerk.network.serialization.ClerkResult
import retrofit2.http.GET

/**
 * Internal API interface for client-related operations.
 *
 * This interface defines the REST API endpoints for retrieving and managing client information
 * in the Clerk system. The client represents the current device/application instance and contains
 * session information, authentication state, and device-specific data.
 *
 * This is an internal API interface used by the Clerk SDK and should not be used directly
 * by application code.
 */
internal interface ClientApi {
  /**
   * Retrieves the current client information.
   *
   * This method fetches the complete client object from the Clerk API, which includes
   * information about active sessions, sign-in attempts, sign-up attempts, and other
   * client-specific data.
   *
   * @return A [ClerkResult] containing the [Client] object on success, or a [ClerkErrorResponse] on failure
   */
  @GET(Paths.ClientPath.CLIENT) suspend fun get(): ClerkResult<Client, ClerkErrorResponse>
}
