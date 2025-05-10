package com.clerk.model.phonenumber

import com.clerk.model.verification.Verification
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

/**
 * The `PhoneNumber` object is a model around a phone number entity.
 *
 * Phone numbers can be used as a proof of identification for users, or simply as a means of
 * contacting users.
 *
 * Phone numbers must be verified to ensure that they can be assigned to their rightful owners. The
 * `PhoneNumber` object holds all the necessary state around the verification process.
 * - The verification process always starts with the `prepareVerification()` method, which will send
 *   a one-time verification code via an SMS message.
 * - The second and final step involves an attempt to complete the verification by calling the
 *   `attemptVerification(code:)` method, passing the one-time code as a parameter.
 *
 * Finally, phone numbers can be used as part of multi-factor authentication. During sign-in, users
 * can opt in to an extra verification step where they will receive an SMS message with a one-time
 * code. This code must be entered to complete the sign-in process.
 */
@Serializable
data class PhoneNumber(
  /** The unique identifier for this phone number. */
  val id: String,

  /** The phone number value. */
  val phoneNumber: String,

  /** An object holding information on the verification of this phone number. */
  val verification: Verification? = null,

  /**
   * A boolean indicating whether this phone number is reserved for second factor authentication.
   */
  val reservedForSecondFactor: Boolean,

  /** A boolean indicating whether this phone number is the default second factor. */
  val defaultSecondFactor: Boolean,

  /** The date when the phone number was created. */
  val createdAt: Instant,

  /** The date when the phone number was last updated. */
  val updatedAt: Instant,

  /**
   * An object containing information about any other identification that might be linked to this
   * phone number.
   */
  val linkedTo: JsonObject? = null,

  /** A list of backup codes in case of lost phone number access. */
  val backupCodes: List<String>? = null,
)
