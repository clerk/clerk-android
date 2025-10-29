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
import com.clerk.api.organizations.UserOrganizationInvitation
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

/**
 * Internal API interface for user-related operations in the Clerk authentication system.
 *
 * This interface defines all the network endpoints for managing user data, authentication, and
 * related resources such as email addresses, phone numbers, passkeys, external accounts, and
 * organization memberships.
 *
 * All methods in this interface return [ClerkResult] which provides type-safe error handling for
 * successful responses and [ClerkErrorResponse] for failures.
 */
internal interface UserApi {

  /**
   * Retrieves the current user's information.
   *
   * @param sessionId Optional session ID. Defaults to current session ID from [Clerk.session]
   * @return [ClerkResult] containing the [User] on success or [ClerkErrorResponse] on failure
   * @see [com.clerk.api.user.get]
   */
  @GET(ApiPaths.User.BASE)
  suspend fun getUser(
    @Query(ApiParams.CLERK_SESSION_ID) sessionId: String? = Clerk.session?.id
  ): ClerkResult<User, ClerkErrorResponse>

  /**
   * Updates the current user's information.
   *
   * @param fields Map of field names to values for updating user properties
   * @param sessionId Optional session ID. Defaults to current session ID from [Clerk.session]
   * @return [ClerkResult] containing the updated [User] on success or [ClerkErrorResponse] on
   *   failure
   * @see [com.clerk.api.user.update]
   */
  @PATCH(ApiPaths.User.BASE)
  @FormUrlEncoded
  suspend fun updateUser(
    @FieldMap fields: Map<String, String>,
    @Query(ApiParams.CLERK_SESSION_ID) sessionId: String? = Clerk.session?.id,
  ): ClerkResult<User, ClerkErrorResponse>

  /**
   * Deletes the current user account.
   *
   * @param sessionId Optional session ID. Defaults to current session ID from [Clerk.session]
   * @return [ClerkResult] containing [DeletedObject] on success or [ClerkErrorResponse] on failure
   * @see [com.clerk.user.User.delete]
   */
  @DELETE(ApiPaths.User.BASE)
  suspend fun deleteUser(
    @Query(ApiParams.CLERK_SESSION_ID) sessionId: String? = Clerk.session?.id
  ): ClerkResult<DeletedObject, ClerkErrorResponse>

  /**
   * Retrieves all sessions for the current user.
   *
   * @param sessionId Optional session ID. Defaults to current session ID from [Clerk.session]
   * @return [ClerkResult] containing a list of [Session] objects on success or [ClerkErrorResponse]
   *   on failure
   */
  @GET(ApiPaths.User.Sessions.BASE)
  suspend fun getSessions(
    @Query(ApiParams.CLERK_SESSION_ID) sessionId: String? = Clerk.session?.id
  ): ClerkResult<List<Session>, ClerkErrorResponse>

  /**
   * Sets or updates the user's profile image.
   *
   * @param file Multipart file containing the image data
   * @param sessionId Optional session ID. Defaults to current session ID from [Clerk.session]
   * @return [ClerkResult] containing [ImageResource] on success or [ClerkErrorResponse] on failure
   */
  @Multipart
  @POST(ApiPaths.User.PROFILE_IMAGE)
  suspend fun setProfileImage(
    @Part file: MultipartBody.Part,
    @Query(ApiParams.CLERK_SESSION_ID) sessionId: String? = Clerk.session?.id,
  ): ClerkResult<ImageResource, ClerkErrorResponse>

  /**
   * Deletes the user's profile image.
   *
   * @param sessionId Optional session ID. Defaults to current session ID from [Clerk.session]
   * @return [ClerkResult] containing [DeletedObject] on success or [ClerkErrorResponse] on failure
   */
  @DELETE(ApiPaths.User.PROFILE_IMAGE)
  suspend fun deleteProfileImage(
    @Query(ApiParams.CLERK_SESSION_ID) sessionId: String? = Clerk.session?.id
  ): ClerkResult<DeletedObject, ClerkErrorResponse>

