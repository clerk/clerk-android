package com.clerk.sdk.network

import com.clerk.sdk.model.client.Client
import com.clerk.sdk.model.environment.Environment
import com.clerk.sdk.model.response.ClerkResponse
import com.clerk.sdk.model.response.ClientPiggybackedResponse
import com.clerk.sdk.model.session.Session
import com.clerk.sdk.model.signin.SignIn
import com.clerk.sdk.model.signup.SignUp
import com.clerk.sdk.model.token.TokenResource
import com.clerk.sdk.network.encoding.FormEncoded
import com.clerk.sdk.network.requests.Requests
import retrofit2.http.Field
import retrofit2.http.FieldMap
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
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
  @GET("client") suspend fun client(): ClerkResponse<ClientPiggybackedResponse<Client>>

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
  ): ClerkResponse<TokenResource>

  // /client/sessions/{id}/tokens/{template}
  @FormUrlEncoded
  @POST("client/sessions/{id}/tokens/{template}")
  suspend fun tokens(
    @Path("id") userId: String,
    @Path("template") templateType: String,
    @Field("id") id: String,
    @Field("template") template: String,
  ): ClerkResponse<TokenResource>

  // endregion

  // region Sign In

  // client/sign_ins
  @POST("client/sign_ins") suspend fun signIn(): ClerkResponse<SignIn>

  // client/sign_ins/{id}
  @GET("client/sign_ins/{id}")
  suspend fun signIn(
    @Path("id") id: String,
    @Query("rotating_token_nonce") rotatingTokenNonce: String? = null,
  ): ClerkResponse<SignIn>

  // client/sign_ins/{id}/attempt_first_factor
  @POST("client/sign_ins/{id}/attempt_first_factor")
  suspend fun attemptFirstFactor(@Path("id") id: String): ClerkResponse<SignIn>

  // clients/sign_ins/{id}/attempt_first_factor
  @POST("client/sign_ins/{id}/attempt_first_factor")
  suspend fun attemptSecondFactor(
    @Path("id") id: String,
    @Query("rotating_token_nonce") rotatingTokenNonce: String? = null,
  ): ClerkResponse<SignIn>

  // client/sign_ins/{id}/prepare_first_factor
  @POST("client/sign_ins/{id}/prepare_first_factor")
  suspend fun prepareFirstFactor(
    @Path("id") id: String,
    // Expecting: Requests.SignIn.PrepareFirstFactorParams
    @FieldMap fields: Map<String, String>,
  ): ClerkResponse<SignIn>

  // client/sign_ins/{id}/prepare_second_factor
  @POST("client/sign_ins/{id}/prepare_second_factor")
  suspend fun prepareSecondFactor(
    @Path("id") id: String,
    @FormEncoded params: Requests.SignIn.PrepareSecondFactorParams,
  ): ClerkResponse<SignIn>

  // client/sign_ins/{id}/reset_password

  @POST("client/sign_ins/{id}/reset_password")
  suspend fun resetPassword(
    @Path("id") id: String,
    @FormEncoded params: Requests.SignIn.ResetPasswordParams,
  )

  // endregion

  // region Environment

  // /environment
  @GET("environment") suspend fun environment(): ClerkResponse<Environment>

  // region Sign Up

  /**
   * Sign up a user with the given parameters. NOTE: THIS IS NOT COMPLETE, NEED GUIDANCE ON OAUTH
   * ENUM
   */
  @FormUrlEncoded
  @POST("client/sign_ups")
  suspend fun createSignUp(
    @FieldMap fields: Map<String, String>
  ): ClerkResponse<ClientPiggybackedResponse<SignUp>>

  // endregion
}
