package com.clerk.api.user

import com.clerk.api.emailaddress.EmailAddress
import com.clerk.api.externalaccount.ExternalAccount
import com.clerk.api.network.ClerkApi
import com.clerk.api.network.model.account.EnterpriseAccount
import com.clerk.api.network.model.backupcodes.BackupCodeResource
import com.clerk.api.network.model.deleted.DeletedObject
import com.clerk.api.network.model.error.ClerkErrorResponse
import com.clerk.api.network.model.image.ImageResource
import com.clerk.api.network.model.totp.TOTPResource
import com.clerk.api.network.model.verification.Verification
import com.clerk.api.network.serialization.ClerkResult
import com.clerk.api.organizations.OrganizationMembership
import com.clerk.api.passkeys.Passkey
import com.clerk.api.passkeys.PasskeyService
import com.clerk.api.phonenumber.PhoneNumber
import com.clerk.api.session.Session
import com.clerk.api.sso.OAuthProvider
import com.clerk.api.sso.RedirectConfiguration
import com.clerk.api.sso.SSOService
import com.clerk.api.user.User.CreateExternalAccountParams
import com.clerk.api.user.User.UpdateParams
import com.clerk.api.user.User.UpdatePasswordParams
import com.clerk.automap.annotations.AutoMap
import com.clerk.automap.annotations.MapProperty
import java.io.File
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

/**
 * The [User] object holds all of the information for a single user of your application and provides
 * a set of methods to manage their account.
 *
 * Each user has a unique authentication identifier which might be their email address, phone
 * number, or a username.
 *
 * A user can be contacted at their primary email address or primary phone number. They can have
 * more than one registered email address, but only one of them will be their primary email address.
 * This goes for phone numbers as well; a user can have more than one, but only one phone number
 * will be their primary. At the same time, a user can also have one or more external accounts by
 * connecting to social providers such as Google, Apple, Facebook, and many more.
 *
 * Finally, a [User] object holds profile data like the user's name, profile picture, and a set of
 * metadata that can be used internally to store arbitrary information. The metadata are split into
 * [publicMetadata] and [privateMetadata]. Both types are set from the Backend API, but public
 * metadata can also be accessed from the Frontend API.
 *
 * The Clerk SDK provides some helper methods on the User object to help retrieve and update user
 * information and authentication status.
 */
