@file:Suppress("unused")

package com.clerk.sdk.model.signup

import com.clerk.sdk.model.error.ClerkErrorResponse
import com.clerk.sdk.model.response.ClientPiggybackedResponse
import com.clerk.sdk.model.signup.SignUp.PrepareVerificationParams
import com.clerk.sdk.model.verification.Verification
import com.clerk.sdk.network.ClerkApi
import com.clerk.sdk.network.requests.Requests
import com.clerk.sdk.network.requests.Requests.SignUp.CreateParams
import com.clerk.sdk.network.requests.toMap
import com.clerk.sdk.network.serialization.ClerkApiResult
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

typealias UpdateParams = CreateParams

/**
 * The `SignUp` object holds the state of the current sign-up and provides helper methods to
 * navigate and complete the sign-up process. Once a sign-up is complete, a new user is created.
 *
 * ### The Sign-Up Process:
 * 1. **Initiate the Sign-Up**: Begin the sign-up process by collecting the user's authentication
 *    information and passing the appropriate parameters to the `create()` method.
 * 2. **Prepare the Verification**: The system will prepare the necessary verification steps to
 *    confirm the user's information.
 * 3. **Complete the Verification**: Attempt to complete the verification by following the required
 *    steps based on the collected authentication data.
 * 4. **Sign Up Complete**: If the verification is successful, the newly created session is set as
 *    the active session.
 */
@Serializable
data class SignUp(
  /** The unique identifier of the current sign-up. */
  val id: String,

  /** The status of the current sign-up. */
  val status: Status,

  /**
   * An array of all the required fields that need to be supplied and verified in order for this
   * sign-up to be marked as complete and converted into a user.
   */
  @SerialName("required_fields") val requiredFields: List<String>,

  /**
   * An array of all the fields that can be supplied to the sign-up, but their absence does not
   * prevent the sign-up from being marked as complete.
   */
  @SerialName("optional_fields") val optionalFields: List<String>,

  /**
   * An array of all the fields whose values are not supplied yet but they are mandatory in order
   * for a sign-up to be marked as complete.
   */
  @SerialName("missing_fields") val missingFields: List<String>,

  /**
   * An array of all the fields whose values have been supplied, but they need additional
   * verification in order for them to be accepted.
   *
   * Examples of such fields are `email_address` and `phone_number`.
   */
  @SerialName("unverified_fields") val unverifiedFields: List<String>,

  /** An object that contains information about all the verifications that are in-flight. */
  val verifications: Map<String, Verification?>,

  /**
   * The username supplied to the current sign-up. Only supported if username is enabled in the
   * instance settings.
   */
  val username: String? = null,

  /**
   * The email address supplied to the current sign-up. Only supported if email address is enabled
   * in the instance settings.
   */
  @SerialName("email_address") val emailAddress: String? = null,

  /**
   * The user's phone number in E.164 format. Only supported if phone number is enabled in the
   * instance settings.
   */
  @SerialName("phone_number") val phoneNumber: String? = null,

  /**
   * The value of this attribute is true if a password was supplied to the current sign-up. Only
   * supported if password is enabled in the instance settings.
   */
  @SerialName("password_enabled") val passwordEnabled: Boolean,

  /**
   * The first name supplied to the current sign-up. Only supported if name is enabled in the
   * instance settings.
   */
  @SerialName("first_name") val firstName: String? = null,

  /**
   * The last name supplied to the current sign-up. Only supported if name is enabled in the
   * instance settings.
   */
  @SerialName("last_name") val lastName: String? = null,

  /**
   * Metadata that can be read and set from the frontend. Once the sign-up is complete, the value of
   * this field will be automatically copied to the newly created user's unsafe metadata. One common
   * use case for this attribute is to use it to implement custom fields that can be collected
   * during sign-up and will automatically be attached to the created User object.
   */
  @SerialName("unsafe_metadata") val unsafeMetadata: JsonObject? = null,

  /**
   * The identifier of the newly-created session. This attribute is populated only when the sign-up
   * is complete.
   */
  @SerialName("created_session_id") val createdSessionId: String? = null,

  /**
   * The identifier of the newly-created user. This attribute is populated only when the sign-up is
   * complete.
   */
  @SerialName("created_user_id") val createdUserId: String? = null,

  /** The date when the sign-up was abandoned by the user. */
  @SerialName("abandoned_at") val abandonedAt: Long? = null,
) {
  /**
   * Represents the current status of the sign-up process.
   *
   * The Status enum defines the possible states of a sign-up flow. Each state indicates a specific
   * requirement or completion level in the sign-up process.
   */
  @Serializable
  enum class Status {
    /** The sign-up has been inactive for over 24 hours. */
    ABANDONED,

    /**
     * A requirement is unverified or missing from the Email, Phone, Username settings. For example,
     * in the Clerk Dashboard, the Password setting is required but a password wasn't provided in
     * the custom flow.
     */
    @SerialName("missing_requirements") MISSING_REQUIREMENTS,

    /**
     * All the required fields have been supplied and verified, so the sign-up is complete and a new
     * user and a session have been created.
     */
    COMPLETE,

    /** The status is unknown. */
    UNKNOWN,
  }

  /** Defines the parameters required to prepare a verification for the sign-up process. */
  enum class PrepareVerificationParams(val strategy: String) {
    /** Send a text message with a unique token to input */
    PHONE_CODE("phone_code"),

    /** Send an email with a unique token to input */
    EMAIL_CODE("email_code"),
  }

  companion object {

    /**
     * Initiates a new sign-up process and returns a `SignUp` object based on the provided strategy
     * and optional parameters.
     *
     * Creates a new sign-up instance using the specified strategy.
     *
     * This method initiates a new sign-up process by sending the appropriate parameters to Clerk's
     * API. It deactivates any existing sign-up process and stores the sign-up lifecycle state in
     * the `status` property of the new `SignUp` object. If required fields are provided, the
     * sign-up process can be completed in one step. If not, Clerk's flexible sign-up process allows
     * multi-step flows.
     *
     * What you must pass to params depends on which sign-up options you have enabled in your Clerk
     * application instance.
     *
     * @param [createParams] The strategy to use for creating the sign-up. @see [CreateParams] for
     *   details.
     * @param createParams The parameters for creating the sign-up. @see [CreateParams] for details.
     * @return A [SignUp] object containing the current status and details of the sign-up process.
     *   The [status] property reflects the current state of the sign-up.
     * @see [SignUp] kdoc for more info
     */
    suspend fun create(
      createParams: Requests.SignUp.CreateParams
    ): ClerkApiResult<ClientPiggybackedResponse<SignUp>, ClerkErrorResponse> {

      return ClerkApi.instance.createSignUp(createParams.toMap())
    }
  }
}