  /**
   * Updates the user's password.
   *
   * @param fields Map containing password update parameters (e.g., current_password, new_password)
   * @param sessionId Optional session ID. Defaults to current session ID from [Clerk.session]
   * @return [ClerkResult] containing the updated [User] on success or [ClerkErrorResponse] on
   *   failure
   */
  @FormUrlEncoded
  @POST(ApiPaths.User.Password.UPDATE)
  suspend fun updatePassword(
    @FieldMap fields: Map<String, String>,
    @Query(ApiParams.CLERK_SESSION_ID) sessionId: String? = Clerk.session?.id,
  ): ClerkResult<User, ClerkErrorResponse>

  /**
   * Deletes the user's password (removes password-based authentication).
   *
   * @param password Current password for verification
   * @param sessionId Optional session ID. Defaults to current session ID from [Clerk.session]
   * @return [ClerkResult] containing the updated [User] on success or [ClerkErrorResponse] on
   *   failure
   */
  @FormUrlEncoded
  @POST
  suspend fun deletePassword(
    @Field("current_password") password: String,
    @Query(ApiParams.CLERK_SESSION_ID) sessionId: String? = Clerk.session?.id,
  ): ClerkResult<User, ClerkErrorResponse>

  /**
   * Retrieves all active sessions for the current user.
   *
   * @param sessionId Optional session ID. Defaults to current session ID from [Clerk.session]
   * @return [ClerkResult] containing a list of active [Session] objects on success or
   *   [ClerkErrorResponse] on failure
   */
  @GET(ApiPaths.User.Sessions.ACTIVE)
  suspend fun getActiveSessions(
    @Query(ApiParams.CLERK_SESSION_ID) sessionId: String? = Clerk.session?.id
  ): ClerkResult<List<Session>, ClerkErrorResponse>

  /**
   * Retrieves all email addresses associated with the current user.
   *
   * @param sessionId Optional session ID. Defaults to current session ID from [Clerk.session]
   * @return [ClerkResult] containing a list of [EmailAddress] objects on success or
   *   [ClerkErrorResponse] on failure
   */
  @GET(ApiPaths.User.EmailAddress.BASE)
  suspend fun getEmailAddresses(
    @Query(ApiParams.CLERK_SESSION_ID) sessionId: String? = Clerk.session?.id
  ): ClerkResult<List<EmailAddress>, ClerkErrorResponse>

  /**
   * Creates a new email address for the current user.
   *
   * @param emailAddress The email address to add
   * @param sessionId Optional session ID. Defaults to current user ID from [Clerk.session]
   * @return [ClerkResult] containing the created [EmailAddress] on success or [ClerkErrorResponse]
   *   on failure
   */
  @FormUrlEncoded
  @POST(ApiPaths.User.EmailAddress.BASE)
  suspend fun createEmailAddress(
    @Field("email_address") emailAddress: String,
    @Query(ApiParams.CLERK_SESSION_ID) sessionId: String? = Clerk.session?.user?.id,
  ): ClerkResult<EmailAddress, ClerkErrorResponse>

  /**
   * Attempts to verify an email address using a verification code.
   *
   * @param emailAddressId The ID of the email address to verify
   * @param code The verification code received via email
   * @param sessionId Optional session ID. Defaults to current session ID from [Clerk.session]
   * @return [ClerkResult] containing the verified [EmailAddress] on success or [ClerkErrorResponse]
   *   on failure
   */
  @FormUrlEncoded
  @POST(ApiPaths.User.EmailAddress.ATTEMPT_VERIFICATION)
  suspend fun attemptEmailAddressVerification(
    @Path(ApiParams.EMAIL_ID) emailAddressId: String,
    @Field(ApiParams.CODE) code: String,
    @Query(ApiParams.CLERK_SESSION_ID) sessionId: String? = Clerk.session?.id,
  ): ClerkResult<EmailAddress, ClerkErrorResponse>

