package com.clerk.network

import com.clerk.network.model.client.Client
import com.clerk.network.model.environment.Environment
import com.clerk.network.model.error.ClerkErrorResponse
import com.clerk.network.model.session.Session
import com.clerk.network.model.token.TokenResource
import com.clerk.network.paths.Paths
import com.clerk.network.serialization.ClerkResult
import com.clerk.signin.SignIn
import com.clerk.signup.SignUp
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
 * When the endpoints are called they'll come back as a [ClerkResult], where the type will either be
 * the type specified or [ClerkErrorResponse] if there was an error.
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

  @GET(Paths.ClientPath.CLIENT) suspend fun client(): ClerkResult<Client, ClerkErrorResponse>

  // endregion

  // region Device Attestation

  @GET(Paths.ClientPath.DeviceAttestation.DEVICE_ATTESTATION) suspend fun deviceAttestation()

  @POST(Paths.ClientPath.DeviceAttestation.CHALLENGES)
  suspend fun challenges(): ClerkResult<Unit, ClerkErrorResponse>

  @POST(Paths.ClientPath.DeviceAttestation.VERIFY)
  suspend fun verify(): ClerkResult<Unit, ClerkErrorResponse>

  // endregion

  // region Session

  @GET(Paths.ClientPath.Sessions.SESSIONS)
  suspend fun sessions(): ClerkResult<Unit, ClerkErrorResponse>

  @POST(Paths.ClientPath.Sessions.WithId.REMOVE)
  suspend fun removeSession(@Path("id") id: String): ClerkResult<Session, ClerkErrorResponse>

  @DELETE(Paths.ClientPath.Sessions.SESSIONS)
  suspend fun deleteSessions(): ClerkResult<Client, ClerkErrorResponse>

  @FormUrlEncoded
  @POST(Paths.ClientPath.Sessions.WithId.TOKENS)
  suspend fun tokens(
    @Path("id") userId: String,
    @Field("id") id: String,
  ): ClerkResult<TokenResource, ClerkErrorResponse>

  @FormUrlEncoded
  @POST(Paths.ClientPath.Sessions.WithId.TEMPLATE)
  suspend fun tokensTemplate(
    @Path("id") userId: String,
    @Path("template") templateType: String,
    @Field("id") id: String,
    @Field("template") template: String,
  ): ClerkResult<TokenResource, ClerkErrorResponse>

  // endregion

  // region Sign In

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
    @Field("strategy") strategy: String = "google_one_tap",
    @Field("token") token: String,
  ): ClerkResult<SignIn, ClerkErrorResponse>

  /** @see SignIn.authenticateWithRedirect */
  @FormUrlEncoded
  @POST(Paths.ClientPath.SignInPath.SIGN_INS)
  suspend fun authenticateWithRedirect(
    @Field("strategy") strategy: String,
    @Field("redirect_url") redirectUrl: String,
  ): ClerkResult<SignIn, ClerkErrorResponse>

  @GET(Paths.ClientPath.SignInPath.WithId.SIGN_INS_WITH_ID)
  suspend fun fetchSignIn(
    @Path("id") id: String,
    @Query("rotating_token_nonce") rotatingTokenNonce: String? = null,
  ): ClerkResult<SignIn, ClerkErrorResponse>

  @FormUrlEncoded
  @POST(Paths.ClientPath.SignInPath.WithId.ATTEMPT_FIRST_FACTOR)
  suspend fun attemptFirstFactor(
    @Path("id") id: String,
    @FieldMap params: Map<String, String>,
  ): ClerkResult<SignIn, ClerkErrorResponse>

  @FormUrlEncoded
  @POST(Paths.ClientPath.SignInPath.WithId.ATTEMPT_SECOND_FACTOR)
  suspend fun attemptSecondFactor(
    @Path("id") id: String,
    @FieldMap params: Map<String, String>,
  ): ClerkResult<SignIn, ClerkErrorResponse>

  @FormUrlEncoded
  @POST(Paths.ClientPath.SignInPath.WithId.PREPARE_FIRST_FACTOR)
  suspend fun prepareSignInFirstFactor(
    @Path("id") id: String,
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
    @Path("id") id: String,
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
    @Path("id") id: String,
    @Field("password") password: String,
    @Field("sign_out_of_other_sessions") signOutOfOtherSessions: Boolean,
  ): ClerkResult<SignIn, ClerkErrorResponse>

  // endregion

  // region Environment

  // /environment
  @GET(Paths.ENVIRONMENT) suspend fun environment(): ClerkResult<Environment, ClerkErrorResponse>

  // region Sign Up

  /** @see [SignUp.create] */
  @FormUrlEncoded
  @POST(Paths.SignUpPath.SIGN_UP)
  suspend fun createSignUp(
    @FieldMap fields: Map<String, String>
  ): ClerkResult<SignUp, ClerkErrorResponse>

  @FormUrlEncoded
  @PATCH(Paths.SignUpPath.WithId.UPDATE)
  suspend fun updateSignUp(
    @Path("id") id: String,
    @FieldMap fields: Map<String, String>,
  ): ClerkResult<SignUp, ClerkErrorResponse>

  /** @see [com.clerk.signup.prepareVerification] */
  @FormUrlEncoded
  @POST(Paths.SignUpPath.WithId.PREPARE_VERIFICATION)
  suspend fun prepareSignUpVerification(
    @Path("id") signUpId: String,
    @Field("strategy") strategy: String,
  ): ClerkResult<SignUp, ClerkErrorResponse>

  /** @see [com.clerk.signup.attemptVerification] */
  @FormUrlEncoded
  @POST(Paths.SignUpPath.WithId.ATTEMPT_VERIFICATION)
  suspend fun attemptSignUpVerification(
    @Path("id") signUpId: String,
    @Field("strategy") strategy: String,
    @Field("code") code: String,
  ): ClerkResult<SignUp, ClerkErrorResponse>

  // endregion
}
