package com.clerk.sdk.network

import com.clerk.sdk.model.client.Client
import com.clerk.sdk.model.environment.Environment
import com.clerk.sdk.model.error.ClerkErrorResponse
import com.clerk.sdk.model.response.ClerkResponse
import com.clerk.sdk.model.response.ClientPiggybackedResponse
import com.clerk.sdk.model.session.Session
import com.clerk.sdk.model.signin.SignIn
import com.clerk.sdk.model.signup.SignUp
import com.clerk.sdk.model.token.TokenResource
import com.clerk.sdk.network.paths.Paths
import com.clerk.sdk.network.requests.Requests
import com.slack.eithernet.ApiResult
import com.slack.eithernet.DecodeErrorBody
import retrofit2.http.Field
import retrofit2.http.FieldMap
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * ClerkApiService is an interface that defines the API endpoints for the Clerk client.
 *
 * When the endpoints are called they'll come back as a [ClerkResponse], where the type will either
 * be the type specified or [com.clerk.sdk.model.error.ClerkErrorResponse] if there was an error.
 *
 * To handle the response:
 * ```kotlin
 * when(val result = clientApi.client()) {
 *   is Success ->  result.data // handle success
 *   is Failure -> result.errorResponse
 * }
 * ```
 */
@Suppress("TooManyFunctions")
internal interface ClerkApiService {

  // region Client

  @GET(Paths.ClientPath.CLIENT)
  suspend fun client(): ApiResult<ClientPiggybackedResponse<Client>, ClerkErrorResponse>

  // endregion

  // region Device Attestation

  @GET(Paths.ClientPath.DeviceAttestation.DEVICE_ATTESTATION) suspend fun deviceAttestation()

  @POST(Paths.ClientPath.DeviceAttestation.CHALLENGES) suspend fun challenges(): ClerkResponse<Unit>

  @POST(Paths.ClientPath.DeviceAttestation.VERIFY) suspend fun verify(): ClerkResponse<Unit>

  // endregion

  // region Session

  @GET(Paths.ClientPath.Sessions.SESSIONS) suspend fun sessions(): ClerkResponse<Unit>

  @FormUrlEncoded
  @POST(Paths.ClientPath.Sessions.WithId.REMOVE)
  suspend fun remove(@Path("id") id: String, @Field("id") userId: String): ClerkResponse<Session>

  @FormUrlEncoded
  @POST(Paths.ClientPath.Sessions.WithId.TOKENS)
  suspend fun tokens(
    @Path("id") userId: String,
    @Field("id") id: String,
  ): ApiResult<TokenResource, ClerkErrorResponse>

  @FormUrlEncoded
  @POST(Paths.ClientPath.Sessions.WithId.TEMPLATE)
  suspend fun tokensTemplate(
    @Path("id") userId: String,
    @Path("template") templateType: String,
    @Field("id") id: String,
    @Field("template") template: String,
  ): ApiResult<TokenResource, ClerkErrorResponse>

  // endregion

  // region Sign In

  @POST(Paths.ClientPath.SignIns.SIGN_INS)
  suspend fun signIn(): ApiResult<SignIn, ClerkErrorResponse>

  @GET(Paths.ClientPath.SignIns.WithId.SIGN_INS_WITH_ID)
  suspend fun signIn(
    @Path("id") id: String,
    @Query("rotating_token_nonce") rotatingTokenNonce: String? = null,
  ): ApiResult<SignIn, ClerkErrorResponse>

  @POST(Paths.ClientPath.SignIns.WithId.ATTEMPT_FIRST_FACTOR)
  suspend fun attemptFirstFactor(@Path("id") id: String): ApiResult<SignIn, ClerkErrorResponse>

  @POST(Paths.ClientPath.SignIns.WithId.ATTEMPT_FIRST_FACTOR)
  suspend fun attemptSecondFactor(
    @Path("id") id: String,
    @Query("rotating_token_nonce") rotatingTokenNonce: String? = null,
  ): ApiResult<SignIn, ClerkErrorResponse>

  @POST(Paths.ClientPath.SignIns.WithId.PREPARE_FIRST_FACTOR)
  suspend fun prepareFirstFactor(
    @Path("id") id: String,
    @FieldMap fields: Map<String, String>,
  ): ApiResult<SignIn, ClerkErrorResponse>

  /**
   * Prepare the second factor for a sign in.
   *
   * @param id The session id.
   * @param params The parameters for the second
   *   factor. @see [Requests.SignIn.PrepareSecondFactorParams]
   */
  @POST(Paths.ClientPath.SignIns.WithId.PREPARE_SECOND_FACTOR)
  suspend fun prepareSecondFactor(
    @Path("id") id: String,
    @FieldMap params: Map<String, String>,
  ): ApiResult<SignIn, ClerkErrorResponse>

  /**
   * Reset the password for a sign in.
   *
   * The request body should contain the reset password fields as key-value pairs. The expected
   * input is [Requests.SignIn.ResetPasswordParams].
   */
  @POST(Paths.ClientPath.SignIns.WithId.RESET_PASSWORD)
  suspend fun resetPassword(@Path("id") id: String, @FieldMap fields: Map<String, String>)

  // endregion

  // region Environment

  // /environment
  @GET(Paths.ENVIRONMENT) suspend fun environment(): ApiResult<Environment, ClerkErrorResponse>

  // region Sign Up

  /**
   * Create a new sign up request.
   *
   * @see [SignUp.create]
   */
  @DecodeErrorBody
  @FormUrlEncoded
  @POST(Paths.SignUpPath.SIGN_UP)
  suspend fun createSignUp(
    @FieldMap fields: Map<String, String>
  ): ApiResult<ClientPiggybackedResponse<SignUp>, ClerkErrorResponse>

  /**
   * Update an ongoing sign up request.
   *
   * @see [updateSignUp]
   */
  @DecodeErrorBody
  @FormUrlEncoded
  @PATCH(Paths.SignUpPath.SIGN_UP)
  suspend fun updateSignUp(
    @Field("id") id: String,
    @FieldMap fields: Map<String, String>,
  ): ApiResult<ClientPiggybackedResponse<SignUp>, ClerkErrorResponse>

  /** @see [prepareSignUpVerification] */
  @DecodeErrorBody
  @FormUrlEncoded
  @POST(Paths.SignUpPath.WithId.PREPARE_VERIFICATION)
  suspend fun prepareSignUpVerification(
    @Path("id") signUpId: String,
    @Field("strategy") strategy: String,
  ): ApiResult<ClientPiggybackedResponse<SignUp>, ClerkErrorResponse>

  /** @see [attemptSignUpVerification] */
  @FormUrlEncoded
  @POST(Paths.SignUpPath.WithId.ATTEMPT_VERIFICATION)
  suspend fun attemptSignUpVerification(
    @Path("id") signUpId: String,
    @Field("strategy") strategy: String,
    @Field("code") code: String,
  ): ApiResult<ClientPiggybackedResponse<SignUp>, ClerkErrorResponse>

  // endregion
}
