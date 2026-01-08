@file:Suppress("unused")

package com.clerk.api.signup

import com.clerk.api.Clerk
import com.clerk.api.Constants.Strategy as AuthStrategy
import com.clerk.api.auth.builders.SendCodeBuilder
import com.clerk.api.auth.builders.SignUpBuilder
import com.clerk.api.auth.types.VerificationType
import com.clerk.api.extensions.sortedByPriority
import com.clerk.api.network.ClerkApi
import com.clerk.api.network.model.error.ClerkErrorResponse
import com.clerk.api.network.model.verification.Verification
import com.clerk.api.network.serialization.ClerkResult
import com.clerk.api.sso.OAuthProvider
import com.clerk.api.sso.OAuthResult
import com.clerk.api.sso.RedirectConfiguration
import com.clerk.api.sso.SSOService
import com.clerk.automap.annotations.AutoMap
import com.clerk.automap.annotations.MapProperty
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

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
  /**
   * The unique identifier of the current sign-up. This ID is used to track the sign-up process
   * throughout its lifecycle.
   */
  val id: String,

  /**
   * The status of the current sign-up. Indicates the current state of the sign-up process (e.g.,
   * complete, missing requirements, etc.).
   */
  val status: Status = Status.UNKNOWN,

  /**
   * An array of all the required fields that need to be supplied and verified in order for this
   * sign-up to be marked as complete and converted into a user.
   *
   * These fields must be provided and verified before the sign-up can be completed successfully.
   */
  @SerialName("required_fields") val requiredFields: List<String>,

  /**
   * An array of all the fields that can be supplied to the sign-up, but their absence does not
   * prevent the sign-up from being marked as complete.
   *
   * These fields are optional and can be provided to enhance the user profile but are not
   * mandatory.
   */
  @SerialName("optional_fields") val optionalFields: List<String>,

  /**
   * An array of all the fields whose values are not supplied yet but they are mandatory in order
   * for a sign-up to be marked as complete.
   *
   * These fields need to be provided before the sign-up can proceed to completion.
   */
  @SerialName("missing_fields") val missingFields: List<String>,

  /**
   * An array of all the fields whose values have been supplied, but they need additional
   * verification in order for them to be accepted.
   *
   * Examples of such fields are `email_address` and `phone_number`. These fields require
   * verification through codes sent via email or SMS.
   */
  @SerialName("unverified_fields") val unverifiedFields: List<String>,

  /**
   * An object that contains information about all the verifications that are in-flight.
   *
   * Each key represents a verification type (e.g., "email_address", "phone_number") and the value
   * contains the verification details and status.
   */
  val verifications: Map<String, Verification?>,

  /**
   * The username supplied to the current sign-up. Only supported if username is enabled in the
   * instance settings.
   *
   * @return The username if provided, null otherwise.
   */
  val username: String? = null,

  /**
   * The email address supplied to the current sign-up. Only supported if email address is enabled
   * in the instance settings.
   *
   * @return The email address if provided, null otherwise.
   */
  @SerialName("email_address") val emailAddress: String? = null,

  /**
   * The user's phone number in E.164 format. Only supported if phone number is enabled in the
   * instance settings.
   *
   * @return The phone number in E.164 format if provided, null otherwise.
   */
  @SerialName("phone_number") val phoneNumber: String? = null,

  /**
   * The value of this attribute is true if a password was supplied to the current sign-up. Only
   * supported if password is enabled in the instance settings.
   *
   * This boolean indicates whether a password has been set for the sign-up, but does not reveal the
   * actual password value for security reasons.
   */
  @SerialName("password_enabled") val passwordEnabled: Boolean,

  /**
   * The first name supplied to the current sign-up. Only supported if name is enabled in the
   * instance settings.
   *
   * @return The user's first name if provided, null otherwise.
   */
  @SerialName("first_name") val firstName: String? = null,

  /**
   * The last name supplied to the current sign-up. Only supported if name is enabled in the
   * instance settings.
   *
   * @return The user's last name if provided, null otherwise.
   */
  @SerialName("last_name") val lastName: String? = null,

  /**
   * Metadata that can be read and set from the frontend. Once the sign-up is complete, the value of
   * this field will be automatically copied to the newly created user's unsafe metadata. One common
   * use case for this attribute is to use it to implement custom fields that can be collected
   * during sign-up and will automatically be attached to the created User object.
   *
   * This metadata is not validated by Clerk and should not contain sensitive information.
   */
  @SerialName("unsafe_metadata") val unsafeMetadata: JsonObject? = null,

  /**
   * The identifier of the newly-created session. This attribute is populated only when the sign-up
   * is complete.
   *
   * @return The session ID if the sign-up is complete, null otherwise.
   */
  @SerialName("created_session_id") val createdSessionId: String? = null,

  /**
   * The identifier of the newly-created user. This attribute is populated only when the sign-up is
   * complete.
   *
   * @return The user ID if the sign-up is complete, null otherwise.
   */
  @SerialName("created_user_id") val createdUserId: String? = null,

  /**
   * The date when the sign-up was abandoned by the user.
   *
   * This timestamp is set when a sign-up process has been inactive for an extended period and is
   * considered abandoned.
   *
   * @return The abandonment timestamp in milliseconds since epoch, null if not abandoned.
   */
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
    /**
     * The sign-up has been inactive for over 24 hours. Once abandoned, the sign-up process cannot
     * be resumed and a new sign-up must be initiated.
     */
    @SerialName("abandoned") ABANDONED,

    /**
     * A requirement is unverified or missing from the Email, Phone, Username settings. For example,
     * in the Clerk Dashboard, the Password setting is required but a password wasn't provided in
     * the custom flow.
     *
     * Additional steps are needed to complete the sign-up process.
     */
    @SerialName("missing_requirements") MISSING_REQUIREMENTS,

    /**
     * All the required fields have been supplied and verified, so the sign-up is complete and a new
     * user and a session have been created.
     *
     * The sign-up process has been successfully completed.
     */
    @SerialName("complete") COMPLETE,

    /**
     * The status is unknown or not recognized. This typically indicates an unexpected state that
     * should be handled gracefully.
     */
    @SerialName("unknown") UNKNOWN,
  }

  /**
   * Defines the possible strategies for attempting verification during the sign-up process. This
   * sealed interface encapsulates the different types of verification attempts, such as email or
   * phone code verification.
   *
   * Use these parameters when calling [attemptVerification] to complete a verification that was
   * previously initiated with [prepareVerification].
   */
  sealed interface AttemptVerificationParams {
    /**
     * The strategy used for verification (e.g., `email_code` or `phone_code`). This must match the
     * strategy used when preparing the verification.
     */
    val strategy: String

    /**
     * The verification code provided by the user. This code should match the one sent via email or
     * SMS during the preparation phase.
     */
    val code: String

    /**
     * Attempts verification using a code sent to the user's email address.
     *
     * @param code The one-time code sent to the user's email address.
     */
    data class EmailCode(
      override val code: String,
      override val strategy: String = AuthStrategy.EMAIL_CODE,
    ) : AttemptVerificationParams

    /**
     * Attempts verification using a code sent to the user's phone number.
     *
     * @param code The one-time code sent to the user's phone number via SMS.
     */
    data class PhoneCode(
      override val code: String,
      override val strategy: String = AuthStrategy.PHONE_CODE,
    ) : AttemptVerificationParams
  }

  /**
   * Defines the parameters for authenticating with redirect-based flows. This sealed interface
   * supports OAuth and Enterprise SSO authentication methods.
   */
  sealed interface AuthenticateWithRedirectParams {

    /** The URL to redirect to after authentication completion. */
    val redirectUrl: String

    /** An optional identifier for the user (e.g., email, username). */
    val identifier: String?

    /** The user's email address for pre-filling authentication forms. */
    val emailAddress: String?

    /** Whether the user has accepted the legal terms and conditions. */
    val legalAccepted: Boolean?

    /**
     * OAuth authentication parameters for redirect-based sign-up.
     *
     * @param provider The OAuth provider to use for authentication.
     * @param redirectUrl The URL to redirect to after OAuth completion.
     * @param identifier Optional user identifier.
     * @param emailAddress Optional email address for pre-filling forms.
     * @param legalAccepted Whether legal terms have been accepted.
     */
    @AutoMap
    @Serializable
    data class OAuth(
      @MapProperty("providerData?.strategy") @SerialName("strategy") val provider: OAuthProvider,
      @SerialName("redirect_url") override val redirectUrl: String,
      override val identifier: String? = null,
      @SerialName("email_address") override val emailAddress: String? = null,
      @SerialName("legal_accepted") override val legalAccepted: Boolean? = null,
    ) : AuthenticateWithRedirectParams

    /**
     * Enterprise SSO params for redirect authentication.
     *
     * @property strategy The Enterprise SSO strategy identifier.
     * @property redirectUrl The URL to redirect to after authentication.
     * @property legalAccepted Whether the user has accepted the legal terms.
     * @property emailAddress The user's email address for pre-filling authentication forms.
     * @property identifier The user's identifier for authentication.
     */
    @Serializable
    @AutoMap
    data class EnterpriseSSO(
      val strategy: String = AuthStrategy.ENTERPRISE_SSO,
      @SerialName("redirect_url")
      override val redirectUrl: String = RedirectConfiguration.DEFAULT_REDIRECT_URL,
      @SerialName("legal_accepted") override val legalAccepted: Boolean? = null,
      @SerialName("email_address") override val emailAddress: String? = null,
      override val identifier: String? = null,
    ) : AuthenticateWithRedirectParams
  }

  /**
   * Contains parameters for preparing verification during the sign-up process. Use these strategies
   * to initiate verification for email addresses or phone numbers.
   */
  object PrepareVerificationParams {

    /**
     * Defines the available strategies for preparing verification. Each strategy corresponds to a
     * different verification method.
     */
    sealed interface Strategy {
      val strategy: String

      /**
       * Send a text message with a unique token to input. The verification code will be sent via
       * SMS to the provided phone number.
       */
      data class PhoneCode(override val strategy: String = AuthStrategy.PHONE_CODE) :
        PrepareVerificationParams.Strategy

      /**
       * Send an email with a unique token to input. The verification code will be sent to the
       * provided email address.
       */
      data class EmailCode(override val strategy: String = AuthStrategy.EMAIL_CODE) :
        PrepareVerificationParams.Strategy
    }
  }

  /**
   * Represents the various strategies for initiating a `SignUp` request. This sealed interface
   * encapsulates the different ways to create a sign-up, such as using standard parameters (e.g.,
   * email, password) or creating without any parameters to inspect the signUp object first.
   *
   * If looking for OAuth or Enterprise SSO use `authenticateWithRedirect`
   */
  sealed interface CreateParams {
    /**
     * Standard sign-up strategy, allowing the user to provide common details such as email,
     * password, and personal information.
     *
     * @param emailAddress The user's email address (optional).
     * @param password The user's password (optional).
     * @param firstName The user's first name (optional).
     * @param lastName The user's last name (optional).
     * @param username The user's username (optional).
     * @param phoneNumber The user's phone number in E.164 format (optional).
     * @param legalAccepted Whether the user has accepted the legal terms and conditions (optional).
     */
    @AutoMap
    @Serializable
    data class Standard(
      @SerialName("email_address") val emailAddress: String? = null,
      val password: String? = null,
      @SerialName("first_name") val firstName: String? = null,
      @SerialName("last_name") val lastName: String? = null,
      val username: String? = null,
      @SerialName("phone_number") val phoneNumber: String? = null,
      @SerialName("legal_accepted") val legalAccepted: Boolean? = null,
    ) : CreateParams

    /**
     * The `SignUp` will be created without any parameters.
     *
     * This is useful for inspecting a newly created `SignUp` object before deciding on a strategy.
     * You can examine the required and optional fields before providing user data.
     */
    object None : CreateParams

    /**
     * The `SignUp` will be created by transferring an existing session.
     *
     * This is used when a user is going through the Sign In flow and we detect they need to sign up
     * instead. This shouldn't be used for any other purpose.
     */
    object Transfer : CreateParams

    /** The `SignUp` will be created with a ticket. */
    @AutoMap @Serializable data class Ticket(val strategy: String = "ticket", val ticket: String)

    /**
     * The `SignUp` will be created using a Google One Tap token.
     *
     * Note: the one tap token should be obtained by calling [SignIn.authenticateWithOneTap()].
     *
     * @param strategy The authentication strategy (defaults to "google_one_tap").
     * @param token The Google One Tap token obtained from the authentication flow.
     */
    @AutoMap
    @Serializable
    data class GoogleOneTap(val strategy: String = "google_one_tap", val token: String) :
      CreateParams
  }

  sealed interface SignUpUpdateParams {
    /**
     * Standard sign-up update strategy, allowing the user to provide common details such as email,
     * password, and personal information. The update parameters are just a mirror of the create
     * parameters.
     *
     * @param emailAddress The user's email address (optional).
     * @param password The user's password (optional).
     * @param firstName The user's first name (optional).
     * @param lastName The user's last name (optional).
     * @param username The user's username (optional).
     * @param phoneNumber The user's phone number in E.164 format (optional).
     * @param legalAccepted Whether the user has accepted the legal terms and conditions (optional).
     */
    @AutoMap
    @Serializable
    data class Standard(
      @SerialName("email_address") val emailAddress: String? = null,
      val password: String? = null,
      @SerialName("first_name") val firstName: String? = null,
      @SerialName("last_name") val lastName: String? = null,
      val username: String? = null,
      @SerialName("phone_number") val phoneNumber: String? = null,
      @SerialName("legal_accepted") val legalAccepted: Boolean? = null,
    ) : SignUpUpdateParams
  }

  companion object {

    /**
     * Initiates a new sign-up process and returns a `SignUp` object.
     *
     * @param params The strategy to use for creating the sign-up.
     * @return A [ClerkResult] containing either a [SignUp] object or a [ClerkErrorResponse].
     */
    suspend fun create(params: CreateParams): ClerkResult<SignUp, ClerkErrorResponse> {
      val baseMap =
        if (params is CreateParams.Transfer) {
          mapOf("transfer" to "true")
        } else {
          params.toMap()
        }
      val paramMap = baseMap + ("locale" to Clerk.locale.value.orEmpty())
      return ClerkApi.signUp.createSignUp(paramMap)
    }

    /**
     * Creates a new sign-up with raw parameters.
     *
     * @param params The parameters for creating the sign-up as a map.
     * @return A [ClerkResult] containing either the created [SignUp] object or a
     *   [ClerkErrorResponse].
     */
    suspend fun create(params: Map<String, String>): ClerkResult<SignUp, ClerkErrorResponse> {
      return ClerkApi.signUp.createSignUp(params)
    }

    /**
     * Initiates authentication with a redirect-based flow (OAuth or Enterprise SSO).
     *
     * @param params The authentication parameters.
     * @return A [ClerkResult] containing either an [OAuthResult] or a [ClerkErrorResponse].
     */
    suspend fun authenticateWithRedirect(
      params: AuthenticateWithRedirectParams
    ): ClerkResult<OAuthResult, ClerkErrorResponse> {
      val strategy =
        when (params) {
          is AuthenticateWithRedirectParams.EnterpriseSSO -> params.strategy
          is AuthenticateWithRedirectParams.OAuth -> params.provider.providerData.strategy
        }
      return SSOService.authenticateWithRedirect(
        strategy = strategy,
        redirectUrl = params.redirectUrl,
        identifier = params.identifier,
        emailAddress = params.emailAddress,
        legalAccepted = params.legalAccepted,
      )
    }

    val fieldPriority: List<String> =
      listOf("email_address", "phone_number", "username", "password", "first_name", "last_name")
  }
}

