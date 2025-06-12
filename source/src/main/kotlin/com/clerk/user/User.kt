package com.clerk.user

import com.clerk.automap.annotations.AutoMap
import com.clerk.network.ClerkApi
import com.clerk.network.model.account.EnterpriseAccount
import com.clerk.network.model.account.ExternalAccount
import com.clerk.network.model.deleted.DeletedObject
import com.clerk.network.model.emailaddress.EmailAddress
import com.clerk.network.model.error.ClerkErrorResponse
import com.clerk.network.model.image.ImageResource
import com.clerk.network.model.organization.OrganizationMembership
import com.clerk.network.model.passkey.Passkey
import com.clerk.network.model.phonenumber.PhoneNumber
import com.clerk.network.serialization.ClerkResult
import com.clerk.session.Session
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import okhttp3.MultipartBody

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
  @SerialName("backup_code_enabled") val backupCodeEnabled: Boolean,

  /** Date when the user was first created. */
  @SerialName("created_at") val createdAt: Long,

  /** A boolean indicating whether the organization creation is enabled for the user or not. */
  @SerialName("create_organization_enabled") val createOrganizationEnabled: Boolean,

  /**
   * An integer indicating the number of organizations that can be created by the user. If the value
   * is 0, then the user can create unlimited organizations. Default is null.
   */
  @SerialName("create_organizations_limit") val createOrganizationsLimit: Int? = null,

  /** A boolean indicating whether the user is able to delete their own account or not. */
  @SerialName("delete_self_enabled") val deleteSelfEnabled: Boolean,

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
  @SerialName("organization_memberships") val organizationMemberships: List<OrganizationMembership>,

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

  /** Parameters for updating a user. */
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
    @SerialName("public_metadata") val publicMetadata: String? = null,
    @SerialName("private_metadata") val privateMetadata: String? = null,
  )

  @AutoMap
  @Serializable
  data class UpdatePasswordParams(
    @SerialName("current_password") val currentPassword: String? = null,
    @SerialName("new_password") val newPassword: String,
    val signOutOfOtherSessions: Boolean = false,
  ) {

    companion object {
      /**
       * Retrieves the current user, or the user with the given session ID, from the Clerk API.
       *
       * @param sessionId The session id of the user to retrieve. If null, the current user is
       *   retrieved.
       * @return A [ClerkResult] containing the [User] if the operation was successful, or a
       *   [ClerkErrorResponse] if it failed.
       */
      suspend fun get(sessionId: String? = null): ClerkResult<User, ClerkErrorResponse> =
        ClerkApi.user.getUser(sessionId)

      /**
       * Updates the current user, or the user with the given session ID, with the provided
       * parameters.
       *
       * @param sessionId The session id of the user to update. If null, the current user is
       *   updated.
       * @param params The parameters to update the user with. **See**: [UpdateParams].
       * @return A [ClerkResult] containing the updated [User] if the operation was successful, or a
       *   [ClerkErrorResponse] if it failed.
       */
      suspend fun update(
        sessionId: String? = null,
        params: UpdateParams,
      ): ClerkResult<User, ClerkErrorResponse> {
        return ClerkApi.user.updateUser(sessionId = sessionId, fields = params.toMap())
      }

      /** Deletes the current user, or the user with the given session ID, from the Clerk API. */
      suspend fun delete(
        sessionId: String? = null
      ): ClerkResult<DeletedObject, ClerkErrorResponse> = ClerkApi.user.deleteUser(sessionId)

      /**
       * Update the current user's profile image, or the user with the given session ID, with the
       * provided image data.
       *
       * @param sessionId The session id of the user to update. If null, the current user is
       *   updated.
       * @param data The image data to upload.
       * @return A [ClerkResult] containing the [ImageResource] if the operation was successful, or
       *   a [ClerkErrorResponse] if it failed.
       */
      suspend fun updateProfileImage(
        sessionId: String? = null,
        data: MultipartBody.Part,
      ): ClerkResult<ImageResource, ClerkErrorResponse> {
        return ClerkApi.user.updateProfileImage(sessionId, data)
      }

      /**
       * Deletes the current user's profile image, or the user with the given session ID, from the
       * Clerk API.
       *
       * @param sessionId The session id of the user to delete the profile image from. If null, the
       *   current user's profile image is deleted.
       * @return A [ClerkResult] containing the [DeletedObject] if the operation was successful, or
       *   a [ClerkErrorResponse] if it failed.
       */
      suspend fun deleteProfileImage(
        sessionId: String? = null
      ): ClerkResult<DeletedObject, ClerkErrorResponse> {
        return ClerkApi.user.deleteProfileImage(sessionId)
      }

      /**
       * Updates the current user's password, or the user with the given session ID, using the Clerk
       * API.
       *
       * @param sessionId The session id of the user to update the password for. If null, the
       *   current user's password is updated.
       * @param params The parameters for updating the password.
       * @return A [ClerkResult] containing the [User] if the operation was successful, or a
       *   [ClerkErrorResponse] if it failed.
       *
       * **See:** [UpdatePasswordParams] for the available parameters.
       */
      suspend fun updatePassword(
        sessionId: String? = null,
        params: UpdatePasswordParams,
      ): ClerkResult<User, ClerkErrorResponse> {
        return ClerkApi.user.updatePassword(sessionId, params.toMap())
      }

      /**
       * Deletes the current user's password, or the user with the given session ID, using the Clerk
       * API.
       *
       * @param sessionId The session id of the user to delete the password for. If null, the
       *   current user's password is deleted.
       * @param currentPassword The current password of the user. If null, the password is deleted
       *   without verification.
       * @return A [ClerkResult] containing the [User] if the operation was successful, or a
       *   [ClerkErrorResponse] if it failed.
       */
      suspend fun deletePassword(
        sessionId: String? = null,
        currentPassword: String,
      ): ClerkResult<User, ClerkErrorResponse> {
        return ClerkApi.user.deletePassword(sessionId, currentPassword)
      }

      suspend fun activeSessions(
        sessionId: String? = null
      ): ClerkResult<List<Session>, ClerkErrorResponse> {
        return ClerkApi.user.getActiveSessions(sessionId)
      }

      suspend fun allSessions(
        sessionId: String? = null
      ): ClerkResult<List<Session>, ClerkErrorResponse> {
        return ClerkApi.user.getSessions(sessionId)
      }

      suspend fun emailAddresses(
        sessionId: String? = null
      ): ClerkResult<List<EmailAddress>, ClerkErrorResponse> {
        return ClerkApi.user.getEmailAddresses(sessionId)
      }

      suspend fun createEmailAddress(
        sessionId: String? = null,
        email: String,
      ): ClerkResult<EmailAddress, ClerkErrorResponse> {
        return ClerkApi.user.createEmailAddress(sessionId, email)
      }

      suspend fun phoneNumbers(
        sessionId: String? = null
      ): ClerkResult<List<PhoneNumber>, ClerkErrorResponse> {
        return ClerkApi.user.getPhoneNumbers(sessionId)
      }

      suspend fun createPhoneNumber(
        sessionId: String? = null,
        phoneNumber: String,
      ): ClerkResult<PhoneNumber, ClerkErrorResponse> {
        return ClerkApi.user.createPhoneNumber(sessionId, phoneNumber)
      }

      suspend fun createPasskey(
        sessionId: String? = null
      ): ClerkResult<Passkey, ClerkErrorResponse> {
        return ClerkApi.user.createPasskey(sessionId = sessionId)
      }
    }
  }
}
