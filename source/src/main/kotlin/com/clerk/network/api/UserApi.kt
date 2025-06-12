@file:Suppress("TooManyFunctions")

package com.clerk.network.api

import com.clerk.network.model.deleted.DeletedObject
import com.clerk.network.model.emailaddress.EmailAddress
import com.clerk.network.model.error.ClerkErrorResponse
import com.clerk.network.model.image.ImageResource
import com.clerk.network.model.passkey.Passkey
import com.clerk.network.model.phonenumber.PhoneNumber
import com.clerk.network.model.totp.TOTPResource
import com.clerk.network.model.verification.Verification
import com.clerk.network.paths.CommonParams
import com.clerk.network.paths.Paths
import com.clerk.network.serialization.ClerkResult
import com.clerk.session.Session
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

internal interface UserApi {
  /** @see [com.clerk.user.User.get] */
  @GET(Paths.UserPath.ME)
  suspend fun getUser(
    @Query(CommonParams.CLERK_SESSION_ID) sessionId: String? = null
  ): ClerkResult<User, ClerkErrorResponse>

  /** @see [com.clerk.user.User.update] */
  @PATCH(Paths.UserPath.ME)
  @FormUrlEncoded
  suspend fun updateUser(
    @Query(CommonParams.CLERK_SESSION_ID) sessionId: String? = null,
    @FieldMap fields: Map<String, String>,
  ): ClerkResult<User, ClerkErrorResponse>

  /** @see [com.clerk.user.User.delete] */
  @DELETE(Paths.UserPath.ME)
  suspend fun deleteUser(
    @Query(CommonParams.CLERK_SESSION_ID) sessionId: String? = null
  ): ClerkResult<DeletedObject, ClerkErrorResponse>

  @GET(Paths.UserPath.Sessions.SESSIONS)
  suspend fun getSessions(
    @Query(CommonParams.CLERK_SESSION_ID) sessionId: String? = null
  ): ClerkResult<List<Session>, ClerkErrorResponse>

  @POST(Paths.UserPath.PROFILE_IMAGE)
  fun updateProfileImage(
    @Query(CommonParams.CLERK_SESSION_ID) sessionId: String? = null,
    @Part file: MultipartBody.Part,
  ): ClerkResult<ImageResource, ClerkErrorResponse>

  @DELETE(Paths.UserPath.PROFILE_IMAGE)
  suspend fun deleteProfileImage(
    @Query(CommonParams.CLERK_SESSION_ID) sessionId: String? = null
  ): ClerkResult<DeletedObject, ClerkErrorResponse>

  @FormUrlEncoded
  @POST(Paths.UserPath.Password.UPDATE)
  suspend fun updatePassword(
    @Query(CommonParams.CLERK_SESSION_ID) sessionId: String? = null,
    @FieldMap fields: Map<String, String>,
  ): ClerkResult<User, ClerkErrorResponse>

  @FormUrlEncoded
  @POST
  suspend fun deletePassword(
    @Query(CommonParams.CLERK_SESSION_ID) sessionId: String? = null,
    @Field("current_password") password: String,
  ): ClerkResult<User, ClerkErrorResponse>

  @GET(Paths.UserPath.Sessions.ACTIVE)
  suspend fun getActiveSessions(
    @Query(CommonParams.CLERK_SESSION_ID) sessionId: String? = null
  ): ClerkResult<List<Session>, ClerkErrorResponse>

  @GET(Paths.UserPath.EmailAddress.EMAIL_ADDRESSES)
  suspend fun getEmailAddresses(
    @Query(CommonParams.CLERK_SESSION_ID) sessionId: String? = null
  ): ClerkResult<List<EmailAddress>, ClerkErrorResponse>

  @FormUrlEncoded
  @POST(Paths.UserPath.EmailAddress.EMAIL_ADDRESSES)
  suspend fun createEmailAddress(
    @Query(CommonParams.CLERK_SESSION_ID) sessionId: String? = null,
    @Field("email_address") emailAddress: String,
  ): ClerkResult<EmailAddress, ClerkErrorResponse>

