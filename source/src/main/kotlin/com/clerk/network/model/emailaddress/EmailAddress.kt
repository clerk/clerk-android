package com.clerk.network.model.emailaddress

import com.clerk.automap.annotations.AutoMap
import com.clerk.network.ClerkApi
import com.clerk.network.model.deleted.DeletedObject
import com.clerk.network.model.error.ClerkErrorResponse
import com.clerk.network.model.verification.Verification
import com.clerk.network.serialization.ClerkResult
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

private const val EMAIL_CODE = "email_code"

private const val ENTERPRISE_SSO = "enterprise_sso"

/** The EmailAddress object represents an email address associated with a user. */
@Serializable
data class EmailAddress(
  /** The unique identifier for the email address. */
  val id: String,

  /** The email address value. */
  @SerialName("email_address") val emailAddress: String,

  /** The verification status of the email address. */
  val verification: Verification? = null,

  /** A list of linked accounts or identifiers associated with this email address. */
  @SerialName("linked_to") val linkedTo: List<LinkedEntity>? = null,
) {

  @Serializable data class LinkedEntity(val id: String, val type: String)

  sealed interface PrepareVerificationParams {
    val strategy: String

    @AutoMap
    @Serializable
    data class EmailCode(override val strategy: String = EMAIL_CODE) : PrepareVerificationParams

    @AutoMap
    @Serializable
    data class EnterpriseSSO(
      override val strategy: String = ENTERPRISE_SSO,
      val redirectUrl: String? = null,
      @SerialName("action_complete_redirect_url") val actionCompleteRedirectUrl: String? = null,
    ) : PrepareVerificationParams
  }
}

/**
 * Attempts to verify the email address with the given code.
 *
 * @param code The verification code sent to the email address.
 * @return A [ClerkResult] containing the updated [EmailAddress] if the verification was successful,
 *   or a [ClerkErrorResponse] if the verification failed.
 */
suspend fun EmailAddress.attemptVerification(
  code: String
): ClerkResult<EmailAddress, ClerkErrorResponse> {
  return ClerkApi.user.attemptEmailAddressVerification(emailAddressId = this.id, code = code)
}

/**
 * Prepares the email address for verification using the specified strategy.
 *
 * @param params The parameters to use for the verification.
 * @return A [ClerkResult] containing the updated [EmailAddress] if the verification was successful,
 *   or a [ClerkErrorResponse] if the verification failed.
 * @see EmailAddress.PrepareVerificationParams
 */
suspend fun EmailAddress.prepareVerification(
  params: EmailAddress.PrepareVerificationParams
): ClerkResult<EmailAddress, ClerkErrorResponse> {
  return ClerkApi.user.prepareEmailAddressVerification(
    emailAddressId = this.id,
    params = params.toMap(),
  )
}

/**
 * Retrieves the [EmailAddress] from the server.
 *
 * @return A [ClerkResult] containing the [EmailAddress] if the request was successful, or a
 *   [ClerkErrorResponse] if the request failed.
 */
suspend fun EmailAddress.get(): ClerkResult<EmailAddress, ClerkErrorResponse> {
  return ClerkApi.user.getEmailAddress(emailAddressId = this.id)
}

/**
 * Deletes the [EmailAddress] from the server.
 *
 * @return A [ClerkResult] containing the deleted [EmailAddress] if the request was successful, or a
 *   [ClerkErrorResponse] if the request failed.
 */
suspend fun EmailAddress.delete(): ClerkResult<DeletedObject, ClerkErrorResponse> {
  return ClerkApi.user.deleteEmailAddress(emailAddressId = this.id)
}
