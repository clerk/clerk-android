package com.clerk.network

import com.clerk.network.model.client.Client
import com.clerk.network.model.deleted.DeletedObject
import com.clerk.network.model.emailaddress.EmailAddress
import com.clerk.network.model.environment.Environment
import com.clerk.network.model.error.ClerkErrorResponse
import com.clerk.network.model.image.ImageResource
import com.clerk.network.model.passkey.Passkey
import com.clerk.network.model.phonenumber.PhoneNumber
import com.clerk.network.model.session.Session
import com.clerk.network.model.token.TokenResource
import com.clerk.network.model.totp.TOTPResource
import com.clerk.network.model.verification.Verification
import com.clerk.network.paths.CommonParams
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
  suspend fun removeSession(
    @Path(CommonParams.ID) id: String
  ): ClerkResult<Session, ClerkErrorResponse>

  @DELETE(Paths.ClientPath.Sessions.SESSIONS)
  suspend fun deleteSessions(): ClerkResult<Client, ClerkErrorResponse>

  @POST(Paths.ClientPath.Sessions.WithId.TOKENS)
  suspend fun tokens(
    @Path(CommonParams.ID) sessionId: String
  ): ClerkResult<TokenResource, ClerkErrorResponse>

  @POST(Paths.ClientPath.Sessions.WithId.TEMPLATE)
  suspend fun tokens(
    @Path(CommonParams.ID) userId: String,
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
    @Field(CommonParams.STRATEGY) strategy: String = "google_one_tap",
    @Field("token") token: String,
  ): ClerkResult<SignIn, ClerkErrorResponse>

  /** @see SignIn.authenticateWithRedirect */
  @FormUrlEncoded
  @POST(Paths.ClientPath.SignInPath.SIGN_INS)
  suspend fun authenticateWithRedirect(
    @Field(CommonParams.STRATEGY) strategy: String,
    @Field("redirect_url") redirectUrl: String,
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
    @Path(CommonParams.ID) id: String,
    @FieldMap fields: Map<String, String>,
  ): ClerkResult<SignUp, ClerkErrorResponse>

  /** @see [com.clerk.signup.prepareVerification] */
  @FormUrlEncoded
  @POST(Paths.SignUpPath.WithId.PREPARE_VERIFICATION)
  suspend fun prepareSignUpVerification(
    @Path(CommonParams.ID) signUpId: String,
    @Field(CommonParams.STRATEGY) strategy: String,
  ): ClerkResult<SignUp, ClerkErrorResponse>

  /** @see [com.clerk.signup.attemptVerification] */
  @FormUrlEncoded
  @POST(Paths.SignUpPath.WithId.ATTEMPT_VERIFICATION)
  suspend fun attemptSignUpVerification(
    @Path(CommonParams.ID) signUpId: String,
    @Field(CommonParams.STRATEGY) strategy: String,
    @Field(CommonParams.CODE) code: String,
  ): ClerkResult<SignUp, ClerkErrorResponse>

  // endregion

  // region User
  /** @see [com.clerk.user.get] */
  @GET(Paths.UserPath.ME)
  suspend fun getUser(
    @Query(CommonParams.CLERK_SESSION_ID) sessionId: String? = null
  ): ClerkResult<User, ClerkErrorResponse>

  /** @see [com.clerk.user.update] */
  @PATCH(Paths.UserPath.ME)
  @FormUrlEncoded
  suspend fun updateUser(
    @FieldMap fields: Map<String, String>
  ): ClerkResult<User, ClerkErrorResponse>

  @DELETE(Paths.UserPath.ME)
  suspend fun deleteUser(
    @Query(CommonParams.CLERK_SESSION_ID) sessionId: String? = null
  ): ClerkResult<DeletedObject, ClerkErrorResponse>

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
  ): ClerkResult<Session, ClerkErrorResponse>

  @FormUrlEncoded
  @POST
  suspend fun deletePassword(
    @Query(CommonParams.CLERK_SESSION_ID) sessionId: String? = null,
    @Field("password") password: String,
  ): ClerkResult<Session, ClerkErrorResponse>

  @GET(Paths.UserPath.Sessions.ACTIVE)
  suspend fun getActiveSessions(
    @Query(CommonParams.CLERK_SESSION_ID) sessionId: String? = null
  ): ClerkResult<List<Session>, ClerkErrorResponse>

  @POST(Paths.UserPath.Sessions.REVOKE)
  suspend fun revokeSession(
    @Query(CommonParams.CLERK_SESSION_ID) sessionId: String? = null,
    @Path("session_id") sessionIdToRevoke: String,
  ): ClerkResult<Session, ClerkErrorResponse>

  @GET(Paths.UserPath.Sessions.SESSIONS)
  suspend fun getSessions(
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

  // endregion
}