  /**
   * Prepares email address verification by sending a verification code.
   *
   * @param emailAddressId The ID of the email address to prepare for verification
   * @param params Additional parameters for the verification preparation
   * @param sessionId Optional session ID. Defaults to current session ID from [Clerk.session]
   * @return [ClerkResult] containing the [EmailAddress] with verification details on success or
   *   [ClerkErrorResponse] on failure
   */
  @FormUrlEncoded
  @POST(ApiPaths.User.EmailAddress.PREPARE_VERIFICATION)
  suspend fun prepareEmailAddressVerification(
    @Path(ApiParams.EMAIL_ID) emailAddressId: String,
    @FieldMap params: Map<String, String>,
    @Query(ApiParams.CLERK_SESSION_ID) sessionId: String? = Clerk.session?.id,
  ): ClerkResult<EmailAddress, ClerkErrorResponse>

  /**
   * Retrieves a specific email address by its ID.
   *
   * @param emailAddressId The ID of the email address to retrieve
   * @param sessionId Optional session ID. Defaults to current session ID from [Clerk.session]
   * @return [ClerkResult] containing the [EmailAddress] on success or [ClerkErrorResponse] on
   *   failure
   */
  @GET(ApiPaths.User.EmailAddress.WITH_ID)
  suspend fun getEmailAddress(
    @Path(ApiParams.EMAIL_ID) emailAddressId: String,
    @Query(ApiParams.CLERK_SESSION_ID) sessionId: String? = Clerk.session?.id,
  ): ClerkResult<EmailAddress, ClerkErrorResponse>

  /**
   * Deletes a specific email address.
   *
   * @param emailAddressId The ID of the email address to delete
   * @param sessionId Optional session ID. Defaults to current session ID from [Clerk.session]
   * @return [ClerkResult] containing [DeletedObject] on success or [ClerkErrorResponse] on failure
   */
  @DELETE(ApiPaths.User.EmailAddress.WITH_ID)
  suspend fun deleteEmailAddress(
    @Path(ApiParams.EMAIL_ID) emailAddressId: String,
    @Query(ApiParams.CLERK_SESSION_ID) sessionId: String? = Clerk.session?.id,
  ): ClerkResult<DeletedObject, ClerkErrorResponse>

  /**
   * Retrieves all phone numbers associated with the current user.
   *
   * @param sessionId Optional session ID. Defaults to current session ID from [Clerk.session]
   * @return [ClerkResult] containing a list of [PhoneNumber] objects on success or
   *   [ClerkErrorResponse] on failure
   */
  @GET(ApiPaths.User.PhoneNumber.BASE)
  suspend fun getPhoneNumbers(
    @Query(ApiParams.CLERK_SESSION_ID) sessionId: String? = Clerk.session?.id
  ): ClerkResult<List<PhoneNumber>, ClerkErrorResponse>

  /**
   * Creates a new phone number for the current user.
   *
   * @param phoneNumber The phone number to add
   * @param reservedForSecondFactor Whether this phone number should be reserved for second factor
   *   authentication
   * @param sessionId Optional session ID. Defaults to current session ID from [Clerk.session]
   * @return [ClerkResult] containing the created [PhoneNumber] on success or [ClerkErrorResponse]
   *   on failure
   */
  @FormUrlEncoded
  @POST(ApiPaths.User.PhoneNumber.BASE)
  suspend fun createPhoneNumber(
    @Field("phone_number") phoneNumber: String,
    @Field("reserved_for_second_factor") reservedForSecondFactor: Boolean = false,
    @Query(ApiParams.CLERK_SESSION_ID) sessionId: String? = Clerk.session?.id,
  ): ClerkResult<PhoneNumber, ClerkErrorResponse>

  /**
   * Attempts to verify a phone number using a verification code.
   *
   * @param phoneNumberId The ID of the phone number to verify
   * @param code The verification code received via SMS
   * @param sessionId Optional session ID. Defaults to current session ID from [Clerk.session]
   * @return [ClerkResult] containing the verified [PhoneNumber] on success or [ClerkErrorResponse]
   *   on failure
   */
  @FormUrlEncoded
  @POST(ApiPaths.User.PhoneNumber.ATTEMPT_VERIFICATION)
  suspend fun attemptPhoneNumberVerification(
    @Path(ApiParams.PHONE_NUMBER_ID) phoneNumberId: String,
    @Field(ApiParams.CODE) code: String,
    @Query(ApiParams.CLERK_SESSION_ID) sessionId: String? = Clerk.session?.id,
  ): ClerkResult<PhoneNumber, ClerkErrorResponse>

