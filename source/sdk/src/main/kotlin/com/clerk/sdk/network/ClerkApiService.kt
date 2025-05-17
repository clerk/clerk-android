package com.clerk.sdk.network

import com.clerk.sdk.model.client.Client
import com.clerk.sdk.model.environment.Environment
import com.clerk.sdk.model.error.ClerkErrorResponse
import com.clerk.sdk.model.response.ClerkResponse
import com.clerk.sdk.model.session.Session
import com.clerk.sdk.model.signin.SignIn
import com.clerk.sdk.model.signup.SignUp
import com.clerk.sdk.model.token.TokenResource
import com.clerk.sdk.network.requests.Requests
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

  // /client
  @GET("client") suspend fun client(): ClerkApiResult<Client, ClerkErrorResponse>

  // endregion

  // region Device Attestation

  // /client/device_attestation
  @GET("client/device_attestation") suspend fun deviceAttestation()

  // /client/device_attestation/challenges
  @POST("client/device_attestation/challenges") suspend fun challenges(): ClerkResponse<Unit>

  // /client/device_attestation/verify
  @POST("client/device_attestation/verify") suspend fun verify(): ClerkResponse<Unit>

  // endregion

  // region Session

  // /client/sessions
  @GET("client/sessions") suspend fun sessions(): ClerkResponse<Unit>

  // /client/sessions/{id}/remove
  @FormUrlEncoded
  @POST("client/sessions/{id}/remove")
  suspend fun remove(@Path("id") id: String, @Field("id") userId: String): ClerkResponse<Session>

  // /client/sessions/{id}/tokens
  @FormUrlEncoded
  @POST("client/sessions/{id}/tokens")
  suspend fun tokens(
    @Path("id") userId: String,
    @Field("id") id: String,
  ): ClerkApiResult<TokenResource, ClerkErrorResponse>

  // /client/sessions/{id}/tokens/{template}
  @FormUrlEncoded
  @POST("client/sessions/{id}/tokens/{template}")
  suspend fun tokens(
    @Path("id") userId: String,
    @Path("template") templateType: String,
    @Field("id") id: String,
    @Field("template") template: String,
  ): ClerkApiResult<TokenResource, ClerkErrorResponse>

  // endregion

  // region Sign In

  // client/sign_ins
  @POST("client/sign_ins") suspend fun signIn(): ClerkApiResult<SignIn, ClerkErrorResponse>

  // client/sign_ins/{id}
  @GET("client/sign_ins/{id}")
  suspend fun signIn(
    @Path("id") id: String,
    @Query("rotating_token_nonce") rotatingTokenNonce: String? = null,
  ): ClerkApiResult<SignIn, ClerkErrorResponse>

  // client/sign_ins/{id}/attempt_first_factor
  @POST("client/sign_ins/{id}/attempt_first_factor")
  suspend fun attemptFirstFactor(@Path("id") id: String): ClerkApiResult<SignIn, ClerkErrorResponse>

  // clients/sign_ins/{id}/attempt_first_factor
  @POST("client/sign_ins/{id}/attempt_first_factor")
  suspend fun attemptSecondFactor(
    @Path("id") id: String,
    @Query("rotating_token_nonce") rotatingTokenNonce: String? = null,
  ): ClerkApiResult<SignIn, ClerkErrorResponse>

  // client/sign_ins/{id}/prepare_first_factor
  @POST("client/sign_ins/{id}/prepare_first_factor")
  suspend fun prepareFirstFactor(
    @Path("id") id: String,
    // Expecting: Requests.SignIn.PrepareFirstFactorParams
    @FieldMap fields: Map<String, String>,
  ): ClerkApiResult<SignIn, ClerkErrorResponse>

  /**
   * Prepare the second factor for a sign in.
   *
   * @param id The session id.
   * @param params The parameters for the second
   *   factor. @see [Requests.SignIn.PrepareSecondFactorParams]
   */
  @POST("client/sign_ins/{id}/prepare_second_factor")
  suspend fun prepareSecondFactor(
    @Path("id") id: String,
    @FieldMap params: Map<String, String>,
  ): ClerkApiResult<SignIn, ClerkErrorResponse>

  /**
   * Reset the password for a sign in.
   *
   * The request body should contain the reset password fields as key-value pairs. The expected
   * input is [Requests.SignIn.ResetPasswordParams].
   */
  @POST("client/sign_ins/{id}/reset_password")
  suspend fun resetPassword(@Path("id") id: String, @FieldMap fields: Map<String, String>)

  // endregion

  // region Environment

  // /environment
  @GET("environment") suspend fun environment(): ClerkApiResult<Environment, ClerkErrorResponse>

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
  ): ClerkApiResult<SignUp, ClerkErrorResponse>

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
  ): ClerkApiResult<SignUp, ClerkErrorResponse>

  /** @see [prepareSignUpVerification] */
  @DecodeErrorBody
  @FormUrlEncoded
  @POST(Paths.SignUpPath.WithId.PREPARE_VERIFICATION)
  suspend fun prepareSignUpVerification(
    @Path("id") signUpId: String,
    @Field("strategy") strategy: String,
  ): ClerkApiResult<SignUp, ClerkErrorResponse>

  /** @see [attemptSignUpVerification] */
  @FormUrlEncoded
  @POST(Paths.SignUpPath.WithId.ATTEMPT_VERIFICATION)
  suspend fun attemptSignUpVerification(
    @Path("id") signUpId: String,
    @Field("strategy") strategy: String,
    @Field("code") code: String,
  ): ClerkApiResult<SignUp, ClerkErrorResponse>

  // endregion
}

internal object Paths {
  internal object SignUpPath {
    const val SIGN_UP = "client/sign_ups"

    internal object WithId {
      private const val SIGN_UP_WITH_ID = "client/sign_ups{id}"

      const val PREPARE_VERIFICATION = "${SIGN_UP_WITH_ID}/prepare_verification"

      const val ATTEMPT_VERIFICATION = "${SIGN_UP_WITH_ID}/attempt_verification"
    }
  }
}
