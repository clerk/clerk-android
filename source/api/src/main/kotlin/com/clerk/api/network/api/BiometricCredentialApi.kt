package com.clerk.api.network.api

import com.clerk.api.Clerk
import com.clerk.api.biometriccredential.BiometricCredential
import com.clerk.api.biometriccredential.BiometricCredentialChallenge
import com.clerk.api.biometriccredential.BiometricCredentialValidation
import com.clerk.api.network.ApiParams
import com.clerk.api.network.ApiPaths
import com.clerk.api.network.model.error.ClerkErrorResponse
import com.clerk.api.network.serialization.ClerkResult
import retrofit2.http.DELETE
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

internal interface BiometricCredentialApi {
  @GET(ApiPaths.User.BiometricCredential.BASE)
  suspend fun list(
    @Query(ApiParams.CLERK_SESSION_ID) sessionId: String? = Clerk.session?.id
  ): ClerkResult<List<BiometricCredential>, ClerkErrorResponse>

  @FormUrlEncoded
  @POST(ApiPaths.User.BiometricCredential.PREPARE)
  suspend fun prepareEnrollment(
    @Field("platform") platform: String = ANDROID_PLATFORM,
    @Field("app_identifier") appIdentifier: String,
    @Field("name") name: String? = null,
    @Field("algorithm") algorithm: String = BiometricCredential.ES256_ALGORITHM,
    @Field("public_key_jwk") publicKeyJwk: String,
    @Query(ApiParams.CLERK_SESSION_ID) sessionId: String? = Clerk.session?.id,
  ): ClerkResult<BiometricCredentialChallenge, ClerkErrorResponse>

  @FormUrlEncoded
  @POST(ApiPaths.User.BiometricCredential.ATTEMPT)
  suspend fun attemptEnrollment(
    @Field("platform") platform: String = ANDROID_PLATFORM,
    @Field("app_identifier") appIdentifier: String,
    @Field("name") name: String? = null,
    @Field("algorithm") algorithm: String = BiometricCredential.ES256_ALGORITHM,
    @Field("public_key_jwk") publicKeyJwk: String,
    @Field("client_data") clientData: String,
    @Field("signature") signature: String,
    @Query(ApiParams.CLERK_SESSION_ID) sessionId: String? = Clerk.session?.id,
  ): ClerkResult<BiometricCredential, ClerkErrorResponse>

  @FormUrlEncoded
  @POST(ApiPaths.Client.BiometricCredential.VALIDATE)
  suspend fun validateSignInCredential(
    @Field("trusted_device_id") biometricCredentialId: String
  ): ClerkResult<BiometricCredentialValidation, ClerkErrorResponse>

  @DELETE(ApiPaths.User.BiometricCredential.WITH_ID)
  suspend fun revoke(
    @Path("biometric_credential_id") biometricCredentialId: String,
    @Query(ApiParams.CLERK_SESSION_ID) sessionId: String? = Clerk.session?.id,
  ): ClerkResult<BiometricCredential, ClerkErrorResponse>

  companion object {
    const val ANDROID_PLATFORM: String = "android"
  }
}