@Serializable
data class User(
  /** A boolean indicating whether the user has enabled Backup codes. */
  @SerialName("backup_code_enabled") val backupCodeEnabled: Boolean? = null,

  /** Date when the user was first created. */
  @SerialName("created_at") val createdAt: Long? = null,

  /** A boolean indicating whether the organization creation is enabled for the user or not. */
  @SerialName("create_organization_enabled") val createOrganizationEnabled: Boolean? = null,

  /**
   * An integer indicating the number of organizations that can be created by the user. If the value
   * is 0, then the user can create unlimited organizations. Default is null.
   */
  @SerialName("create_organizations_limit") val createOrganizationsLimit: Int? = null,

  /** A boolean indicating whether the user is able to delete their own account or not. */
  @SerialName("delete_self_enabled") val deleteSelfEnabled: Boolean = false,

  /** An array of all the EmailAddress objects associated with the user. Includes the primary. */
  @SerialName("email_addresses") val emailAddresses: List<EmailAddress>,

  /** A list of enterprise accounts associated with the user. */
  @SerialName("enterprise_accounts") val enterpriseAccounts: List<EnterpriseAccount>? = null,

  /**
   * An array of all the ExternalAccount objects associated with the user via OAuth. Note: This
   * includes both verified & unverified external accounts.
   */
  @SerialName("external_accounts") val externalAccounts: List<ExternalAccount>,

  /** The user's first name. */
  @SerialName("first_name") val firstName: String? = null,

  /**
   * A boolean to check if the user has uploaded an image or one was copied from OAuth. Returns
   * false if Clerk is displaying an avatar for the user.
   */
  @SerialName("has_image") @Deprecated("Use hasUploadedImage instead") val hasImage: Boolean,

  /** The unique identifier for the user. */
  val id: String,

  /** Holds the default avatar or user's uploaded profile image */
  @SerialName("image_url") val imageUrl: String,

  /** Date when the user last signed in. May be empty if the user has never signed in. */
  @SerialName("last_sign_in_at") val lastSignInAt: Long? = null,

  /** The user's last name. */
  @SerialName("last_name") val lastName: String? = null,

  /** The date on which the user accepted the legal requirements if required. */
  @SerialName("legal_accepted_at") val legalAcceptedAt: Long? = null,

  /**
   * A list of OrganizationMemberships representing the list of organizations the user is member
   * with.
   */
  @SerialName("organization_memberships")
  val organizationMemberships: List<OrganizationMembership>? = null,

  /** An array of all the Passkey objects associated with the user. */
  val passkeys: List<Passkey>,

  /** A boolean indicating whether the user has a password on their account. */
  @SerialName("password_enabled") val passwordEnabled: Boolean,

  /** An array of all the PhoneNumber objects associated with the user. Includes the primary. */
  @SerialName("phone_numbers") val phoneNumbers: List<PhoneNumber>,

  /** The unique identifier for the EmailAddress that the user has set as primary. */
  @SerialName("primary_email_address_id") val primaryEmailAddressId: String? = null,

  /** The unique identifier for the PhoneNumber that the user has set as primary. */
  @SerialName("primary_phone_number_id") val primaryPhoneNumberId: String? = null,

  /**
   * Metadata that can be read from the Frontend API and Backend API and can be set only from the
   * Backend API.
   */
  @SerialName("public_metadata") val publicMetadata: JsonObject? = null,

  /**
   * Metadata that can be read from the Frontend API and Backend API and can be set only from the
   * Backend API.
   */
  @SerialName("private_metadata") val privateMetadata: JsonObject? = null,

  /**
   * A boolean indicating whether the user has enabled TOTP by generating a TOTP secret and
   * verifying it via an authenticator app.
   */
  @SerialName("totp_enabled") val totpEnabled: Boolean,

  /** A boolean indicating whether the user has enabled two-factor authentication. */
  @SerialName("two_factor_enabled") val twoFactorEnabled: Boolean,

  /** Date of the last time the user was updated. */
  @SerialName("updated_at") val updatedAt: Long,

  /**
   * Metadata that can be read and set from the Frontend API. One common use case for this attribute
   * is to implement custom fields that will be attached to the User object. Please note that there
   * is also an unsafeMetadata attribute in the SignUp object. The value of that field will be
   * automatically copied to the user's unsafe metadata once the sign up is complete.
   */
  @SerialName("unsafe_metadata") val unsafeMetadata: JsonObject? = null,

  /** The user's username. */
  val username: String? = null,
) {

  /**
   * Parameters for updating a user's profile information.
   *
   * All fields are optional - only provide the fields you want to update. Null values will be
   * ignored and the existing values will be preserved.
   */
  @AutoMap
  @Serializable
  data class UpdateParams(
    /** The user's first name. */
    @SerialName("first_name") val firstName: String? = null,
    /** The user's last name. */
    @SerialName("last_name") val lastName: String? = null,
    /** The user's username. */
    val username: String? = null,
    /** The ID for the [EmailAddress] to be set as primary. */
    @SerialName("primary_email_address_id") val primaryEmailAddressId: String? = null,
    /** The ID for the [PhoneNumber] to be set as primary. */
    @SerialName("primary_phone_number_id") val primaryPhoneNumberId: String? = null,
    /** The ID for the image to be set as profile image. */
    @SerialName("profile_image_id") val profileImageId: String? = null,
    /** JSON string containing public metadata to update. */
    @SerialName("public_metadata") val publicMetadata: String? = null,
    /** JSON string containing private metadata to update. */
    @SerialName("private_metadata") val privateMetadata: String? = null,
  )

  /**
   * Parameters for updating a user's password.
   *
   * @property currentPassword The user's current password (required for verification)
   * @property newPassword The new password to set
   * @property signOutOfOtherSessions Whether to sign out of all other sessions after password
   *   change
   */
  @AutoMap
  @Serializable
  data class UpdatePasswordParams(
    /** The user's current password for verification. */
    @SerialName("current_password") val currentPassword: String? = null,
    /** The new password to set for the user. */
    @SerialName("new_password") val newPassword: String,
    /** Whether to sign out of all other sessions after changing the password. Default is false. */
    @SerialName("sign_out_of_other_sessions") val signOutOfOtherSessions: Boolean = false,
  )

  /**
   * Parameters for creating an external account connection.
   *
   * External accounts allow users to sign in using social providers like Google, Facebook, GitHub,
   * etc. The provider must be enabled in your Clerk Dashboard settings before it can be used.
   *
   * @property provider The OAuth provider to connect (e.g., Google, Facebook, GitHub)
   * @property redirectUrl The URL to redirect to after successful OAuth authorization
   * @property oidcPrompt Optional OpenID Connect prompt parameter
   * @property oidcLoginHint Optional OpenID Connect login hint parameter
   */
  @AutoMap
  @Serializable
  data class CreateExternalAccountParams(
    /** The strategy corresponding to the OAuth provider. For example: `oauth_google` */
    @MapProperty("providerData?.strategy") @SerialName("strategy") val provider: OAuthProvider,
    /**
     * The full URL or path that the OAuth provider should redirect to, on successful authorization
     * on their part.
     */
    @SerialName("redirect_url")
    val redirectUrl: String = RedirectConfiguration.DEFAULT_REDIRECT_URL,
    /** Optional OpenID Connect prompt parameter to control the authentication flow. */
    @SerialName("oidc_prompt") val oidcPrompt: String? = null,
    /** Optional OpenID Connect login hint parameter to pre-fill the user's identifier. */
    @SerialName("oidc_login_hint") val oidcLoginHint: String? = null,
  )

  val verifiedExternalAccounts: List<ExternalAccount>
    get() = externalAccounts.filter { it.verification?.status == Verification.Status.VERIFIED }
}

