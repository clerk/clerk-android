package com.clerk.network.api

import com.clerk.network.model.error.ClerkErrorResponse
import com.clerk.network.paths.CommonParams
import com.clerk.network.paths.Paths
import com.clerk.network.serialization.ClerkResult
import com.clerk.signin.SignIn
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
  @POST(Paths.ClientPath.SignInPath.SIGN_INS)
  suspend fun createSignIn(
    @FieldMap params: Map<String, String>
  ): ClerkResult<SignIn, ClerkErrorResponse>

  /** @see SignIn.authenticateWithRedirect */
  @FormUrlEncoded
  @POST(Paths.ClientPath.SignInPath.SIGN_INS)
  suspend fun authenticateWithGoogle(
    @Field(CommonParams.STRATEGY) strategy: String = "google_one_tap",
    @Field("token") token: String,
  ): ClerkResult<SignIn, ClerkErrorResponse>

  /** @see SignIn.authenticateWithRedirect */
  @FormUrlEncoded
  @POST(Paths.ClientPath.SignInPath.SIGN_INS)
  suspend fun authenticateWithRedirect(
    @FieldMap params: Map<String, String>
  ): ClerkResult<SignIn, ClerkErrorResponse>

  @GET(Paths.ClientPath.SignInPath.WithId.SIGN_INS_WITH_ID)
  suspend fun fetchSignIn(
    @Path(CommonParams.ID) id: String,
    @Query("rotating_token_nonce") rotatingTokenNonce: String? = null,
  ): ClerkResult<SignIn, ClerkErrorResponse>

  @FormUrlEncoded
  @POST(Paths.ClientPath.SignInPath.WithId.ATTEMPT_FIRST_FACTOR)
  suspend fun attemptFirstFactor(
    @Path(CommonParams.ID) id: String,
    @FieldMap params: Map<String, String>,
  ): ClerkResult<SignIn, ClerkErrorResponse>

  @FormUrlEncoded
  @POST(Paths.ClientPath.SignInPath.WithId.ATTEMPT_SECOND_FACTOR)
  suspend fun attemptSecondFactor(
    @Path(CommonParams.ID) id: String,
    @FieldMap params: Map<String, String>,
  ): ClerkResult<SignIn, ClerkErrorResponse>

  @FormUrlEncoded
  @POST(Paths.ClientPath.SignInPath.WithId.PREPARE_FIRST_FACTOR)
  suspend fun prepareSignInFirstFactor(
    @Path(CommonParams.ID) id: String,
    @FieldMap fields: Map<String, String>,
  ): ClerkResult<SignIn, ClerkErrorResponse>

  /**
   * Prepare the second factor for a sign in.
   *
   * @param id The session id.
   * @param params The parameters for the second factor. @see [SignIn.PrepareSecondFactorParams]
   */
  @FormUrlEncoded
  @POST(Paths.ClientPath.SignInPath.WithId.PREPARE_SECOND_FACTOR)
  suspend fun prepareSecondFactor(
    @Path(CommonParams.ID) id: String,
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
  @POST(Paths.ClientPath.SignInPath.WithId.RESET_PASSWORD)
  suspend fun resetPassword(
    @Path(CommonParams.ID) id: String,
    @Field("password") password: String,
    @Field("sign_out_of_other_sessions") signOutOfOtherSessions: Boolean,
  ): ClerkResult<SignIn, ClerkErrorResponse>
}
