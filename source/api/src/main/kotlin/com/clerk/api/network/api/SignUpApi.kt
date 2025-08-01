package com.clerk.api.network.api

import com.clerk.api.network.model.error.ClerkErrorResponse
import com.clerk.api.network.paths.CommonParams
import com.clerk.api.network.paths.Paths
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
  @POST(Paths.SignUpPath.SIGN_UP)
  suspend fun createSignUp(
    @FieldMap fields: Map<String, String>
  ): ClerkResult<SignUp, ClerkErrorResponse>

  @FormUrlEncoded
  @PATCH(Paths.SignUpPath.WithId.UPDATE)
  suspend fun updateSignUp(
    @Path(CommonParams.ID) id: String,
    @FieldMap fields: Map<String, String>,
  ): ClerkResult<SignUp, ClerkErrorResponse>

  /** @see [com.clerk.signup.prepareVerification] */
  @FormUrlEncoded
  @POST(Paths.SignUpPath.WithId.PREPARE_VERIFICATION)
  suspend fun prepareSignUpVerification(
    @Path(CommonParams.ID) signUpId: String,
    @Field(CommonParams.STRATEGY) strategy: String,
  ): ClerkResult<SignUp, ClerkErrorResponse>

  /** @see [com.clerk.signup.attemptVerification] */
  @FormUrlEncoded
  @POST(Paths.SignUpPath.WithId.ATTEMPT_VERIFICATION)
  suspend fun attemptSignUpVerification(
    @Path(CommonParams.ID) signUpId: String,
    @Field(CommonParams.STRATEGY) strategy: String,
    @Field(CommonParams.CODE) code: String,
  ): ClerkResult<SignUp, ClerkErrorResponse>
}