/**
 * Retrieves the current user, or the user with the given session ID, from the Clerk API.
 *
 * retrieved.
 *
 * @return A [ClerkResult] containing the [User] if the operation was successful, or a
 *   [ClerkErrorResponse] if it failed.
 */
suspend fun User.get(): ClerkResult<User, ClerkErrorResponse> = ClerkApi.user.getUser()

/**
 * Updates the current user, or the user with the given session ID, with the provided parameters.
 *
 * @param sessionId The session id of the user to update. If null, the current user is updated.
 * @param params The parameters to update the user with. **See**: [UpdateParams].
 * @return A [ClerkResult] containing the updated [User] if the operation was successful, or a
 *   [ClerkErrorResponse] if it failed.
 */
suspend fun User.update(params: UpdateParams): ClerkResult<User, ClerkErrorResponse> {
  return ClerkApi.user.updateUser(fields = params.toMap())
}

/** Deletes the current user, or the user with the given session ID, from the Clerk API. */
suspend fun User.delete(): ClerkResult<DeletedObject, ClerkErrorResponse> =
  ClerkApi.user.deleteUser()

/**
 * Update the current user's profile image, or the user with the given session ID, with the provided
 * image data.
 *
 * @param file The image file to set as the user's profile image.
 * @return A [ClerkResult] containing the [ImageResource] if the operation was successful, or a
 *   [ClerkErrorResponse] if it failed.
 */
suspend fun User.setProfileImage(file: File): ClerkResult<ImageResource, ClerkErrorResponse> {
  return UserService.setProfilePhoto(file)
}

/**
 * Deletes the current user's profile image, or the user with the given session ID, from the Clerk
 * API.
 *
 * @return A [ClerkResult] containing the [DeletedObject] if the operation was successful, or a
 *   [ClerkErrorResponse] if it failed.
 */
suspend fun User.deleteProfileImage(): ClerkResult<DeletedObject, ClerkErrorResponse> {
  return ClerkApi.user.deleteProfileImage()
}

/**
 * Updates the current user's password, or the user with the given session ID, using the Clerk API.
 *
 * @param params The parameters for updating the password.
 * @return A [ClerkResult] containing the [User] if the operation was successful, or a
 *   [ClerkErrorResponse] if it failed.
 *
 * **See:** [UpdatePasswordParams] for the available parameters.
 */
