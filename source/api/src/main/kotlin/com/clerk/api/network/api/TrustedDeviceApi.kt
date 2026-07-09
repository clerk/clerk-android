package com.clerk.api.network.api

import com.clerk.api.Clerk
import com.clerk.api.network.ApiParams
import com.clerk.api.network.ApiPaths
import com.clerk.api.network.model.error.ClerkErrorResponse
import com.clerk.api.network.serialization.ClerkResult
import com.clerk.api.trusteddevice.TrustedDevice
import com.clerk.api.trusteddevice.TrustedDeviceChallenge
import com.clerk.api.trusteddevice.TrustedDeviceValidation
import retrofit2.http.DELETE
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Internal API interface for trusted-device operations.
 *
 * Trusted devices let users sign in with device biometrics. Enrollment happens against the
 * signed-in user (`me/trusted_devices`), while sign-in credential validation happens against the
 * client (`client/trusted_devices/validate`).
 */
internal interface TrustedDeviceApi {

  /** Lists active trusted-device credentials for the signed-in user. */
  @GET(ApiPaths.User.TrustedDevice.BASE)
  suspend fun list(
    @Query(ApiParams.CLERK_SESSION_ID) sessionId: String? = Clerk.session?.id
  ): ClerkResult<List<TrustedDevice>, ClerkErrorResponse>

  /** Prepares trusted-device enrollment and returns a challenge to sign. */
  @FormUrlEncoded
  @POST(ApiPaths.User.TrustedDevice.PREPARE)
  suspend fun prepareEnrollment(
    @Field("platform") platform: String = ANDROID_PLATFORM,
    @Field("app_identifier") appIdentifier: String,
    @Field("name") name: String? = null,
    @Field("algorithm") algorithm: String = TrustedDevice.ES256_ALGORITHM,
    @Field("public_key_jwk") publicKeyJwk: String,
    @Query(ApiParams.CLERK_SESSION_ID) sessionId: String? = Clerk.session?.id,
  ): ClerkResult<TrustedDeviceChallenge, ClerkErrorResponse>

  /** Completes trusted-device enrollment with the signed challenge. */
  @FormUrlEncoded
  @POST(ApiPaths.User.TrustedDevice.ATTEMPT)
  suspend fun attemptEnrollment(
    @Field("platform") platform: String = ANDROID_PLATFORM,
    @Field("app_identifier") appIdentifier: String,
    @Field("name") name: String? = null,
    @Field("algorithm") algorithm: String = TrustedDevice.ES256_ALGORITHM,
    @Field("public_key_jwk") publicKeyJwk: String,
    @Field("client_data") clientData: String,
    @Field("signature") signature: String,
    @Query(ApiParams.CLERK_SESSION_ID) sessionId: String? = Clerk.session?.id,
  ): ClerkResult<TrustedDevice, ClerkErrorResponse>

  /** Validates that a local trusted-device credential can still be used for sign-in. */
  @FormUrlEncoded
  @POST(ApiPaths.Client.TrustedDevice.VALIDATE)
  suspend fun validateSignInCredential(
    @Field("trusted_device_id") trustedDeviceId: String
  ): ClerkResult<TrustedDeviceValidation, ClerkErrorResponse>

  /** Revokes a trusted-device credential for the signed-in user. */
  @DELETE(ApiPaths.User.TrustedDevice.WITH_ID)
  suspend fun revoke(
    @Path("trusted_device_id") trustedDeviceId: String,
    @Query(ApiParams.CLERK_SESSION_ID) sessionId: String? = Clerk.session?.id,
  ): ClerkResult<TrustedDevice, ClerkErrorResponse>

  companion object {
    /** The platform value sent for credentials enrolled from this SDK. */
    const val ANDROID_PLATFORM: String = "android"
  }
}