  /**
   * Prepares phone number verification by sending a verification code.
   *
   * @param phoneNumberId The ID of the phone number to prepare for verification
   * @param strategy The verification strategy to use (e.g., "sms")
   * @param sessionId Optional session ID. Defaults to current session ID from [Clerk.session]
   * @return [ClerkResult] containing the [PhoneNumber] with verification details on success or
   *   [ClerkErrorResponse] on failure
   */
  @FormUrlEncoded
  @POST(ApiPaths.User.PhoneNumber.PREPARE_VERIFICATION)
  suspend fun preparePhoneNumberVerification(
    @Path(ApiParams.PHONE_NUMBER_ID) phoneNumberId: String,
    @Field(ApiParams.STRATEGY) strategy: String,
    @Query(ApiParams.CLERK_SESSION_ID) sessionId: String? = Clerk.session?.id,
  ): ClerkResult<PhoneNumber, ClerkErrorResponse>

  /**
   * Retrieves a specific phone number by its ID.
   *
   * @param phoneNumberId The ID of the phone number to retrieve
   * @param sessionId Optional session ID. Defaults to current session ID from [Clerk.session]
   * @return [ClerkResult] containing the [PhoneNumber] on success or [ClerkErrorResponse] on
   *   failure
   */
  @GET(ApiPaths.User.PhoneNumber.WITH_ID)
  suspend fun getPhoneNumber(
    @Path(ApiParams.PHONE_NUMBER_ID) phoneNumberId: String,
    @Query(ApiParams.CLERK_SESSION_ID) sessionId: String? = Clerk.session?.id,
  ): ClerkResult<PhoneNumber, ClerkErrorResponse>

  /**
   * Deletes a specific phone number.
   *
   * @param phoneNumberId The ID of the phone number to delete
   * @param sessionId Optional session ID. Defaults to current session ID from [Clerk.session]
   * @return [ClerkResult] containing [DeletedObject] on success or [ClerkErrorResponse] on failure
   */
  @DELETE(ApiPaths.User.PhoneNumber.WITH_ID)
  suspend fun deletePhoneNumber(
    @Path(ApiParams.PHONE_NUMBER_ID) phoneNumberId: String,
    @Query(ApiParams.CLERK_SESSION_ID) sessionId: String? = Clerk.session?.id,
  ): ClerkResult<DeletedObject, ClerkErrorResponse>

  /**
   * Updates properties of a specific phone number.
   *
   * @param phoneNumberId The ID of the phone number to update
   * @param reservedForSecondFactor Optional: Whether this phone number should be reserved for
   *   second factor authentication
   * @param defaultSecondFactor Optional: Whether this phone number should be the default for second
   *   factor authentication
   * @param sessionId Optional session ID. Defaults to current session ID from [Clerk.session]
   * @return [ClerkResult] containing the updated [PhoneNumber] on success or [ClerkErrorResponse]
   *   on failure
   */
  @FormUrlEncoded
  @PATCH(ApiPaths.User.PhoneNumber.WITH_ID)
  suspend fun updatePhoneNumber(
    @Path(ApiParams.PHONE_NUMBER_ID) phoneNumberId: String,
    @Field("reserved_for_second_factor") reservedForSecondFactor: Boolean? = null,
    @Field("default_second_factor") defaultSecondFactor: Boolean? = null,
    @Query(ApiParams.CLERK_SESSION_ID) sessionId: String? = Clerk.session?.id,
  ): ClerkResult<PhoneNumber, ClerkErrorResponse>

