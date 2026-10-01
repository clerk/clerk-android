package com.clerk.api.network.api

import com.clerk.api.Clerk
import com.clerk.api.network.ApiParams
import com.clerk.api.network.ApiPaths
import com.clerk.api.network.model.client.Client
import com.clerk.api.network.model.error.ClerkErrorResponse
import com.clerk.api.network.model.token.TokenResource
import com.clerk.api.network.serialization.ClerkResult
import com.clerk.api.session.Session
import com.clerk.api.session.SessionVerification
import retrofit2.http.DELETE
import retrofit2.http.Field
import retrofit2.http.FieldMap
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

@Suppress("TooManyFunctions")
internal interface SessionApi {
  @GET(ApiPaths.Client.Sessions.BASE) suspend fun sessions(): ClerkResult<Unit, ClerkErrorResponse>

  @POST(ApiPaths.Client.Sessions.REMOVE)
  suspend fun removeSession(
    @Path(ApiParams.ID) id: String
  ): ClerkResult<Session, ClerkErrorResponse>

  @DELETE(ApiPaths.Client.Sessions.BASE)
  suspend fun deleteSessions(): ClerkResult<Client, ClerkErrorResponse>

  @POST(ApiPaths.Client.Sessions.TOKENS)
  @FormUrlEncoded
  suspend fun tokens(
    @Path(ApiParams.ID) sessionId: String,
    @Field("organization_id") organizationId: String = "",
    @Field("token") token: String? = null,
    @Field("force_origin") forceOrigin: String? = null,
  ): ClerkResult<TokenResource, ClerkErrorResponse>

  @POST(ApiPaths.Client.Sessions.TOKEN_TEMPLATE)
  suspend fun tokens(
    @Path(ApiParams.ID) userId: String,
    @Path("template") templateType: String,
  ): ClerkResult<TokenResource, ClerkErrorResponse>

  @FormUrlEncoded
  @POST(ApiPaths.Client.Sessions.VERIFY)
  suspend fun startVerification(
    @Path(ApiParams.ID) sessionId: String,
    @FieldMap params: Map<String, String>,
  ): ClerkResult<SessionVerification, ClerkErrorResponse>

  @FormUrlEncoded
  @POST(ApiPaths.Client.Sessions.PREPARE_FIRST_FACTOR)
  suspend fun prepareFirstFactorVerification(
    @Path(ApiParams.ID) sessionId: String,
    @FieldMap params: Map<String, String>,
  ): ClerkResult<SessionVerification, ClerkErrorResponse>

  @FormUrlEncoded
  @POST(ApiPaths.Client.Sessions.ATTEMPT_FIRST_FACTOR)
  suspend fun attemptFirstFactorVerification(
    @Path(ApiParams.ID) sessionId: String,
    @FieldMap params: Map<String, String>,
  ): ClerkResult<SessionVerification, ClerkErrorResponse>

  @FormUrlEncoded
  @POST(ApiPaths.Client.Sessions.PREPARE_SECOND_FACTOR)
  suspend fun prepareSecondFactorVerification(
    @Path(ApiParams.ID) sessionId: String,
    @FieldMap params: Map<String, String>,
  ): ClerkResult<SessionVerification, ClerkErrorResponse>

  @FormUrlEncoded
  @POST(ApiPaths.Client.Sessions.ATTEMPT_SECOND_FACTOR)
  suspend fun attemptSecondFactorVerification(
    @Path(ApiParams.ID) sessionId: String,
    @FieldMap params: Map<String, String>,
  ): ClerkResult<SessionVerification, ClerkErrorResponse>

  /**
   * Revokes a specific session.
   *
   * This method revokes the specified session, making it invalid for future authentication. The
   * revoked session will no longer be usable for API calls or authentication.
   *
   * @param sessionIdToRevoke The unique identifier of the session to revoke
   * @param sessionId The session making the request; defaults to the active session
   * @return A [ClerkResult] containing the revoked [Session] on success, or a [ClerkErrorResponse]
   *   on failure
   */
  @POST(ApiPaths.User.Sessions.REVOKE)
  suspend fun revokeSession(
    @Path("session_id") sessionIdToRevoke: String,
    @Query(ApiParams.CLERK_SESSION_ID) sessionId: String? = Clerk.session?.id,
  ): ClerkResult<Session, ClerkErrorResponse>
}
