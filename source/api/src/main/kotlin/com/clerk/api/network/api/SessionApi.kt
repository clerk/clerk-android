package com.clerk.api.network.api

import com.clerk.api.network.ApiParams
import com.clerk.api.network.ApiPaths
import com.clerk.api.network.model.client.Client
import com.clerk.api.network.model.error.ClerkErrorResponse
import com.clerk.api.network.model.token.TokenResource
import com.clerk.api.network.serialization.ClerkResult
import com.clerk.api.session.Session
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Internal API interface for session management operations.
 *
 * This interface defines the REST API endpoints for managing user sessions in the Clerk system.
 * Sessions represent authenticated user states and contain tokens, user information, and
 * authentication metadata.
 *
 * This is an internal API interface used by the Clerk SDK and should not be used directly by
 * application code.
 */
internal interface SessionApi {
  /**
   * Retrieves all sessions for the current client.
   *
   * This method fetches information about all sessions associated with the current client,
   * including both active and inactive sessions.
   *
   * @return A [ClerkResult] with Unit on success, or a [ClerkErrorResponse] on failure
   */
  @GET(ApiPaths.Client.Sessions.BASE) suspend fun sessions(): ClerkResult<Unit, ClerkErrorResponse>

  /**
   * Removes a specific session from the client.
   *
   * This method removes the specified session from the client's active sessions list. The session
   * will no longer be available for authentication purposes.
   *
   * @param id The unique identifier of the session to remove
   * @return A [ClerkResult] containing the removed [Session] on success, or a [ClerkErrorResponse]
   *   on failure
   */
  @POST(ApiPaths.Client.Sessions.REMOVE)
  suspend fun removeSession(
    @Path(ApiParams.ID) id: String
  ): ClerkResult<Session, ClerkErrorResponse>

  /**
   * Deletes all sessions for the current client.
   *
   * This method removes all sessions associated with the current client, effectively signing out
   * the user from all devices and sessions.
   *
   * @return A [ClerkResult] containing the updated [Client] on success, or a [ClerkErrorResponse]
   *   on failure
   */
  @DELETE(ApiPaths.Client.Sessions.BASE)
  suspend fun deleteSessions(): ClerkResult<Client, ClerkErrorResponse>

  /**
   * Retrieves tokens for a specific session.
   *
   * This method fetches authentication tokens associated with the specified session, which can be
   * used for API authentication and authorization.
   *
   * @param sessionId The unique identifier of the session to get tokens for
   * @return A [ClerkResult] containing the [TokenResource] on success, or a [ClerkErrorResponse] on
   *   failure
   */
  @POST(ApiPaths.Client.Sessions.TOKENS)
  suspend fun tokens(
    @Path(ApiParams.ID) sessionId: String
  ): ClerkResult<TokenResource, ClerkErrorResponse>

  /**
   * Retrieves tokens for a specific user and template type.
   *
   * This method fetches authentication tokens for a specific user using a template type, which
   * allows for customized token generation based on the template configuration.
   *
   * @param userId The unique identifier of the user
   * @param templateType The type of template to use for token generation
   * @return A [ClerkResult] containing the [TokenResource] on success, or a [ClerkErrorResponse] on
   *   failure
   */
  @POST(ApiPaths.Client.Sessions.TOKEN_TEMPLATE)
  suspend fun tokens(
    @Path(ApiParams.ID) userId: String,
    @Path("template") templateType: String,
  ): ClerkResult<TokenResource, ClerkErrorResponse>

  /**
   * Revokes a specific session.
   *
   * This method revokes the specified session, making it invalid for future authentication. The
   * revoked session will no longer be usable for API calls or authentication.
   *
   * @param sessionId Optional session ID of the current session making the request
   * @param sessionIdToRevoke The unique identifier of the session to revoke
   * @return A [ClerkResult] containing the revoked [Session] on success, or a [ClerkErrorResponse]
   *   on failure
   */
  @POST(ApiPaths.User.Sessions.REVOKE)
  suspend fun revokeSession(
    @Query(ApiParams.CLERK_SESSION_ID) sessionId: String? = null,
    @Path("session_id") sessionIdToRevoke: String,
  ): ClerkResult<Session, ClerkErrorResponse>
}