  /**
   * Creates a new passkey for the current user.
   *
   * @param sessionId Optional session ID. Defaults to current session ID from [Clerk.session]
   * @return [ClerkResult] containing the created [Passkey] on success or [ClerkErrorResponse] on
   *   failure
   */
  @POST(ApiPaths.User.Passkey.BASE)
  suspend fun createPasskey(
    @Query(ApiParams.CLERK_SESSION_ID) sessionId: String? = Clerk.session?.id
  ): ClerkResult<Passkey, ClerkErrorResponse>

  /**
   * Retrieves a specific passkey by its ID.
   *
   * @param passkeyId The ID of the passkey to retrieve
   * @return [ClerkResult] containing the [Passkey] on success or [ClerkErrorResponse] on failure
   */
  @GET(ApiPaths.User.Passkey.WITH_ID)
  suspend fun getPasskey(
    @Path(ApiParams.PASSKEY_ID) passkeyId: String
  ): ClerkResult<Passkey, ClerkErrorResponse>

  /**
   * Deletes a specific passkey.
   *
   * @param passkeyId The ID of the passkey to delete
   * @return [ClerkResult] containing [DeletedObject] on success or [ClerkErrorResponse] on failure
   */
  @DELETE(ApiPaths.User.Passkey.WITH_ID)
  suspend fun deletePasskey(
    @Path(ApiParams.PASSKEY_ID) passkeyId: String
  ): ClerkResult<DeletedObject, ClerkErrorResponse>

  /**
   * Updates properties of a specific passkey.
   *
   * @param passkeyId The ID of the passkey to update
   * @param name Optional: New name for the passkey
   * @return [ClerkResult] containing the updated [Passkey] on success or [ClerkErrorResponse] on
   *   failure
   */
  @FormUrlEncoded
  @PATCH(ApiPaths.User.Passkey.WITH_ID)
  suspend fun updatePasskey(
    @Path(ApiParams.PASSKEY_ID) passkeyId: String,
    @Field("name") name: String? = null,
  ): ClerkResult<Passkey, ClerkErrorResponse>

  /**
   * Attempts to verify a passkey using WebAuthn credentials.
   *
   * @param passkeyId The ID of the passkey to verify
   * @param strategy The verification strategy (defaults to "passkey")
   * @param publicKeyCredential The WebAuthn public key credential data
   * @return [ClerkResult] containing the verified [Passkey] on success or [ClerkErrorResponse] on
   *   failure
   */
  @FormUrlEncoded
  @POST(ApiPaths.User.Passkey.ATTEMPT_VERIFICATION)
  suspend fun attemptPasskeyVerification(
    @Path(ApiParams.PASSKEY_ID) passkeyId: String,
    @Field(ApiParams.STRATEGY) strategy: String = "passkey",
    @Field("public_key_credential") publicKeyCredential: String,
  ): ClerkResult<Passkey, ClerkErrorResponse>

  /**
   * Creates a new external account connection for the user.
   *
   * @param params Parameters for creating the external account (provider, redirect URL, etc.)
   * @return [ClerkResult] containing the created [ExternalAccount] on success or
   *   [ClerkErrorResponse] on failure
   */
  @FormUrlEncoded
  @POST(ApiPaths.User.ExternalAccount.BASE)
  suspend fun createExternalAccount(
    @FieldMap params: Map<String, String>
  ): ClerkResult<ExternalAccount, ClerkErrorResponse>

  /**
   * Reauthorizes an existing external account connection.
   *
   * @param externalAccountId The ID of the external account to reauthorize
   * @param redirectUrl The URL to redirect to after reauthorization
   * @return [ClerkResult] containing the reauthorized [ExternalAccount] on success or
   *   [ClerkErrorResponse] on failure
   */
  @FormUrlEncoded
  @PATCH(ApiPaths.User.ExternalAccount.REAUTHORIZE)
  suspend fun reauthorizeExternalAccount(
    @Path(ApiParams.EXTERNAL_ACCOUNT_ID) externalAccountId: String,
    @Field("redirect_url") redirectUrl: String,
  ): ClerkResult<ExternalAccount, ClerkErrorResponse>

