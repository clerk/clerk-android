package com.clerk.network.api

import com.clerk.network.model.client.Client
import com.clerk.network.model.error.ClerkErrorResponse
import com.clerk.network.model.token.TokenResource
import com.clerk.network.paths.CommonParams
import com.clerk.network.paths.Paths
import com.clerk.network.serialization.ClerkResult
import com.clerk.session.Session
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

internal interface SessionApi {
  @GET(Paths.ClientPath.Sessions.SESSIONS)
  suspend fun sessions(): ClerkResult<Unit, ClerkErrorResponse>

  @POST(Paths.ClientPath.Sessions.WithId.REMOVE)
  suspend fun removeSession(
    @Path(CommonParams.ID) id: String
  ): ClerkResult<Session, ClerkErrorResponse>

  @DELETE(Paths.ClientPath.Sessions.SESSIONS)
  suspend fun deleteSessions(): ClerkResult<Client, ClerkErrorResponse>

  @POST(Paths.ClientPath.Sessions.WithId.TOKENS)
  suspend fun tokens(
    @Path(CommonParams.ID) sessionId: String
  ): ClerkResult<TokenResource, ClerkErrorResponse>

  @POST(Paths.ClientPath.Sessions.WithId.TEMPLATE)
  suspend fun tokens(
    @Path(CommonParams.ID) userId: String,
    @Path("template") templateType: String,
  ): ClerkResult<TokenResource, ClerkErrorResponse>

  @POST(Paths.UserPath.Sessions.REVOKE)
  suspend fun revokeSession(
    @Query(CommonParams.CLERK_SESSION_ID) sessionId: String? = null,
    @Path("session_id") sessionIdToRevoke: String,
  ): ClerkResult<Session, ClerkErrorResponse>
}
