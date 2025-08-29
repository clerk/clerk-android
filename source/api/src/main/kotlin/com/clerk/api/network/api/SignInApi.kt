package com.clerk.api.network.api

import com.clerk.api.network.model.error.ClerkErrorResponse
import com.clerk.api.network.ApiParams
import com.clerk.api.network.ApiPaths
import com.clerk.api.network.serialization.ClerkResult
import com.clerk.api.signin.SignIn
import retrofit2.http.Field
import retrofit2.http.FieldMap
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

internal interface SignInApi {
  /**
   * @param params The parameters for the sign in. @see [SignIn.SignInCreateParams]
   * @see SignIn.create
   */
  @FormUrlEncoded
  @POST(ApiPaths.Client.SignIn.BASE)
  suspend fun createSignIn(
    @FieldMap params: Map<String, String>
  ): ClerkResult<SignIn, ClerkErrorResponse>

  /** @see SignIn.authenticateWithRedirect */
  @FormUrlEncoded
  @POST(ApiPaths.Client.SignIn.BASE)
  suspend fun authenticateWithGoogle(
    @Field(ApiParams.STRATEGY) strategy: String = "google_one_tap",
    @Field("token") token: String,
  ): ClerkResult<SignIn, ClerkErrorResponse>

  /** @see SignIn.authenticateWithRedirect */
  @FormUrlEncoded
  @POST(ApiPaths.Client.SignIn.BASE)
  suspend fun authenticateWithRedirect(
    @Field("strategy") strategy: String,
    @Field("redirect_url") redirectUrl: String?,
    @Field("identifier") identifier: String? = null,
    @Field("email_address") emailAddress: String? = null,
    @Field("legal_accepted") legalAccepted: Boolean? = null,
  ): ClerkResult<SignIn, ClerkErrorResponse>

  @GET(ApiPaths.Client.SignIn.WITH_ID)
  suspend fun fetchSignIn(
    @Path(ApiParams.ID) id: String,
    @Query("rotating_token_nonce") rotatingTokenNonce: String? = null,
  ): ClerkResult<SignIn, ClerkErrorResponse>

  @FormUrlEncoded
  @POST(ApiPaths.Client.SignIn.ATTEMPT_FIRST_FACTOR)
  suspend fun attemptFirstFactor(
    @Path(ApiParams.ID) id: String,
    @FieldMap params: Map<String, String>,
  ): ClerkResult<SignIn, ClerkErrorResponse>

  @FormUrlEncoded
  @POST(ApiPaths.Client.SignIn.ATTEMPT_SECOND_FACTOR)
  suspend fun attemptSecondFactor(
    @Path(ApiParams.ID) id: String,
    @FieldMap params: Map<String, String>,
  ): ClerkResult<SignIn, ClerkErrorResponse>

  @FormUrlEncoded
  @POST(ApiPaths.Client.SignIn.PREPARE_FIRST_FACTOR)
  suspend fun prepareSignInFirstFactor(
    @Path(ApiParams.ID) id: String,
    @FieldMap fields: Map<String, String>,
  ): ClerkResult<SignIn, ClerkErrorResponse>

  /**
   * Prepare the second factor for a sign in.
   *
   * @param id The session id.
   * @param params The parameters for the second factor. @see [SignIn.PrepareSecondFactorParams]
   */
  @FormUrlEncoded
  @POST(ApiPaths.Client.SignIn.PREPARE_SECOND_FACTOR)
  suspend fun prepareSecondFactor(
    @Path(ApiParams.ID) id: String,
    @FieldMap params: Map<String, String>,
  ): ClerkResult<SignIn, ClerkErrorResponse>

  /**
   * Reset the password for a sign in.
   *
   * The request body should contain the reset password fields as key-value pairs. The expected
   *
   * @param id The session id.
   * @param password The new password.
   * @param signOutOfOtherSessions Whether to sign out of other sessions.
   */
  @FormUrlEncoded
  @POST(ApiPaths.Client.SignIn.RESET_PASSWORD)
  suspend fun resetPassword(
    @Path(ApiParams.ID) id: String,
    @Field("password") password: String,
    @Field("sign_out_of_other_sessions") signOutOfOtherSessions: Boolean,
  ): ClerkResult<SignIn, ClerkErrorResponse>
}
