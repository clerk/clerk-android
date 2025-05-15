package com.clerk.sdk.model.signup

import com.clerk.mapgenerator.annotation.AutoMap
import com.clerk.sdk.model.response.ClerkResponse
import com.clerk.sdk.model.response.ClientPiggybackedResponse
import com.clerk.sdk.model.signup.SignUp.CreateStrategy.Standard
import com.clerk.sdk.model.verification.Verification
import com.clerk.sdk.network.ClerkApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
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

  @Serializable
  data class CreateParams(
    // The strategy to use for the sign-up flow.
    val strategy: String? = null,

    // The user's first name. Only supported if name is enabled.
    @SerialName("first_name") val firstName: String? = null,

    // The user's last name. Only supported if name is enabled.
    @SerialName("last_name") val lastName: String? = null,

    // The user's password. Only supported if password is enabled.
    val password: String? = null,

    // The user's email address. Only supported if email address is enabled. Keep in mind that the
    // email address requires an extra verification process.
    @SerialName("email_address") val emailAddress: String? = null,

    // The user's phone number in E.164 format. Only supported if phone number is enabled. Keep in
    // mind that the phone number requires an extra verification process.
    @SerialName("phone_number") val phoneNumber: String? = null,

    // The user's username. Only supported if usernames are enabled.
    val username: String? = null,

    // Metadata that can be read and set from the frontend.
    // Once the sign-up is complete, the value of this field will be automatically copied to the
    // newly created user's unsafe metadata.
    // One common use case for this attribute is to use it to implement custom fields that can be
    // collected during sign-up and will automatically be attached to the created User object.
    @SerialName("unsafe_metadata") val unsafeMetadata: JsonElement? = null,

    // If strategy is set to 'oauth_{provider}' or 'enterprise_sso', this specifies full URL or path
    // that the OAuth provider should redirect to after successful authorization on their part.
    // If strategy is set to 'email_link', this specifies The full URL that the user will be
    // redirected to when they visit the email link. See the custom flow for implementation details.

    @SerialName("redirect_url") val redirectUrl: String? = null,

    // Required if strategy is set to 'ticket'. The ticket or token generated from the Backend API.
    val ticket: String? = null,

    // When set to true, the SignUp will attempt to retrieve information from the active SignIn
    // instance and use it to complete the sign-up process.
    // This is useful when you want to seamlessly transition a user from a sign-in attempt to a
    // sign-up attempt.
    val transfer: Boolean? = null,

    // A boolean indicating whether the user has agreed to the legal compliance documents.
    @SerialName("legal_accepted") val legalAccepted: Boolean? = null,

    // Optional if strategy is set to 'oauth_{provider}' or 'enterprise_sso'. The value to pass to
    // the OIDC prompt parameter in the generated OAuth redirect URL.
    @SerialName("oidc_prompt") val oidcPrompt: String? = null,

    // Optional if strategy is set to 'oauth_<provider>' or 'enterprise_sso'. The value to pass to
    // the OIDC login_hint parameter in the generated OAuth redirect URL.
    @SerialName("oidc_login_hint") val oidcLoginHint: String? = null,

    // The ID token from a provider used for authentication (e.g., SignInWithApple).
    val token: String? = null,
  )

  /** Represents the various strategies for initiating a `SignUp` request. */
  sealed class CreateStrategy {

    /**
     * Standard sign-up strategy, allowing the user to provide common details such as email,
     * password, and personal information.
     *
     * @param emailAddress The user's email address (optional).
     * @param password The user's password (optional).
     * @param firstName The user's first name (optional).
     * @param lastName The user's last name (optional).
     * @param username The user's username (optional).
     * @param phoneNumber The user's phone number (optional).
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
    ) : CreateStrategy()

    /**
     * The `SignUp` will be created without any parameters.
     *
     * This is useful for inspecting a newly created `SignUp` object before deciding on a strategy.
     */
    object None : CreateStrategy()
  }

  companion object {
    suspend fun create(createStrategy: CreateStrategy): ClerkResponse<ClientPiggybackedResponse> {
      val formMap =
        if (createStrategy is Standard) {
          createStrategy.toMap()
        } else {
          emptyMap()
        }
      return ClerkApi.instance.createSignUp(formMap)
    }
  }
}