  /**
   * Deletes an external account connection.
   *
   * @param externalAccountId The ID of the external account to delete
   * @return [ClerkResult] containing [DeletedObject] on success or [ClerkErrorResponse] on failure
   */
  @DELETE(ApiPaths.User.ExternalAccount.WITH_ID)
  suspend fun deleteExternalAccount(
    @Path(ApiParams.EXTERNAL_ACCOUNT_ID) externalAccountId: String
  ): ClerkResult<DeletedObject, ClerkErrorResponse>

  /**
   * Revokes tokens for an external account connection.
   *
   * @param externalAccountId The ID of the external account whose tokens should be revoked
   * @return [ClerkResult] containing the updated [User] on success or [ClerkErrorResponse] on
   *   failure
   */
  @DELETE(ApiPaths.User.ExternalAccount.REVOKE_TOKENS)
  suspend fun revokeExternalAccountTokens(
    @Path(ApiParams.EXTERNAL_ACCOUNT_ID) externalAccountId: String
  ): ClerkResult<User, ClerkErrorResponse>

  /**
   * Creates a new TOTP (Time-based One-Time Password) authenticator for the user.
   *
   * @return [ClerkResult] containing the created [TOTPResource] on success or [ClerkErrorResponse]
   *   on failure
   */
  @POST(ApiPaths.User.TOTP.BASE)
  suspend fun createTOTP(): ClerkResult<TOTPResource, ClerkErrorResponse>

  /**
   * Deletes the user's TOTP authenticator.
   *
   * @return [ClerkResult] containing [DeletedObject] on success or [ClerkErrorResponse] on failure
   */
  @DELETE(ApiPaths.User.TOTP.BASE)
  suspend fun deleteTOTP(): ClerkResult<DeletedObject, ClerkErrorResponse>

  /**
   * Attempts to verify a TOTP code.
   *
   * @param code The TOTP code to verify
   * @return [ClerkResult] containing the verified [TOTPResource] on success or [ClerkErrorResponse]
   *   on failure
   */
  @FormUrlEncoded
  @POST(ApiPaths.User.TOTP.ATTEMPT_VERIFICATION)
  suspend fun attemptTOTPVerification(
    @Field(ApiParams.CODE) code: String
  ): ClerkResult<TOTPResource, ClerkErrorResponse>

  /**
   * Creates backup codes for the user's account recovery.
   *
   * @return [ClerkResult] containing the created [BackupCodeResource] on success or
   *   [ClerkErrorResponse] on failure
   */
  @POST(ApiPaths.User.BACKUP_CODES)
  suspend fun createBackupCodes(): ClerkResult<BackupCodeResource, ClerkErrorResponse>

  /**
   * Accepts a user organization invitation.
   *
   * @param invitationId The unique identifier of the invitation to accept
   * @param sessionId Optional session ID for the operation
   * @return A [ClerkResult] containing either the accepted [OrganizationInvitation] on success or a
   *   [ClerkErrorResponse] on failure
   * @see com.clerk.api.organizations.accept
   */
  @POST(ApiPaths.User.ACCEPT_ORGANIZATION_INVITATION)
  suspend fun acceptUserOrganizationInvitation(
    @Path("invitation_id") invitationId: String,
    @Query(ApiParams.CLERK_SESSION_ID) sessionId: String? = null,
  ): ClerkResult<UserOrganizationInvitation, ClerkErrorResponse>

  /**
   * Accepts an organization suggestion for a user.
   *
   * @param suggestionId The unique identifier of the suggestion to accept
   * @param sessionId Optional session ID for the operation
   * @return A [ClerkResult] containing either the accepted [OrganizationSuggestion] on success or a
   *   [ClerkErrorResponse] on failure
   * @see com.clerk.api.organizations.accept
   */
  @POST(ApiPaths.User.ACCEPT_ORGANIZATION_SUGGESTION)
  suspend fun acceptOrganizationSuggestion(
    @Path("suggestion_id") suggestionId: String,
    @Query(ApiParams.CLERK_SESSION_ID) sessionId: String? = null,
  ): ClerkResult<OrganizationSuggestion, ClerkErrorResponse>

