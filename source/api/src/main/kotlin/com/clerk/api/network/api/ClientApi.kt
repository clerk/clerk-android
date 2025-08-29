package com.clerk.api.network.api

import com.clerk.api.network.model.client.Client
import com.clerk.api.network.model.error.ClerkErrorResponse
import com.clerk.api.network.ApiPaths
import com.clerk.api.network.serialization.ClerkResult
import com.clerk.api.session.Session
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

/**
 * Internal API interface for client-related operations.
 *
 * This interface defines the REST API endpoints for retrieving and managing client information in
 * the Clerk system. The client represents the current device/application instance and contains
 * session information, authentication state, and device-specific data.
 *
 * This is an internal API interface used by the Clerk SDK and should not be used directly by
 * application code.
 */
internal interface ClientApi {
  /**
   * Retrieves the current client information.
   *
   * This method fetches the complete client object from the Clerk API, which includes information
   * about active sessions, sign-in attempts, sign-up attempts, and other client-specific data.
   *
   * @return A [ClerkResult] containing the [Client] object on success, or a [ClerkErrorResponse] on
   *   failure
   */
  @GET(ApiPaths.Client.BASE) suspend fun get(): ClerkResult<Client, ClerkErrorResponse>

  /**
   * Sets a session as active for this client.
   *
   * This method activates a specific session on the current client, making it the primary
   * authenticated session. When a session is set as active, it becomes the session used for
   * subsequent API requests and authentication operations.
   *
   * @param sessionId The unique identifier of the session to activate
   * @param organizationId Optional organization ID to set as the active organization context for
   *   the session. If provided, the session will be scoped to this organization.
   * @return A [ClerkResult] with [Unit] on success, or a [ClerkErrorResponse] on failure
   */
  @FormUrlEncoded
  @POST(ApiPaths.Client.Sessions.SET_ACTIVE)
  suspend fun setActive(
    @Path("id") sessionId: String,
    @Field("active_organization_id") organizationId: String?,
  ): ClerkResult<Session, ClerkErrorResponse>
}
