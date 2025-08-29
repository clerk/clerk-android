@file:Suppress("TooManyFunctions")

package com.clerk.api.network.api

import com.clerk.api.Clerk
import com.clerk.api.emailaddress.EmailAddress
import com.clerk.api.externalaccount.ExternalAccount
import com.clerk.api.network.ApiParams
import com.clerk.api.network.ApiPaths
import com.clerk.api.network.ClerkPaginatedResponse
import com.clerk.api.network.model.backupcodes.BackupCodeResource
import com.clerk.api.network.model.deleted.DeletedObject
import com.clerk.api.network.model.error.ClerkErrorResponse
import com.clerk.api.network.model.image.ImageResource
import com.clerk.api.network.model.totp.TOTPResource
import com.clerk.api.network.serialization.ClerkResult
import com.clerk.api.organizations.OrganizationInvitation
import com.clerk.api.organizations.OrganizationMembership
import com.clerk.api.organizations.OrganizationSuggestion
import com.clerk.api.passkeys.Passkey
import com.clerk.api.phonenumber.PhoneNumber
import com.clerk.api.session.Session
import com.clerk.api.user.User
import okhttp3.MultipartBody
import retrofit2.http.DELETE
import retrofit2.http.Field
import retrofit2.http.FieldMap
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

internal interface UserApi {
  /** @see [com.clerk.user.User.get] */
  @GET(ApiPaths.User.BASE)
  suspend fun getUser(
    @Query(ApiParams.CLERK_SESSION_ID) sessionId: String? = Clerk.session?.id
  ): ClerkResult<User, ClerkErrorResponse>

  /** @see [com.clerk.user.User.update] */
  @PATCH(ApiPaths.User.BASE)
  @FormUrlEncoded
  suspend fun updateUser(
    @FieldMap fields: Map<String, String>,
    @Query(ApiParams.CLERK_SESSION_ID) sessionId: String? = Clerk.session?.id,
  ): ClerkResult<User, ClerkErrorResponse>

  /** @see [com.clerk.user.User.delete] */
  @DELETE(ApiPaths.User.BASE)
  suspend fun deleteUser(
    @Query(ApiParams.CLERK_SESSION_ID) sessionId: String? = Clerk.session?.id
  ): ClerkResult<DeletedObject, ClerkErrorResponse>

  @GET(ApiPaths.User.Sessions.BASE)
  suspend fun getSessions(
    @Query(ApiParams.CLERK_SESSION_ID) sessionId: String? = Clerk.session?.id
  ): ClerkResult<List<Session>, ClerkErrorResponse>

  @Multipart
  @POST(ApiPaths.User.PROFILE_IMAGE)
  suspend fun setProfileImage(
    @Part file: MultipartBody.Part,
    @Query(ApiParams.CLERK_SESSION_ID) sessionId: String? = Clerk.session?.id,
  ): ClerkResult<ImageResource, ClerkErrorResponse>

  @DELETE(ApiPaths.User.PROFILE_IMAGE)
  suspend fun deleteProfileImage(
    @Query(ApiParams.CLERK_SESSION_ID) sessionId: String? = Clerk.session?.id
  ): ClerkResult<DeletedObject, ClerkErrorResponse>

  @FormUrlEncoded
  @POST(ApiPaths.User.Password.UPDATE)
  suspend fun updatePassword(
    @FieldMap fields: Map<String, String>,
    @Query(ApiParams.CLERK_SESSION_ID) sessionId: String? = Clerk.session?.id,
  ): ClerkResult<User, ClerkErrorResponse>

  @FormUrlEncoded
  @POST
  suspend fun deletePassword(
    @Field("current_password") password: String,
    @Query(ApiParams.CLERK_SESSION_ID) sessionId: String? = Clerk.session?.id,
  ): ClerkResult<User, ClerkErrorResponse>

  @GET(ApiPaths.User.Sessions.ACTIVE)
  suspend fun getActiveSessions(
    @Query(ApiParams.CLERK_SESSION_ID) sessionId: String? = Clerk.session?.id
  ): ClerkResult<List<Session>, ClerkErrorResponse>