/**
 * Helper property to get the first field that needs to be collected.
 *
 * This property returns the name of the first field in the `missingFields` list, sorted by
 * priority. The priority order is defined by [SignUp.fieldPriority].
 *
 * @return The name of the first field to collect, or `null` if there are no missing fields.
 */
val SignUp.firstFieldToCollect: String?
  get() = missingFields.sortedByPriority(SignUp.fieldPriority).firstOrNull()

/**
 * Helper property to get the first field that needs to be verified.
 *
 * This property returns the name of the first field in the `unverifiedFields` list, sorted by
 * priority. The priority order is defined by [SignUp.fieldPriority].
 *
 * @return The name of the first field to verify, or `null` if there are no unverified fields.
 */
val SignUp.firstFieldToVerify: String?
  get() = this.unverifiedFields.sortedByPriority(SignUp.fieldPriority).firstOrNull()

// region Internal Extension Functions (for SDK internal use)

/**
 * Updates the sign-up with additional information.
 *
 * @param updateParams The parameters for updating the sign-up.
 * @return A [ClerkResult] containing the updated [SignUp] object on success, or a
 *   [ClerkErrorResponse] on failure.
 */
suspend fun SignUp.update(
  updateParams: SignUp.SignUpUpdateParams
): ClerkResult<SignUp, ClerkErrorResponse> {
  return ClerkApi.signUp.updateSignUp(this.id, updateParams.toMap())
}