  @FormUrlEncoded
  @POST(Paths.UserPath.EmailAddress.WithId.EMAIL_ADDRESSES_WITH_ID)
  suspend fun attemptEmailAddressVerification(
    @Path(CommonParams.EMAIL_ID) emailAddressId: String,
    @Query(CommonParams.CLERK_SESSION_ID) sessionId: String? = null,
    @Field(CommonParams.CODE) strategy: String,
  ): ClerkResult<EmailAddress, ClerkErrorResponse>

  @FormUrlEncoded
  @POST(Paths.UserPath.EmailAddress.WithId.EMAIL_ADDRESSES_WITH_ID)
  suspend fun prepareEmailAddressVerification(
    @Path(CommonParams.EMAIL_ID) emailAddressId: String,
    @Query(CommonParams.CLERK_SESSION_ID) sessionId: String? = null,
    @FieldMap params: Map<String, String>,
  ): ClerkResult<EmailAddress, ClerkErrorResponse>

  @GET(Paths.UserPath.EmailAddress.WithId.EMAIL_ADDRESSES_WITH_ID)
  suspend fun getEmailAddress(
    @Path(CommonParams.EMAIL_ID) emailAddressId: String,
    @Query(CommonParams.CLERK_SESSION_ID) sessionId: String? = null,
  ): ClerkResult<EmailAddress, ClerkErrorResponse>

  @DELETE(Paths.UserPath.EmailAddress.WithId.EMAIL_ADDRESSES_WITH_ID)
  suspend fun deleteEmailAddress(
    @Path(CommonParams.EMAIL_ID) emailAddressId: String,
    @Query(CommonParams.CLERK_SESSION_ID) sessionId: String? = null,
  ): ClerkResult<EmailAddress, ClerkErrorResponse>

  @GET(Paths.UserPath.PhoneNumbers.PHONE_NUMBERS)
  suspend fun getPhoneNumbers(
    @Query(CommonParams.CLERK_SESSION_ID) sessionId: String? = null
  ): ClerkResult<List<PhoneNumber>, ClerkErrorResponse>

  @FormUrlEncoded
  @POST(Paths.UserPath.PhoneNumbers.PHONE_NUMBERS)
  suspend fun createPhoneNumber(
    @Query(CommonParams.CLERK_SESSION_ID) sessionId: String? = null,
    @Field("phone_number") phoneNumber: String,
    @Field("reserved_for_second_factor") reservedForSecondFactor: Boolean = false,
  ): ClerkResult<PhoneNumber, ClerkErrorResponse>

  @FormUrlEncoded
  @POST(Paths.UserPath.PhoneNumbers.WithId.PHONE_NUMBERS_WITH_ID)
  suspend fun attemptPhoneNumberVerification(
    @Path(CommonParams.PHONE_NUMBER_ID) phoneNumberId: String,
    @Query(CommonParams.CLERK_SESSION_ID) sessionId: String? = null,
    @Field(CommonParams.CODE) code: String,
  ): ClerkResult<PhoneNumber, ClerkErrorResponse>

  @FormUrlEncoded
  @POST(Paths.UserPath.PhoneNumbers.WithId.PHONE_NUMBERS_WITH_ID)
  suspend fun preparePhoneNumberVerification(
    @Path(CommonParams.PHONE_NUMBER_ID) phoneNumberId: String,
    @Query(CommonParams.CLERK_SESSION_ID) sessionId: String? = null,
    @Field(CommonParams.STRATEGY) strategy: String,
  ): ClerkResult<PhoneNumber, ClerkErrorResponse>

  @GET(Paths.UserPath.PhoneNumbers.WithId.PHONE_NUMBERS_WITH_ID)
  suspend fun getPhoneNumber(
    @Path(CommonParams.PHONE_NUMBER_ID) phoneNumberId: String,
    @Query(CommonParams.CLERK_SESSION_ID) sessionId: String? = null,
  ): ClerkResult<PhoneNumber, ClerkErrorResponse>

  @DELETE(Paths.UserPath.PhoneNumbers.WithId.PHONE_NUMBERS_WITH_ID)
  suspend fun deletePhoneNumber(
    @Path(CommonParams.PHONE_NUMBER_ID) phoneNumberId: String,
    @Query(CommonParams.CLERK_SESSION_ID) sessionId: String? = null,
  ): ClerkResult<DeletedObject, ClerkErrorResponse>