suspend fun SignUp.update(
  updateParams: UpdateParams
): ClerkApiResult<ClientPiggybackedResponse<SignUp>, ClerkErrorResponse> {
  return ClerkApi.instance.updateSignUp(id, updateParams.toMap())
}

/**
 * The [prepareVerification] method is used to initiate the verification process for a field that
 * requires it.
 *
 * There are two fields that need to be verified: [com.clerk.sdk.model.signup.SignUp.emailAddress]:
 * The email address can be verified via an email code. This is a one-time code that is sent to the
 * email already provided to the [SignUp] object. The [prepareVerification] sends this email.
 * [com.clerk.sdk.model.signup.SignUp.phoneNumber]: The phone number can be verified via a phone
 * code. This is a one-time code that is sent via an SMS to the phone already provided to the
 * [SignUp] object. The [prepareVerification] sends this SMS.
 *
 * @param prepareVerificationParams: The parameters for preparing the verification.Specifies the
 *   field which requires verification
 * @return A [ClerkApiResult] containing the result of the verification preparation. A successful
 *   response indicates that the verification process has been initiated, and the [SignUp] object is
 *   returned.
 */
suspend fun SignUp.prepareVerification(
  prepareVerificationParams: PrepareVerificationParams
): ClerkApiResult<ClientPiggybackedResponse<SignUp>, ClerkErrorResponse> {
  return ClerkApi.instance.prepareSignUpVerification(this.id, prepareVerificationParams.strategy)
}

/**
 * // Attempts to complete the in-flight verification process that corresponds to the given
 * strategy. In order to use this method, you should first initiate a verification process by
 * calling SignUp.prepareVerification.
 *
 * Depending on the strategy, the method parameters could differ.
 *
 * @param params: The parameters for the verification attempt. This includes the strategy and the
 *   code @return: The updated [SignUp] object reflecting the verification attempt's result.
 */
suspend fun SignUp.attemptVerification(
  params: Requests.SignUp.AttemptVerificationParams
): ClerkApiResult<ClientPiggybackedResponse<SignUp>, ClerkErrorResponse> {
  return ClerkApi.instance.attemptSignUpVerification(
    signUpId = this.id,
    strategy = params.params.strategy,
    code = params.params.code,
  )
}