/**
 * Prepares verification for the specified strategy.
 *
 * @param prepareVerification The verification strategy to prepare.
 * @return A [ClerkResult] containing the updated [SignUp] object on success, or a
 *   [ClerkErrorResponse] on failure.
 */
suspend fun SignUp.prepareVerification(
  prepareVerification: SignUp.PrepareVerificationParams.Strategy
): ClerkResult<SignUp, ClerkErrorResponse> {
  return ClerkApi.signUp.prepareSignUpVerification(this.id, prepareVerification.strategy)
}

/**
 * Attempts to complete the verification process.
 *
 * @param params The verification attempt parameters.
 * @return A [ClerkResult] containing the updated [SignUp] object on success, or a
 *   [ClerkErrorResponse] on failure.
 */
suspend fun SignUp.attemptVerification(
  params: SignUp.AttemptVerificationParams
): ClerkResult<SignUp, ClerkErrorResponse> {
  return ClerkApi.signUp.attemptSignUpVerification(
    signUpId = this.id,
    strategy = params.strategy,
    code = params.code,
  )
}

// endregion

// region New Auth Namespace Extension Functions

/**
 * Sends a verification code to the specified email or phone.
 *
 * @param block Builder block to configure where to send the code.
 * @return A [ClerkResult] containing the updated [SignUp] object on success, or a
 *   [ClerkErrorResponse] on failure.
 *
 * ### Example usage:
 * ```kotlin
 * signUp.sendCode { email = "newuser@email.com" }
 * // or
 * signUp.sendCode { phone = "+1234567890" }
 * ```
 */