  @GET(ApiPaths.User.EmailAddress.BASE)
  suspend fun getEmailAddresses(
    @Query(ApiParams.CLERK_SESSION_ID) sessionId: String? = Clerk.session?.id
  ): ClerkResult<List<EmailAddress>, ClerkErrorResponse>

  @FormUrlEncoded
  @POST(ApiPaths.User.EmailAddress.BASE)
  suspend fun createEmailAddress(
    @Field("email_address") emailAddress: String,
    @Query(ApiParams.CLERK_SESSION_ID) sessionId: String? = Clerk.session?.user?.id,
  ): ClerkResult<EmailAddress, ClerkErrorResponse>

  @FormUrlEncoded
  @POST(ApiPaths.User.EmailAddress.ATTEMPT_VERIFICATION)
  suspend fun attemptEmailAddressVerification(
    @Path(ApiParams.EMAIL_ID) emailAddressId: String,
    @Field(ApiParams.CODE) code: String,
    @Query(ApiParams.CLERK_SESSION_ID) sessionId: String? = Clerk.session?.id,
  ): ClerkResult<EmailAddress, ClerkErrorResponse>

  @FormUrlEncoded
  @POST(ApiPaths.User.EmailAddress.PREPARE_VERIFICATION)
  suspend fun prepareEmailAddressVerification(
    @Path(ApiParams.EMAIL_ID) emailAddressId: String,
    @FieldMap params: Map<String, String>,
    @Query(ApiParams.CLERK_SESSION_ID) sessionId: String? = Clerk.session?.id,
  ): ClerkResult<EmailAddress, ClerkErrorResponse>

  @GET(ApiPaths.User.EmailAddress.WITH_ID)
  suspend fun getEmailAddress(
    @Path(ApiParams.EMAIL_ID) emailAddressId: String,
    @Query(ApiParams.CLERK_SESSION_ID) sessionId: String? = Clerk.session?.id,
  ): ClerkResult<EmailAddress, ClerkErrorResponse>

  @DELETE(ApiPaths.User.EmailAddress.WITH_ID)
  suspend fun deleteEmailAddress(
    @Path(ApiParams.EMAIL_ID) emailAddressId: String,
    @Query(ApiParams.CLERK_SESSION_ID) sessionId: String? = Clerk.session?.id,
  ): ClerkResult<DeletedObject, ClerkErrorResponse>

  @GET(ApiPaths.User.PhoneNumber.BASE)
  suspend fun getPhoneNumbers(
    @Query(ApiParams.CLERK_SESSION_ID) sessionId: String? = Clerk.session?.id
  ): ClerkResult<List<PhoneNumber>, ClerkErrorResponse>

  @FormUrlEncoded
  @POST(ApiPaths.User.PhoneNumber.BASE)
  suspend fun createPhoneNumber(
    @Field("phone_number") phoneNumber: String,
    @Field("reserved_for_second_factor") reservedForSecondFactor: Boolean = false,
    @Query(ApiParams.CLERK_SESSION_ID) sessionId: String? = Clerk.session?.id,
  ): ClerkResult<PhoneNumber, ClerkErrorResponse>

  @FormUrlEncoded
  @POST(ApiPaths.User.PhoneNumber.ATTEMPT_VERIFICATION)
  suspend fun attemptPhoneNumberVerification(
    @Path(ApiParams.PHONE_NUMBER_ID) phoneNumberId: String,
    @Field(ApiParams.CODE) code: String,
    @Query(ApiParams.CLERK_SESSION_ID) sessionId: String? = Clerk.session?.id,
  ): ClerkResult<PhoneNumber, ClerkErrorResponse>

  @FormUrlEncoded
  @POST(ApiPaths.User.PhoneNumber.PREPARE_VERIFICATION)
  suspend fun preparePhoneNumberVerification(
    @Path(ApiParams.PHONE_NUMBER_ID) phoneNumberId: String,
    @Field(ApiParams.STRATEGY) strategy: String,
    @Query(ApiParams.CLERK_SESSION_ID) sessionId: String? = Clerk.session?.id,
  ): ClerkResult<PhoneNumber, ClerkErrorResponse>

