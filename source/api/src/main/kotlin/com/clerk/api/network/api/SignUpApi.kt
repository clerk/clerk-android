package com.clerk.api.network.api

import com.clerk.api.network.ApiParams
import com.clerk.api.network.ApiPaths
import com.clerk.api.network.model.error.ClerkErrorResponse
import com.clerk.api.network.serialization.ClerkResult
import com.clerk.api.signup.SignUp
import retrofit2.http.Field
import retrofit2.http.FieldMap
import retrofit2.http.FormUrlEncoded
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path

internal interface SignUpApi {
  /** @see [SignUp.create] */
  @FormUrlEncoded
  @POST(ApiPaths.Client.SignUp.BASE)
  suspend fun createSignUp(
    @FieldMap fields: Map<String, String>
  ): ClerkResult<SignUp, ClerkErrorResponse>

  @FormUrlEncoded
  @PATCH(ApiPaths.Client.SignUp.WITH_ID)
  suspend fun updateSignUp(
    @Path(ApiParams.ID) id: String,
    @FieldMap fields: Map<String, String>,
  ): ClerkResult<SignUp, ClerkErrorResponse>

  /** @see [com.clerk.signup.prepareVerification] */
  @FormUrlEncoded
  @POST(ApiPaths.Client.SignUp.PREPARE_VERIFICATION)
  suspend fun prepareSignUpVerification(
    @Path(ApiParams.ID) signUpId: String,
    @Field(ApiParams.STRATEGY) strategy: String,
  ): ClerkResult<SignUp, ClerkErrorResponse>

  /** @see [com.clerk.signup.attemptVerification] */
  @FormUrlEncoded
  @POST(ApiPaths.Client.SignUp.ATTEMPT_VERIFICATION)
  suspend fun attemptSignUpVerification(
    @Path(ApiParams.ID) signUpId: String,
    @Field(ApiParams.STRATEGY) strategy: String,
    @Field(ApiParams.CODE) code: String,
  ): ClerkResult<SignUp, ClerkErrorResponse>
}
