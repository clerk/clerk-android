package com.clerk.network

import com.clerk.network.model.client.Client
import com.clerk.network.model.deleted.DeletedObject
import com.clerk.network.model.environment.Environment
import com.clerk.network.model.error.ClerkErrorResponse
import com.clerk.network.model.image.ImageResource
import com.clerk.network.model.session.Session
import com.clerk.network.model.token.TokenResource
import com.clerk.network.paths.Paths
import com.clerk.network.serialization.ClerkResult
import com.clerk.signin.SignIn
import com.clerk.signup.SignUp
import com.clerk.user.User
import okhttp3.MultipartBody
import retrofit2.http.DELETE
import retrofit2.http.Field
import retrofit2.http.FieldMap
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Part
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

  @POST(Paths.ClientPath.Sessions.WithId.TOKENS)
  suspend fun tokens(@Path("id") sessionId: String): ClerkResult<TokenResource, ClerkErrorResponse>

  @POST(Paths.ClientPath.Sessions.WithId.TEMPLATE)
  suspend fun tokens(
    @Path("id") userId: String,
    @Path("template") templateType: String,
  ): ClerkResult<TokenResource, ClerkErrorResponse>

  // endregion

  // region Sign In

  /**
   * @param params The parameters for the sign in. @see [SignIn.CreateParams]
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

  // region User
  /** @see [com.clerk.user.get] */
  @GET(Paths.UserPath.ME)
  suspend fun getUser(
    @Query("_clerk_session_id") sessionId: String? = null
  ): ClerkResult<User, ClerkErrorResponse>

  /** @see [com.clerk.user.update] */
  @PATCH(Paths.UserPath.ME)
  @FormUrlEncoded
  suspend fun updateUser(
    @FieldMap fields: Map<String, String>
  ): ClerkResult<User, ClerkErrorResponse>

  @DELETE(Paths.UserPath.ME)
  suspend fun deleteUser(
    @Query("_clerk_session_id") sessionId: String? = null
  ): ClerkResult<DeletedObject, ClerkErrorResponse>

  @POST(Paths.UserPath.PROFILE_IMAGE)
  fun updateProfileImage(
    @Query("_clerk_session_id") sessionId: String? = null,
    @Part file: MultipartBody.Part,
  ): ClerkResult<ImageResource, ClerkErrorResponse>

  @DELETE(Paths.UserPath.PROFILE_IMAGE)
  suspend fun deleteProfileImage(
    @Query("_clerk_session_id") sessionId: String? = null
  ): ClerkResult<DeletedObject, ClerkErrorResponse>

  @FormUrlEncoded
  @POST(Paths.UserPath.Password.UPDATE)
  suspend fun updatePassword(
    @Query("_clerk_session_id") sessionId: String? = null,
    @FieldMap fields: Map<String, String>,
  ): ClerkResult<Session, ClerkErrorResponse>

  @FormUrlEncoded
  @POST
  suspend fun deletePassword(
    @Query("_clerk_session_id") sessionId: String? = null,
    @Field("password") password: String,
  ): ClerkResult<Session, ClerkErrorResponse>

  @GET(Paths.UserPath.Sessions.ACTIVE)
  suspend fun getActiveSessions(
    @Query("_clerk_session_id") sessionId: String? = null
  ): ClerkResult<List<Session>, ClerkErrorResponse>

  @POST(Paths.UserPath.Sessions.REVOKE)
  suspend fun revokeSession(
    @Query("_clerk_session_id") sessionId: String? = null,
    @Path("session_id") sessionIdToRevoke: String,
  ): ClerkResult<Session, ClerkErrorResponse>

  @GET(Paths.UserPath.Sessions.SESSIONS)
  suspend fun getSessions(
    @Query("_clerk_session_id") sessionId: String? = null
  ): ClerkResult<List<Session>, ClerkErrorResponse>
  // endregion
}