  @GET(ApiPaths.User.PhoneNumber.WITH_ID)
  suspend fun getPhoneNumber(
    @Path(ApiParams.PHONE_NUMBER_ID) phoneNumberId: String,
    @Query(ApiParams.CLERK_SESSION_ID) sessionId: String? = Clerk.session?.id,
  ): ClerkResult<PhoneNumber, ClerkErrorResponse>

  @DELETE(ApiPaths.User.PhoneNumber.WITH_ID)
  suspend fun deletePhoneNumber(
    @Path(ApiParams.PHONE_NUMBER_ID) phoneNumberId: String,
    @Query(ApiParams.CLERK_SESSION_ID) sessionId: String? = Clerk.session?.id,
  ): ClerkResult<DeletedObject, ClerkErrorResponse>

  @FormUrlEncoded
  @PATCH(ApiPaths.User.PhoneNumber.WITH_ID)
  suspend fun updatePhoneNumber(
    @Path(ApiParams.PHONE_NUMBER_ID) phoneNumberId: String,
    @Field("reserved_for_second_factor") reservedForSecondFactor: Boolean? = null,
    @Field("default_second_factor") defaultSecondFactor: Boolean? = null,
    @Query(ApiParams.CLERK_SESSION_ID) sessionId: String? = Clerk.session?.id,
  ): ClerkResult<PhoneNumber, ClerkErrorResponse>

  @POST(ApiPaths.User.Passkey.BASE)
  suspend fun createPasskey(
    @Query(ApiParams.CLERK_SESSION_ID) sessionId: String? = Clerk.session?.id
  ): ClerkResult<Passkey, ClerkErrorResponse>

  @GET(ApiPaths.User.Passkey.WITH_ID)
  suspend fun getPasskey(
    @Path(ApiParams.PASSKEY_ID) passkeyId: String
  ): ClerkResult<Passkey, ClerkErrorResponse>

  @DELETE(ApiPaths.User.Passkey.WITH_ID)
  suspend fun deletePasskey(
    @Path(ApiParams.PASSKEY_ID) passkeyId: String
  ): ClerkResult<DeletedObject, ClerkErrorResponse>

  @PATCH(ApiPaths.User.Passkey.WITH_ID)
  suspend fun updatePasskey(
    @Path(ApiParams.PASSKEY_ID) passkeyId: String,
    @Field("name") name: String? = null,
  ): ClerkResult<Passkey, ClerkErrorResponse>

  @FormUrlEncoded
  @POST(ApiPaths.User.Passkey.ATTEMPT_VERIFICATION)
  suspend fun attemptPasskeyVerification(
    @Path(ApiParams.PASSKEY_ID) passkeyId: String,
    @Field(ApiParams.STRATEGY) strategy: String = "passkey",
    @Field("public_key_credential") publicKeyCredential: String,
  ): ClerkResult<Passkey, ClerkErrorResponse>

  @FormUrlEncoded
  @POST(ApiPaths.User.ExternalAccount.BASE)
  suspend fun createExternalAccount(
    @FieldMap params: Map<String, String>
  ): ClerkResult<ExternalAccount, ClerkErrorResponse>

  @FormUrlEncoded
  @PATCH(ApiPaths.User.ExternalAccount.REAUTHORIZE)
  suspend fun reauthorizeExternalAccount(
    @Path(ApiParams.EXTERNAL_ACCOUNT_ID) externalAccountId: String,
    @Field("redirect_url") redirectUrl: String,
  ): ClerkResult<ExternalAccount, ClerkErrorResponse>

  @DELETE(ApiPaths.User.ExternalAccount.WITH_ID)
  suspend fun deleteExternalAccount(
    @Path(ApiParams.EXTERNAL_ACCOUNT_ID) externalAccountId: String
  ): ClerkResult<DeletedObject, ClerkErrorResponse>

  @DELETE(ApiPaths.User.ExternalAccount.REVOKE_TOKENS)
  suspend fun revokeExternalAccountTokens(
    @Path(ApiParams.EXTERNAL_ACCOUNT_ID) externalAccountId: String
  ): ClerkResult<User, ClerkErrorResponse>