suspend fun SignUp.sendCode(
  block: SendCodeBuilder.() -> Unit
): ClerkResult<SignUp, ClerkErrorResponse> {
  val builder = SendCodeBuilder().apply(block)
  builder.validate()

  val strategy =
    if (builder.email != null) {
      SignUp.PrepareVerificationParams.Strategy.EmailCode()
    } else {
      SignUp.PrepareVerificationParams.Strategy.PhoneCode()
    }

  return ClerkApi.signUp.prepareSignUpVerification(this.id, strategy.strategy)
}

/**
 * Verifies with the provided code and type.
 *
 * Type is required since multiple verifications can be active during sign-up (e.g., both email and
 * phone verifications).
 *
 * @param code The verification code to verify.
 * @param type The type of verification (EMAIL or PHONE).
 * @return A [ClerkResult] containing the updated [SignUp] object on success, or a
 *   [ClerkErrorResponse] on failure.
 *
 * ### Example usage:
 * ```kotlin
 * signUp.verifyCode("123456", VerificationType.EMAIL)
 * signUp.verifyCode("654321", VerificationType.PHONE)
 * ```
 */
suspend fun SignUp.verifyCode(
  code: String,
  type: VerificationType,
): ClerkResult<SignUp, ClerkErrorResponse> {
  val strategy =
    when (type) {
      VerificationType.EMAIL -> AuthStrategy.EMAIL_CODE
      VerificationType.PHONE -> AuthStrategy.PHONE_CODE
    }

  return ClerkApi.signUp.attemptSignUpVerification(
    signUpId = this.id,
    strategy = strategy,
    code = code,
  )
}

