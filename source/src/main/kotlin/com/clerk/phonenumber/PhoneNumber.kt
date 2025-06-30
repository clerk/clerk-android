package com.clerk.phonenumber

import com.clerk.Constants.Strategy.PHONE_CODE
import com.clerk.network.ClerkApi
import com.clerk.network.model.deleted.DeletedObject
import com.clerk.network.model.error.ClerkErrorResponse
import com.clerk.network.model.verification.Verification
import com.clerk.network.serialization.ClerkResult
import kotlinx.serialization.Serializable

/** The verification strategy constant used for phone number verification via SMS code. */

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
  val reservedForSecondFactor: Boolean = false,

  /** A boolean indicating whether this phone number is the default second factor. */
  val defaultSecondFactor: Boolean = false,

  /** The date when the phone number was created. */
  val createdAt: Long? = null,

  /** The date when the phone number was last updated. */
  val updatedAt: Long? = null,

  /**
   * An object containing information about any other identification that might be linked to this
   * phone number.
   */
  val linkedTo: List<String>? = null,

  /** A list of backup codes in case of lost phone number access. */
  val backupCodes: List<String>? = null,
) {

  companion object {
    /**
     * Creates a new phone number for the current user or the user with the given session ID.
     *
     * The newly created phone number will be unverified initially. The user will need to complete
     * the verification process (typically via SMS) before the phone number can be used for
     * authentication or two-factor authentication.
     *
     * @param phoneNumber The phone number to add to the user's account (should include country
     *   code)
     * @return A [ClerkResult] containing the created [PhoneNumber] object on success, or a
     *   [ClerkErrorResponse] on failure
     */
    suspend fun create(phoneNumber: String): ClerkResult<PhoneNumber, ClerkErrorResponse> {
      return ClerkApi.user.createPhoneNumber(phoneNumber)
    }
  }
}

/**
 * Attempts to verify this phone number using the provided verification code.
 *
 * This is the second and final step in the phone number verification process. The verification code
 * is typically sent via SMS after calling [prepareVerification].
 *
 * @param code The one-time verification code received via SMS
 * @return A [ClerkResult] containing the updated [PhoneNumber] on success, or a
 *   [ClerkErrorResponse] on failure
 */
suspend fun PhoneNumber.attemptVerification(
  code: String
): ClerkResult<PhoneNumber, ClerkErrorResponse> {
  return ClerkApi.user.attemptPhoneNumberVerification(phoneNumberId = this.id, code = code)
}

/**
 * Initiates the phone number verification process by sending a verification code via SMS.
 *
 * This is the first step in the phone number verification process. After calling this method, a
 * one-time verification code will be sent to the phone number via SMS. Use [attemptVerification] to
 * complete the verification process.
 *
 * @return A [ClerkResult] containing the updated [PhoneNumber] on success, or a
 *   [ClerkErrorResponse] on failure
 */
suspend fun PhoneNumber.prepareVerification(): ClerkResult<PhoneNumber, ClerkErrorResponse> {
  return ClerkApi.user.preparePhoneNumberVerification(
    phoneNumberId = this.id,
    strategy = PHONE_CODE,
  )
}

/**
 * Updates the properties of this phone number.
 *
 * Allows modification of second factor authentication settings for this phone number.
 *
 * @param reservedForSecondFactor Whether this phone number should be reserved for second factor
 *   authentication
 * @param defaultSecondFactor Whether this phone number should be the default second factor method
 * @return A [ClerkResult] containing the updated [PhoneNumber] on success, or a
 *   [ClerkErrorResponse] on failure
 */
suspend fun PhoneNumber.update(
  reservedForSecondFactor: Boolean? = null,
  defaultSecondFactor: Boolean? = null,
): ClerkResult<PhoneNumber, ClerkErrorResponse> {
  return ClerkApi.user.updatePhoneNumber(
    phoneNumberId = this.id,
    reservedForSecondFactor = reservedForSecondFactor,
    defaultSecondFactor = defaultSecondFactor,
  )
}

/**
 * Deletes this phone number from the user's account.
 *
 * This operation is irreversible. Once deleted, the phone number will no longer be associated with
 * the user's account and cannot be used for authentication or contact purposes.
 *
 * @return A [ClerkResult] containing a [DeletedObject] on success, or a [ClerkErrorResponse] on
 *   failure
 */
suspend fun PhoneNumber.delete(): ClerkResult<DeletedObject, ClerkErrorResponse> {
  return ClerkApi.user.deletePhoneNumber(phoneNumberId = this.id)
}
