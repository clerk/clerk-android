package com.clerk.api.network.api

import com.clerk.api.network.ApiPaths
import com.clerk.api.network.middleware.ManualClientSyncRequest
import com.clerk.api.network.middleware.ResponseGuard
import com.clerk.api.network.middleware.SensitiveRequest
import com.clerk.api.network.middleware.outgoing.INTERNAL_HEADER_SKIP_CLIENT_ID
import com.clerk.api.network.middleware.outgoing.INTERNAL_HEADER_TRUE
import com.clerk.api.network.model.client.Client
import com.clerk.api.network.model.error.ClerkErrorResponse
import com.clerk.api.network.model.hostedauth.HostedAuthResource
import com.clerk.api.network.serialization.ClerkResult
import com.clerk.api.session.Session
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Tag

internal const val SET_ACTIVE_INTENT_SELECT_ORG = "select_org"

internal interface ClientApi {
  @FormUrlEncoded
  @POST(ApiPaths.Client.HostedAuth.BASE)
  suspend fun createHostedAuth(
    @Field("redirect_url") redirectUrl: String,
    @Field("code_challenge") codeChallenge: String,
    @Field("state") state: String,
    @Field("mode") mode: String? = null,
    @Header(INTERNAL_HEADER_SKIP_CLIENT_ID) skipClientId: String? = null,
    @Tag sensitiveRequest: SensitiveRequest = SensitiveRequest,
    @Tag responseGuard: ResponseGuard = ResponseGuard.always,
  ): ClerkResult<HostedAuthResource, ClerkErrorResponse>

  @FormUrlEncoded
  @POST(ApiPaths.Client.BASE)
  suspend fun redeemHostedAuth(
    @Field("_method") method: String = "GET",
    @Field("rotating_token_nonce") rotatingTokenNonce: String,
    @Field("code_verifier") codeVerifier: String,
    @Tag manualClientSyncRequest: ManualClientSyncRequest,
    @Tag sensitiveRequest: SensitiveRequest = SensitiveRequest,
  ): ClerkResult<Client, ClerkErrorResponse>

  @GET(ApiPaths.Client.BASE) suspend fun get(): ClerkResult<Client, ClerkErrorResponse>

  @GET(ApiPaths.Client.BASE)
  suspend fun getSkippingClientId(
    @Header(INTERNAL_HEADER_SKIP_CLIENT_ID) skipClientId: String = INTERNAL_HEADER_TRUE
  ): ClerkResult<Client, ClerkErrorResponse>

  @FormUrlEncoded
  @POST(ApiPaths.Client.Sessions.SET_ACTIVE)
  suspend fun setActive(
    @Path("id") sessionId: String,
    @Field("active_organization_id") organizationId: String,
    @Field("intent") intent: String = SET_ACTIVE_INTENT_SELECT_ORG,
  ): ClerkResult<Session, ClerkErrorResponse>
}