suspend fun User.updatePassword(
  params: UpdatePasswordParams
): ClerkResult<User, ClerkErrorResponse> {
  return ClerkApi.user.updatePassword(params.toMap())
}

/**
 * Deletes the current user's password, or the user with the given session ID, using the Clerk API.
 *
 * @param currentPassword The current password of the user. If null, the password is deleted without
 *   verification.
 * @return A [ClerkResult] containing the [User] if the operation was successful, or a
 *   [ClerkErrorResponse] if it failed.
 */
suspend fun User.deletePassword(currentPassword: String): ClerkResult<User, ClerkErrorResponse> {
  return ClerkApi.user.deletePassword(currentPassword)
}

/**
 * Retrieves the active sessions for the current user or the user with the given session ID.
 *
 * Active sessions are sessions that are currently valid and can be used for authentication. This
 * excludes expired or revoked sessions.
 *
 * @return A [ClerkResult] containing a list of active [Session] objects on success, or a
 *   [ClerkErrorResponse] on failure
 */
suspend fun User.activeSessions(): ClerkResult<List<Session>, ClerkErrorResponse> {
  return ClerkApi.user.getActiveSessions()
}

/**
 * Retrieves all sessions for the current user or the user with the given session ID.
 *
 * This includes both active and inactive (expired/revoked) sessions, providing a complete history
 * of the user's authentication sessions.
 *
 * @return A [ClerkResult] containing a list of all [Session] objects on success, or a
 *   [ClerkErrorResponse] on failure
 */
suspend fun User.allSessions(): ClerkResult<List<Session>, ClerkErrorResponse> {
  return ClerkApi.user.getSessions()
}

/**
 * Retrieves all email addresses associated with the current user or the user with the given session
 * ID.
 *
 * This includes both verified and unverified email addresses, including the primary email address.
 *
 * @return A [ClerkResult] containing a list of [EmailAddress] objects on success, or a
 *   [ClerkErrorResponse] on failure
 */
suspend fun User.emailAddresses(): ClerkResult<List<EmailAddress>, ClerkErrorResponse> {
  return ClerkApi.user.getEmailAddresses()
}

/**
 * Creates a new email address for the current user or the user with the given session ID.
 *
 * The newly created email address will be unverified initially. The user will need to complete the
 * verification process before the email address can be used for authentication.
 *
 * @param email The email address to add to the user's account
 * @return A [ClerkResult] containing the created [EmailAddress] object on success, or a
 *   [ClerkErrorResponse] on failure
 */
suspend fun User.createEmailAddress(email: String): ClerkResult<EmailAddress, ClerkErrorResponse> {
  return ClerkApi.user.createEmailAddress(emailAddress = email)
}

/**
 * Retrieves all phone numbers associated with the current user or the user with the given session
 * ID.
 *
 * This includes both verified and unverified phone numbers, including the primary phone number.
 *
 * @return A [ClerkResult] containing a list of [PhoneNumber] objects on success, or a
 *   [ClerkErrorResponse] on failure
 */
suspend fun User.phoneNumbers(): ClerkResult<List<PhoneNumber>, ClerkErrorResponse> {
  return ClerkApi.user.getPhoneNumbers()
}

/**
 * Creates a new phone number for the current user or the user with the given session ID.
 *
 * The newly created phone number will be unverified initially. The user will need to complete the
 * verification process (typically via SMS) before the phone number can be used for authentication
 * or two-factor authentication.
 *
 * @param phoneNumber The phone number to add to the user's account (should include country code)
 * @return A [ClerkResult] containing the created [PhoneNumber] object on success, or a
 *   [ClerkErrorResponse] on failure
 */
suspend fun User.createPhoneNumber(
  phoneNumber: String
): ClerkResult<PhoneNumber, ClerkErrorResponse> {
  return ClerkApi.user.createPhoneNumber(phoneNumber)
}

/**
 * Creates a new passkey for the current user or the user with the given session ID.
 *
 * Passkeys are a modern, secure authentication method that uses cryptographic key pairs. The
 * creation process will typically prompt the user to use their device's biometric authentication
 * (fingerprint, face recognition) or device PIN to create the passkey.
 *
 * @return A [ClerkResult] containing the created [Passkey] object on success, or a
 *   [ClerkErrorResponse] on failure
 */
