package com.clerk.sdk.model.user

import com.clerk.sdk.model.account.EnterpriseAccount
import com.clerk.sdk.model.account.ExternalAccount
import com.clerk.sdk.model.emailaddress.EmailAddress
import com.clerk.sdk.model.organization.OrganizationMembership
import com.clerk.sdk.model.phonenumber.PhoneNumber
import com.clerk.sdk.model.verification.Verification
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

/**
 * The `User` object holds all of the information for a single user of your application and provides
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
 * Finally, a `User` object holds profile data like the user's name, profile picture, and a set of
 * metadata that can be used internally to store arbitrary information. The metadata are split into
 * `publicMetadata` and `privateMetadata`. Both types are set from the Backend API, but public
 * metadata can also be accessed from the Frontend API.
 *
 * The Clerk SDK provides some helper methods on the User object to help retrieve and update user
 * information and authentication status.
 */
@Serializable
data class User(
  /** A boolean indicating whether the user has enabled Backup codes. */
  val backupCodeEnabled: Boolean,

  /** Date when the user was first created. */
  val createdAt: Long,

  /** A boolean indicating whether the organization creation is enabled for the user or not. */
  val createOrganizationEnabled: Boolean,

  /**
   * An integer indicating the number of organizations that can be created by the user. If the value
   * is 0, then the user can create unlimited organizations. Default is null.
   */
  val createOrganizationsLimit: Int? = null,

  /** A boolean indicating whether the user is able to delete their own account or not. */
  val deleteSelfEnabled: Boolean,

  /** An array of all the EmailAddress objects associated with the user. Includes the primary. */
  val emailAddresses: List<EmailAddress>,

  /** A list of enterprise accounts associated with the user. */
  val enterpriseAccounts: List<EnterpriseAccount>? = null,

  /**
   * An array of all the ExternalAccount objects associated with the user via OAuth. Note: This
   * includes both verified & unverified external accounts.
   */
  val externalAccounts: List<ExternalAccount>,

  /** The user's first name. */
  val firstName: String? = null,

  /**
   * A boolean to check if the user has uploaded an image or one was copied from OAuth. Returns
   * false if Clerk is displaying an avatar for the user.
   */
  val hasImage: Boolean,

  /** The unique identifier for the user. */
  val id: String,

  /** Holds the default avatar or user's uploaded profile image */
  val imageUrl: String,

  /** Date when the user last signed in. May be empty if the user has never signed in. */
  val lastSignInAt: Long? = null,

  /** The user's last name. */
  val lastName: String? = null,

  /** The date on which the user accepted the legal requirements if required. */
  val legalAcceptedAt: Long? = null,

  /**
   * A list of OrganizationMemberships representing the list of organizations the user is member
   * with.
   */
  val organizationMemberships: List<OrganizationMembership>,

  /** An array of all the Passkey objects associated with the user. */
  val passkeys: List<com.clerk.sdk.model.passkey.Passkey>,

  /** A boolean indicating whether the user has a password on their account. */
  val passwordEnabled: Boolean,

  /** An array of all the PhoneNumber objects associated with the user. Includes the primary. */
  val phoneNumbers: List<PhoneNumber>,

  /** The unique identifier for the EmailAddress that the user has set as primary. */
  val primaryEmailAddressId: String? = null,

  /** The unique identifier for the PhoneNumber that the user has set as primary. */
  val primaryPhoneNumberId: String? = null,

  /**
   * Metadata that can be read from the Frontend API and Backend API and can be set only from the
   * Backend API.
   */
  val publicMetadata: JsonObject? = null,

  /**
   * A boolean indicating whether the user has enabled TOTP by generating a TOTP secret and
   * verifying it via an authenticator app.
   */
  val totpEnabled: Boolean,

  /** A boolean indicating whether the user has enabled two-factor authentication. */
  val twoFactorEnabled: Boolean,

  /** Date of the last time the user was updated. */
  val updatedAt: Long,

  /**
   * Metadata that can be read and set from the Frontend API. One common use case for this attribute
   * is to implement custom fields that will be attached to the User object. Please note that there
   * is also an unsafeMetadata attribute in the SignUp object. The value of that field will be
   * automatically copied to the user's unsafe metadata once the sign up is complete.
   */
  val unsafeMetadata: JsonObject? = null,

  /** The user's username. */
  val username: String? = null,
) {
  /** A boolean to check if the user has verified an email address. */
  val hasVerifiedEmailAddress: Boolean
    get() = emailAddresses.any { it.verification?.status == Verification.Status.VERIFIED }

  /** A boolean to check if the user has verified a phone number. */
  val hasVerifiedPhoneNumber: Boolean
    get() = phoneNumbers.any { it.verification?.status == Verification.Status.VERIFIED }

  /** Information about the user's primary email address. */
  val primaryEmailAddress: EmailAddress?
    get() = emailAddresses.firstOrNull { it.id == primaryEmailAddressId }

  /** Information about the user's primary phone number. */
  val primaryPhoneNumber: PhoneNumber?
    get() = phoneNumbers.firstOrNull { it.id == primaryPhoneNumberId }

  /** A getter for the user's list of unverified external accounts. */
  val unverifiedExternalAccounts: List<ExternalAccount>
    get() = externalAccounts.filter { it.verification?.status == Verification.Status.UNVERIFIED }

  /** A getter for the user's list of verified external accounts. */
  val verifiedExternalAccounts: List<ExternalAccount>
    get() = externalAccounts.filter { it.verification?.status == Verification.Status.VERIFIED }
}