  @FormUrlEncoded
  @PATCH(Paths.UserPath.PhoneNumbers.WithId.PHONE_NUMBERS_WITH_ID)
  suspend fun updatePhoneNumber(
    @Path(CommonParams.PHONE_NUMBER_ID) phoneNumberId: String,
    @Query(CommonParams.CLERK_SESSION_ID) sessionId: String? = null,
    @Field("reserved_for_second_factor") reservedForSecondFactor: Boolean? = null,
    @Field("default_second_factor") defaultSecondFactor: Boolean? = null,
  ): ClerkResult<PhoneNumber, ClerkErrorResponse>

  @POST(Paths.UserPath.Passkeys.PASSKEYS)
  suspend fun createPasskey(
    @Query(CommonParams.CLERK_SESSION_ID) sessionId: String? = null
  ): ClerkResult<Passkey, ClerkErrorResponse>

  @GET(Paths.UserPath.Passkeys.WithId.PASSKEYS_WITH_ID)
  suspend fun getPasskey(
    @Path(CommonParams.PASSKEY_ID) passkeyId: String
  ): ClerkResult<Passkey, ClerkErrorResponse>

  @DELETE(Paths.UserPath.Passkeys.WithId.PASSKEYS_WITH_ID)
  suspend fun deletePasskey(
    @Path(CommonParams.PASSKEY_ID) passkeyId: String
  ): ClerkResult<DeletedObject, ClerkErrorResponse>

  @PATCH(Paths.UserPath.Passkeys.WithId.PASSKEYS_WITH_ID)
  suspend fun updatePasskey(
    @Path(CommonParams.PASSKEY_ID) passkeyId: String
  ): ClerkResult<Passkey, ClerkErrorResponse>

  @FormUrlEncoded
  @POST(Paths.UserPath.Passkeys.WithId.ATTEMPT_VERIFICATION)
  suspend fun attemptPasskeyVerification(
    @Path(CommonParams.PASSKEY_ID) passkeyId: String,
    @Field(CommonParams.STRATEGY) strategy: String = "passkey",
    @Field("public_key_credential") publicKeyCredential: String,
  ): ClerkResult<Passkey, ClerkErrorResponse>

  @FormUrlEncoded
  @POST(Paths.UserPath.ExternalAccounts.EXTERNAL_ACCOUNTS)
  suspend fun connectOAuthAccount(
    @FieldMap params: Map<String, String>
  ): ClerkResult<Verification, ClerkErrorResponse>

  @FormUrlEncoded
  @PATCH(Paths.UserPath.ExternalAccounts.WithId.REAUTHORIZE)
  suspend fun reauthorizeExternalAccount(
    @Path(CommonParams.EXTERNAL_ACCOUNT_ID) externalAccountId: String,
    @FieldMap params: Map<String, String>,
  ): ClerkResult<Verification, ClerkErrorResponse>

  @DELETE(Paths.UserPath.ExternalAccounts.WithId.EXTERNAL_ACCOUNTS_WITH_ID)
  suspend fun deleteExternalAccount(
    @Path(CommonParams.EXTERNAL_ACCOUNT_ID) externalAccountId: String
  ): ClerkResult<DeletedObject, ClerkErrorResponse>

  @DELETE(Paths.UserPath.ExternalAccounts.WithId.REVOKE_TOKENS)
  suspend fun revokeExternalAccountTokens(
    @Path(CommonParams.EXTERNAL_ACCOUNT_ID) externalAccountId: String
  ): ClerkResult<User, ClerkErrorResponse>

  @POST(Paths.UserPath.TOTP.TOTP)
  suspend fun createTOTP(): ClerkResult<TOTPResource, ClerkErrorResponse>

  @DELETE(Paths.UserPath.TOTP.TOTP)
  suspend fun deleteTOTP(): ClerkResult<DeletedObject, ClerkErrorResponse>

  @FormUrlEncoded
  @POST(Paths.UserPath.TOTP.ATTEMPT_VERIFICATION)
  suspend fun attemptTOTPVerification(
    @Field(CommonParams.CODE) code: String
  ): ClerkResult<TOTPResource, ClerkErrorResponse>
}