suspend fun User.createPasskey(): ClerkResult<Passkey, ClerkErrorResponse> {
  return PasskeyService.createPasskey()
}

/**
 * Adds an external account for the user. A new [ExternalAccount] will be created and associated
 * with the user. This method is useful if you want to allow an already signed-in user to connect
 * their account with an external provider, such as Facebook, GitHub, etc., so that they can sign in
 * with that provider in the future.
 *
 * **Note:** The social provider that you want to connect to must be enabled in your app's settings
 * in the Clerk Dashboard. See: (Social
 * connections)[https://clerk.com/docs/authentication/configuration/sign-up-sign-in-options#social-connections-o-auth]
 *
 * After calling `createExternalAccount`, the initial state of the returned [ExternalAccount] will
 * be unverified. To initiate the connection with the external provider, redirect the user to the
 * [com.clerk.network.model.verification.Verification.externalVerificationRedirectUrl] contained in
 * the result of the `createExternalAccount` call.
 *
 * Upon return, inspect within the user.externalAccounts the entry that corresponds to the requested
 * strategy:
 * - If the connection succeeded, then externalAccount.verification.status will be verified.
 * - If the connection failed, then the externalAccount.verification.status will not be verified and
 *   the externalAccount.verification.error will contain the error encountered, which you can
 *   present to the user. To learn more about the properties available on verification, see the
 *   verification reference.
 */
suspend fun User.createExternalAccount(
  params: CreateExternalAccountParams
): ClerkResult<ExternalAccount, ClerkErrorResponse> {
  return SSOService.connectExternalAccount(params)
}

/**
 * Creates a new TOTP (Time-based One-Time Password) configuration for the current user.
 *
 * TOTP is commonly used for two-factor authentication with authenticator apps like Google
 * Authenticator, Authy, or 1Password. This method generates a secret key that can be used to set up
 * the authenticator app.
 *
 * After calling this method, the user will need to scan a QR code or manually enter the secret into
 * their authenticator app, then verify it using [attemptTOTPVerification].
 *
 * @return A [ClerkResult] containing the [TOTPResource] with setup information on success, or a
 *   [ClerkErrorResponse] on failure
 */
suspend fun User.createTOTP(): ClerkResult<TOTPResource, ClerkErrorResponse> {
  return ClerkApi.user.createTOTP()
}

/**
 * Deletes the TOTP (Time-based One-Time Password) configuration for the current user.
 *
 * This removes the user's TOTP setup, disabling two-factor authentication via authenticator apps.
 * The user will no longer be able to use TOTP codes for authentication until they set up TOTP
 * again.
 *
 * @return A [ClerkResult] containing a [DeletedObject] on success, or a [ClerkErrorResponse] on
 *   failure
 */
suspend fun User.deleteTOTP(): ClerkResult<DeletedObject, ClerkErrorResponse> {
  return ClerkApi.user.deleteTOTP()
}

/**
 * Verifies a TOTP (Time-based One-Time Password) code to complete the TOTP setup process.
 *
 * This method should be called after [createTOTP] to verify that the user has correctly configured
 * their authenticator app. The user should provide a 6-digit code generated by their authenticator
 * app.
 *
 * @param code The 6-digit TOTP code generated by the user's authenticator app
 * @return A [ClerkResult] containing the verified [TOTPResource] on success, or a
 *   [ClerkErrorResponse] on failure
 */
suspend fun User.attemptTOTPVerification(
  code: String
): ClerkResult<TOTPResource, ClerkErrorResponse> {
  return ClerkApi.user.attemptTOTPVerification(code)
}

/**
 * Generates backup codes for the current user's account.
 *
 * Backup codes are single-use recovery codes that can be used for authentication when the user's
 * primary two-factor authentication method (like TOTP or SMS) is unavailable. These codes should be
 * stored securely by the user.
 *
 * @return A [ClerkResult] containing the [BackupCodeResource] with the generated backup codes on
 *   success, or a [ClerkErrorResponse] on failure
 */
suspend fun User.createBackupCodes(): ClerkResult<BackupCodeResource, ClerkErrorResponse> {
  return ClerkApi.user.createBackupCodes()
}