  /**
   * Retrieves organization memberships for the current user.
   *
   * @param limit Maximum number of memberships to return
   * @param offset Number of memberships to skip for pagination
   * @param paginated Whether to return paginated results (default: true)
   * @param sessionId Optional session ID for the operation
   * @return [ClerkResult] containing paginated [OrganizationMembership] list on success or
   *   [ClerkErrorResponse] on failure
   */
  @GET(ApiPaths.User.ORGANIZATION_MEMBERSHIPS)
  suspend fun getOrganizationMemberships(
    @Query(ApiParams.LIMIT) limit: Int? = null,
    @Query(ApiParams.OFFSET) offset: Int? = null,
    @Query("paginated") paginated: Boolean = true,
    @Query(ApiParams.CLERK_SESSION_ID) sessionId: String? = null,
  ): ClerkResult<ClerkPaginatedResponse<OrganizationMembership>, ClerkErrorResponse>

  /**
   * Deletes a user's membership from an organization.
   *
   * @param organizationId The ID of the organization to leave
   * @param sessionId Optional session ID for the operation
   * @return [ClerkResult] containing [DeletedObject] on success or [ClerkErrorResponse] on failure
   */
  @DELETE(ApiPaths.User.ORGANIZATION_MEMBERSHIP_WITH_ID)
  suspend fun deleteMembership(
    @Path("organization_id") organizationId: String,
    @Query(ApiParams.CLERK_SESSION_ID) sessionId: String? = null,
  ): ClerkResult<DeletedObject, ClerkErrorResponse>

  /**
   * Retrieves organization invitations for the current user.
   *
   * @param status Optional status filter for invitations (e.g., "pending", "accepted")
   * @param limit Maximum number of invitations to return
   * @param offset Number of invitations to skip for pagination
   * @param sessionId Optional session ID for the operation
   * @return [ClerkResult] containing paginated [UserOrganizationInvitation] list on success or
   *   [ClerkErrorResponse] on failure
   */
  @GET(ApiPaths.User.ORGANIZATION_INVITATIONS)
  suspend fun getOrganizationInvitations(
    @Query("status") status: String? = null,
    @Query(ApiParams.LIMIT) limit: Int? = null,
    @Query(ApiParams.OFFSET) offset: Int? = null,
    @Query(ApiParams.CLERK_SESSION_ID) sessionId: String? = null,
  ): ClerkResult<ClerkPaginatedResponse<UserOrganizationInvitation>, ClerkErrorResponse>

  /**
   * Retrieves organization suggestions for the current user.
   *
   * @param status Optional status filter for suggestions (e.g., "pending", "accepted")
   * @param limit Maximum number of suggestions to return
   * @param offset Number of suggestions to skip for pagination
   * @param sessionId Optional session ID for the operation
   * @return [ClerkResult] containing paginated [OrganizationSuggestion] list on success or
   *   [ClerkErrorResponse] on failure
   */
  @GET(ApiPaths.User.ORGANIZATION_SUGGESTIONS)
  suspend fun getOrganizationSuggestions(
    @Query("status") status: String? = null,
    @Query(ApiParams.LIMIT) limit: Int? = null,
    @Query(ApiParams.OFFSET) offset: Int? = null,
    @Query(ApiParams.CLERK_SESSION_ID) sessionId: String? = null,
  ): ClerkResult<ClerkPaginatedResponse<OrganizationSuggestion>, ClerkErrorResponse>

  @FormUrlEncoded
  @PATCH(ApiPaths.User.PhoneNumber.WITH_ID)
  suspend fun setReservedForSecondFactor(
    @Path(ApiParams.PHONE_NUMBER_ID) phoneNumberId: String,
    @Field("reserved_for_second_factor") reservedForSecondFactor: Boolean,
    @Query(ApiParams.CLERK_SESSION_ID) sessionId: String? = Clerk.session?.id,
  ): ClerkResult<PhoneNumber, ClerkErrorResponse>
}
