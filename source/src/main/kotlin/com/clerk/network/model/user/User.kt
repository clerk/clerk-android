package com.clerk.network.model.user

import com.clerk.model.account.EnterpriseAccount
import com.clerk.model.account.ExternalAccount
import com.clerk.model.emailaddress.EmailAddress
import com.clerk.model.organization.OrganizationMembership
import com.clerk.model.passkey.Passkey
import com.clerk.model.phonenumber.PhoneNumber
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
)
