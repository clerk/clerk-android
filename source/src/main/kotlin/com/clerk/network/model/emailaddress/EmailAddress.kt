package com.clerk.network.model.emailaddress

import com.clerk.automap.annotations.AutoMap
import com.clerk.network.ClerkApi
import com.clerk.network.model.error.ClerkErrorResponse
import com.clerk.network.model.verification.Verification
import com.clerk.network.serialization.ClerkResult
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

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

  /**
   * Parameters used to prepare the email address for verification using the specified strategy.
   *
   * @property strategy The strategy to use for verification. This can be one of the following:
   * - `email_code`: The email address will be verified by sending a verification email to the user.
   *   The user will then enter the code from the email to complete the verification process.
   * - `email_link`: The email address will be verified by sending a verification email to the user.
   * - `enterprise_sso`: The email address will be verified by using an enterprise single sign-on
   *   (SSO) provider. The user will then click a link in the email to complete the verification
   *   process. This is useful for verifying the email address in a single step.
   *
   * @property redirectUrl Used with the `email_link` strategy. The URL to redirect to after the
   *   verification email is sent.
   * @property actionCompleteRedirectUrl Used with `oauth_provider` and `saml` strategies. The URL
   *   to redirect to after the verification is complete.
   */
  @AutoMap
  @Serializable
  data class PrepareVerificationParams(
    val strategy: Strategy,
    val redirectUrl: String? = null,
    @SerialName("action_complete_redirect_url") val actionCompleteRedirectUrl: String? = null,
  ) {

    @Serializable
    enum class Strategy(val value: String) {
      /** The email address will be verified by sending a verification email to the user. */
      EMAIL_CODE("email_code"),
      EMAIL_LINK("email_link"),
      ENTERPRISE_SSO("enterprise_sso"),
    }
  }
}

/**
 * Attempts to verify the email address with the given code.
 *
 * @param code The verification code sent to the email address.
 * @param sessionId The session ID to use for the request. If null, the request will be made without
 *   a session ID.
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
 * @param sessionId The session ID to use for the request. If null, the request will be made without
 *   a session ID.
 * @param params The parameters to use for the verification.
 * @return A [ClerkResult] containing the updated [EmailAddress] if the verification was successful,
 *   or a [ClerkErrorResponse] if the verification failed.
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
 * @param sessionId The session ID to use for the request. If null, the request will be made without
 *   a session ID.
 * @return A [ClerkResult] containing the [EmailAddress] if the request was successful, or a
 *   [ClerkErrorResponse] if the request failed.
 */
suspend fun EmailAddress.get(): ClerkResult<EmailAddress, ClerkErrorResponse> {
  return ClerkApi.user.getEmailAddress(emailAddressId = this.id)
}

/**
 * Deletes the [EmailAddress] from the server.
 *
 * @param sessionId The session ID to use for the request. If null, the request will be made without
 *   a session ID.
 * @return A [ClerkResult] containing the deleted [EmailAddress] if the request was successful, or a
 *   [ClerkErrorResponse] if the request failed.
 */
suspend fun EmailAddress.delete(): ClerkResult<EmailAddress, ClerkErrorResponse> {
  return ClerkApi.user.deleteEmailAddress(emailAddressId = this.id)
}
