package com.clerk.sdk.network

import com.clerk.sdk.model.client.Client
import com.clerk.sdk.model.environment.Environment
import com.clerk.sdk.model.error.ClerkErrorResponse
import com.clerk.sdk.model.response.ClientPiggybackedResponse
import com.clerk.sdk.model.session.Session
import com.clerk.sdk.model.signin.SignIn
import com.clerk.sdk.model.signup.SignUp
import com.clerk.sdk.model.token.TokenResource
import com.clerk.sdk.network.paths.Paths
import com.clerk.sdk.network.requests.Requests
import com.clerk.sdk.network.serialization.ClerkApiResult
import retrofit2.http.DELETE
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
 * When the endpoints are called they'll come back as a [ClerkApiResult], where the type will either
 * be the type specified or [com.clerk.sdk.model.error.ClerkErrorResponse] if there was an error.
 *
 * To handle the response:
 * ```kotlin
 * when(val result = clientApi.client()) {
 *   is Success ->  result.data // handle success
 *   is Failure -> result.error
 * }
 * ```
 */
@Suppress("TooManyFunctions")
internal interface ClerkApiService {

  // region Client

  @GET(Paths.ClientPath.CLIENT)
  suspend fun client(): ClerkApiResult<ClientPiggybackedResponse<Client>, ClerkErrorResponse>

  // endregion

  // region Device Attestation

  @GET(Paths.ClientPath.DeviceAttestation.DEVICE_ATTESTATION) suspend fun deviceAttestation()

  @POST(Paths.ClientPath.DeviceAttestation.CHALLENGES)
  suspend fun challenges(): ClerkApiResult<Unit, ClerkErrorResponse>

  @POST(Paths.ClientPath.DeviceAttestation.VERIFY)
  suspend fun verify(): ClerkApiResult<Unit, ClerkErrorResponse>

  // endregion

  // region Sesszion

  @GET(Paths.ClientPath.Sessions.SESSIONS)
  suspend fun sessions(): ClerkApiResult<Unit, ClerkErrorResponse>

  @POST(Paths.ClientPath.Sessions.WithId.REMOVE)
  suspend fun removeSession(@Path("id") id: String): ClerkApiResult<Session, ClerkErrorResponse>

  @DELETE(Paths.ClientPath.Sessions.SESSIONS)
  suspend fun deleteSessions(): ClerkApiResult<Client, ClerkErrorResponse>

  @FormUrlEncoded
  @POST(Paths.ClientPath.Sessions.WithId.TOKENS)
  suspend fun tokens(
    @Path("id") userId: String,
    @Field("id") id: String,
  ): ClerkApiResult<TokenResource, ClerkErrorResponse>

  @FormUrlEncoded
  @POST(Paths.ClientPath.Sessions.WithId.TEMPLATE)
  suspend fun tokensTemplate(
    @Path("id") userId: String,
    @Path("template") templateType: String,
    @Field("id") id: String,
    @Field("template") template: String,
  ): ClerkApiResult<TokenResource, ClerkErrorResponse>

  // endregion

  // region Sign In
  @FormUrlEncoded
  @POST(Paths.ClientPath.SignInPath.SIGN_INS)
  suspend fun signIn(
    @Field("identifier") identifier: String
  ): ClerkApiResult<ClientPiggybackedResponse<SignIn>, ClerkErrorResponse>

  @GET(Paths.ClientPath.SignInPath.WithId.SIGN_INS_WITH_ID)
  suspend fun signIn(
    @Path("id") id: String,
    @Query("rotating_token_nonce") rotatingTokenNonce: String? = null,
  ): ClerkApiResult<ClientPiggybackedResponse<SignIn>, ClerkErrorResponse>

  @FormUrlEncoded
  @POST(Paths.ClientPath.SignInPath.WithId.ATTEMPT_FIRST_FACTOR)
  suspend fun attemptFirstFactor(
    @Path("id") id: String,
    @FieldMap params: Map<String, String>,
  ): ClerkApiResult<ClientPiggybackedResponse<SignIn>, ClerkErrorResponse>

  @POST(Paths.ClientPath.SignInPath.WithId.ATTEMPT_FIRST_FACTOR)
  suspend fun attemptSecondFactor(
    @Path("id") id: String,
    @Query("rotating_token_nonce") rotatingTokenNonce: String? = null,
  ): ClerkApiResult<ClientPiggybackedResponse<SignIn>, ClerkErrorResponse>

  @POST(Paths.ClientPath.SignInPath.WithId.PREPARE_FIRST_FACTOR)
  suspend fun prepareSignInFirstFactor(
    @Path("id") id: String,
    @FieldMap fields: Map<String, String>,
  ): ClerkApiResult<ClientPiggybackedResponse<SignIn>, ClerkErrorResponse>

  /**
   * Prepare the second factor for a sign in.
   *
   * @param id The session id.
   * @param params The parameters for the second
   *   factor. @see [Requests.SignInRequest.PrepareSecondFactorParams]
   */
  @POST(Paths.ClientPath.SignInPath.WithId.PREPARE_SECOND_FACTOR)
  suspend fun prepareSecondFactor(
    @Path("id") id: String,
    @FieldMap params: Map<String, String>,
  ): ClerkApiResult<ClientPiggybackedResponse<SignIn>, ClerkErrorResponse>

  /**
   * Reset the password for a sign in.
   *
   * The request body should contain the reset password fields as key-value pairs. The expected
   * input is [Requests.SignInRequest.ResetPasswordParams].
   */
  @POST(Paths.ClientPath.SignInPath.WithId.RESET_PASSWORD)
  suspend fun resetPassword(@Path("id") id: String, @FieldMap fields: Map<String, String>)

  // endregion

  // region Environment

  // /environment
  @GET(Paths.ENVIRONMENT) suspend fun environment(): ClerkApiResult<Environment, ClerkErrorResponse>

  // region Sign Up

  /** @see [SignUp.create] */
  @FormUrlEncoded
  @POST(Paths.SignUpPath.SIGN_UP)
  suspend fun createSignUp(
    @FieldMap fields: Map<String, String>
  ): ClerkApiResult<ClientPiggybackedResponse<SignUp>, ClerkErrorResponse>

  @FormUrlEncoded
  @PATCH(Paths.SignUpPath.SIGN_UP)
  suspend fun updateSignUp(
    @Field("id") id: String,
    @FieldMap fields: Map<String, String>,
  ): ClerkApiResult<ClientPiggybackedResponse<SignUp>, ClerkErrorResponse>

  /** @see [com.clerk.sdk.model.signup.prepareVerification] */
  @FormUrlEncoded
  @POST(Paths.SignUpPath.WithId.PREPARE_VERIFICATION)
  suspend fun prepareSignUpVerification(
    @Path("id") signUpId: String,
    @Field("strategy") strategy: String,
  ): ClerkApiResult<ClientPiggybackedResponse<SignUp>, ClerkErrorResponse>

  /** @see [com.clerk.sdk.model.signup.attemptVerification] */
  @FormUrlEncoded
  @POST(Paths.SignUpPath.WithId.ATTEMPT_VERIFICATION)
  suspend fun attemptSignUpVerification(
    @Path("id") signUpId: String,
    @Field("strategy") strategy: String,
    @Field("code") code: String,
  ): ClerkApiResult<ClientPiggybackedResponse<SignUp>, ClerkErrorResponse>

  // endregion
}