  @POST(ApiPaths.User.TOTP.BASE)
  suspend fun createTOTP(): ClerkResult<TOTPResource, ClerkErrorResponse>

  @DELETE(ApiPaths.User.TOTP.BASE)
  suspend fun deleteTOTP(): ClerkResult<DeletedObject, ClerkErrorResponse>

  @FormUrlEncoded
  @POST(ApiPaths.User.TOTP.ATTEMPT_VERIFICATION)
  suspend fun attemptTOTPVerification(
    @Field(ApiParams.CODE) code: String
  ): ClerkResult<TOTPResource, ClerkErrorResponse>

  @POST(ApiPaths.User.BACKUP_CODES)
  suspend fun createBackupCodes(): ClerkResult<BackupCodeResource, ClerkErrorResponse>

  /**
   * Accepts a user organization invitation.
   *
   * @param invitationId The unique identifier of the invitation to accept
   * @param sessionId Optional session ID for the operation
   * @return A [ClerkResult] containing either the accepted [OrganizationInvitation] on success or a
   *   [ClerkErrorResponse] on failure
   * @see com.clerk.api.organizations.acceptInvitation
   */
  @POST(ApiPaths.User.ACCEPT_ORGANIZATION_INVITATION)
  suspend fun acceptUserOrganizationInvitation(
    @Path("invitation_id") invitationId: String,
    @Query(ApiParams.CLERK_SESSION_ID) sessionId: String? = null,
  ): ClerkResult<OrganizationInvitation, ClerkErrorResponse>

  /**
   * Accepts an organization suggestion for a user.
   *
   * @param suggestionId The unique identifier of the suggestion to accept
   * @param sessionId Optional session ID for the operation
   * @return A [ClerkResult] containing either the accepted [OrganizationSuggestion] on success or a
   *   [ClerkErrorResponse] on failure
   * @see com.clerk.api.organizations.acceptSuggestion
   */
  @POST(ApiPaths.User.ACCEPT_ORGANIZATION_SUGGESTION)
  suspend fun acceptOrganizationSuggestion(
    @Path("suggestion_id") suggestionId: String,
    @Query(ApiParams.CLERK_SESSION_ID) sessionId: String? = null,
  ): ClerkResult<OrganizationSuggestion, ClerkErrorResponse>

  @GET(ApiPaths.User.ORGANIZATION_MEMBERSHIPS)
  suspend fun getOrganizationMemberships(
    @Query(ApiParams.LIMIT) limit: Int? = null,
    @Query(ApiParams.OFFSET) offset: Int? = null,
    @Query("paginated") paginated: Boolean = true,
    @Query(ApiParams.CLERK_SESSION_ID) sessionId: String? = null,
  ): ClerkResult<ClerkPaginatedResponse<OrganizationMembership>, ClerkErrorResponse>

  @DELETE(ApiPaths.User.ORGANIZATION_MEMBERSHIP_WITH_ID)
  fun deleteMembership(
    @Path("organization_id") organizationId: String,
    @Query(ApiParams.CLERK_SESSION_ID) sessionId: String? = null,
  ): ClerkResult<DeletedObject, ClerkErrorResponse>

  @GET(ApiPaths.User.ORGANIZATION_INVITATIONS)
  fun getOrganizationInvitations(
    @Query("status") status: String? = null,
    @Query(ApiParams.LIMIT) limit: Int? = null,
    @Query(ApiParams.OFFSET) offset: Int? = null,
    @Query(ApiParams.CLERK_SESSION_ID) sessionId: String? = null,
  ): ClerkResult<ClerkPaginatedResponse<OrganizationInvitation>, ClerkErrorResponse>

  @GET(ApiPaths.User.ORGANIZATION_SUGGESTIONS)
  fun getOrganizationSuggestions(
    @Query("status") status: String? = null,
    @Query(ApiParams.LIMIT) limit: Int? = null,
    @Query(ApiParams.OFFSET) offset: Int? = null,
    @Query(ApiParams.CLERK_SESSION_ID) sessionId: String? = null,
  ): ClerkResult<ClerkPaginatedResponse<OrganizationSuggestion>, ClerkErrorResponse>
}
