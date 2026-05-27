package com.clerk.api.network.api

import com.clerk.api.network.ApiPaths
import com.clerk.api.network.model.error.ClerkErrorResponse
import com.clerk.api.network.model.magiclink.NativeMagicLinkCompleteResponse
import com.clerk.api.network.serialization.ClerkResult
import retrofit2.http.FieldMap
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST

internal interface MagicLinkApi {
  @FormUrlEncoded
  @POST(ApiPaths.Client.MagicLinks.COMPLETE)
  suspend fun complete(
    @FieldMap fields: Map<String, String>
  ): ClerkResult<NativeMagicLinkCompleteResponse, ClerkErrorResponse>
}