/**
 * Updates the sign-up with additional information.
 *
 * @param block Builder block to configure the update.
 * @return A [ClerkResult] containing the updated [SignUp] object on success, or a
 *   [ClerkErrorResponse] on failure.
 *
 * ### Example usage:
 * ```kotlin
 * signUp.update {
 *     firstName = "John"
 *     lastName = "Doe"
 * }
 * ```
 */
suspend fun SignUp.update(
  block: SignUpBuilder.() -> Unit
): ClerkResult<SignUp, ClerkErrorResponse> {
  val builder = SignUpBuilder().apply(block)

  val params = buildMap {
    builder.email?.let { put("email_address", it) }
    builder.phone?.let { put("phone_number", it) }
    builder.password?.let { put("password", it) }
    builder.firstName?.let { put("first_name", it) }
    builder.lastName?.let { put("last_name", it) }
    builder.username?.let { put("username", it) }
    builder.legalAccepted?.let { put("legal_accepted", it.toString()) }
  }

  return ClerkApi.signUp.updateSignUp(this.id, params)
}

// endregion

/**
 * Converts the [SignUp] object to an [OAuthResult] object.
 *
 * This is an internal utility function used for OAuth flow integration. The resulting [OAuthResult]
 * wraps the sign-up data for OAuth processing.
 *
 * @return An [OAuthResult] containing this [SignUp] object.
 */
internal fun SignUp.toOAuthResult() = OAuthResult(signUp = this)
