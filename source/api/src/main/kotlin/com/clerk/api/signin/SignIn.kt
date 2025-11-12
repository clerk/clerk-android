package com.clerk.api.signin

import com.clerk.api.Clerk
import com.clerk.api.Clerk.signIn
import com.clerk.api.Constants.Strategy.BACKUP_CODE
import com.clerk.api.Constants.Strategy.EMAIL_CODE
import com.clerk.api.Constants.Strategy.ENTERPRISE_SSO
import com.clerk.api.Constants.Strategy.PASSKEY
import com.clerk.api.Constants.Strategy.PASSWORD
import com.clerk.api.Constants.Strategy.PHONE_CODE
import com.clerk.api.Constants.Strategy.RESET_PASSWORD_EMAIL_CODE
import com.clerk.api.Constants.Strategy.RESET_PASSWORD_PHONE_CODE
import com.clerk.api.Constants.Strategy.TICKET
import com.clerk.api.Constants.Strategy.TOTP as STRATEGY_TOTP
import com.clerk.api.Constants.Strategy.TRANSFER
import com.clerk.api.network.ClerkApi
import com.clerk.api.network.model.error.ClerkErrorResponse
import com.clerk.api.network.model.factor.Factor
import com.clerk.api.network.model.verification.Verification
import com.clerk.api.network.serialization.ClerkResult
import com.clerk.api.passkeys.GoogleCredentialAuthenticationService
import com.clerk.api.passkeys.PasskeyService
import com.clerk.api.sso.GoogleSignInService
import com.clerk.api.sso.OAuthProvider
import com.clerk.api.sso.OAuthResult
import com.clerk.api.sso.RedirectConfiguration
import com.clerk.api.sso.SSOService
import com.clerk.automap.annotations.AutoMap
import com.clerk.automap.annotations.MapProperty
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * The `SignIn` object holds the state of the current sign-in process and provides helper methods to
 * navigate and complete the sign-in lifecycle. This includes managing the first and second factor
 * verifications, as well as creating a new session.
 *
 * ## The sign-in process follows these steps:
 * 1. **Initiate the Sign-In Process**
 *
 *    Collect the user's authentication information and pass the appropriate parameters to the
 *    [SignIn.create] method to start the sign-in.
 * 2. **Prepare for First Factor Verification**
 *
 *    Users **must** complete a first factor verification. This can include:
 *     - Providing a password
 *     - Using an email link
 *     - Entering a one-time code (OTP)
 *     - Authenticating with a Web3 wallet address
 *     - Providing proof of identity through an external social account (SSO/OAuth).
 * 3. **Complete First Factor Verification**
 *
 *    Attempt to verify the user's first factor authentication details.
 * 4. **Prepare for Second Factor Verification (Optional)**
 *
 *    If multi-factor authentication (MFA) is enabled for your application, prepare the second
 *    factor verification for users who have set up 2FA for their account.
 * 5. **Complete Second Factor Verification**
 *
 *    Attempt to verify the user's second factor authentication details if MFA is required.
 *
 * @property id Unique identifier for this sign in.
 * @property status The status of the current sign-in.
 * @property supportedIdentifiers Array of all the authentication identifiers that are supported for
 *   this sign in.
 * @property identifier The authentication identifier value for the current sign-in.
 * @property supportedFirstFactors Array of the first factors that are supported in the current
 *   sign-in.
 * @property supportedSecondFactors Array of the second factors that are supported in the current
 *   sign-in.
 * @property firstFactorVerification The state of the verification process for the selected first
 *   factor.
 * @property secondFactorVerification The state of the verification process for the selected second
 *   factor.
 * @property userData An object containing information about the user of the current sign-in.
 * @property createdSessionId The identifier of the session that was created upon completion of the
 *   current sign-in.
 */
@Serializable
data class SignIn(
  /** Unique identifier for this sign in. */
  val id: String,

  /** The status of the current sign-in. */
  val status: Status = Status.UNKNOWN,

  /** Array of all the authentication identifiers that are supported for this sign in. */
  @SerialName("supported_identifiers") val supportedIdentifiers: List<String>? = null,

  /** The authentication identifier value for the current sign-in. */
  val identifier: String? = null,

  /**
   * Array of the first factors that are supported in the current sign-in.
   *
   * Each factor contains information about the verification strategy that can be used.
   */
  @SerialName("supported_first_factors") val supportedFirstFactors: List<Factor>? = null,

  /**
   * Array of the second factors that are supported in the current sign-in.
   *
   * Each factor contains information about the verification strategy that can be used. This
   * property is populated only when the first factor is verified.
   */
  @SerialName("supported_second_factors") val supportedSecondFactors: List<Factor>? = null,

  /**
   * The state of the verification process for the selected first factor.
   *
   * Initially, this property contains an empty verification object, since there is no first factor
   * selected. You need to call the `prepareFirstFactor` method in order to start the verification
   * process.
   */
  @SerialName("first_factor_verification") val firstFactorVerification: Verification? = null,

  /**
   * The state of the verification process for the selected second factor.
   *
   * Initially, this property contains an empty verification object, since there is no second factor
   * selected. For the `phone_code` strategy, you need to call the `prepareSecondFactor` method in
   * order to start the verification process. For the `totp` strategy, you can directly attempt.
   */
  @SerialName("second_factor_verification") val secondFactorVerification: Verification? = null,

  /**
   * An object containing information about the user of the current sign-in.
   *
   * This property is populated only once an identifier is given to the SignIn object.
   */
  @SerialName("user_data") val userData: UserData? = null,

  /**
   * The identifier of the session that was created upon completion of the current sign-in.
   *
   * The value of this property is null if the sign-in status is not `complete`.
   */
  @SerialName("created_session_id") val createdSessionId: String? = null,
) {

  /**
   * An object containing information about the user of the current sign-in. This property is
   * populated only once an identifier is given to the SignIn object.
   *
   * @property firstName The user's first name.
   * @property lastName The user's last name.
   * @property imageUrl Holds the default avatar or user's uploaded profile image.
   * @property hasImage A boolean indicating whether the user has uploaded an image or one was
   *   copied from OAuth. Returns `false` if Clerk is displaying an avatar for the user.
   */
  @Serializable
  data class UserData(
    /** The user's first name. */
    val firstName: String? = null,

    /** The user's last name. */
    val lastName: String? = null,

    /** Holds the default avatar or user's uploaded profile image. */
    val imageUrl: String? = null,

    /**
     * A boolean to check if the user has uploaded an image or one was copied from OAuth. Returns
     * false if Clerk is displaying an avatar for the user.
     */
    val hasImage: Boolean? = null,
  )

  /**
   * Represents the status of a sign-in process.
   *
   * Each status indicates the current state of the sign-in flow and what action is required next.
   */
  @Serializable
  enum class Status {
    /** The sign-in process is complete. */
    @SerialName("complete") COMPLETE,

    /** The sign-in process needs a first factor verification. */
    @SerialName("needs_first_factor") NEEDS_FIRST_FACTOR,

    /** The sign-in process needs a second factor verification. */
    @SerialName("needs_second_factor") NEEDS_SECOND_FACTOR,

    /** The sign-in process needs an identifier. */
    @SerialName("needs_identifier") NEEDS_IDENTIFIER,

    /** The user needs to create a new password. */
    @SerialName("needs_new_password") NEEDS_NEW_PASSWORD,

    /** The sign-in process is in an unknown state. */
    UNKNOWN,
  }

  /**
   * A sealed interface defining parameter objects for attempting first factor verification in the
   * sign-in process.
   *
   * Each implementation represents a different verification strategy that can be used to complete
   * the first factor authentication step.
   *
   * The [strategy] value depends on the object's identifier value. Each authentication identifier
   * supports different verification strategies.
   */
  sealed interface AttemptFirstFactorParams {

    /**
     * The [strategy] value depends on the object's identifier value. Each authentication identifier
     * supports different verification strategies.
     */
    val strategy: String

    /**
     * Parameters for email code verification strategy.
     *
     * @property code The verification code received via email.
     */
    @AutoMap
    @Serializable
    data class EmailCode(val code: String, override val strategy: String = EMAIL_CODE) :
      AttemptFirstFactorParams

    /**
     * Parameters for phone code verification strategy.
     *
     * @property code The verification code received via SMS.
     */
    @AutoMap
    @Serializable
    data class PhoneCode(val code: String, override val strategy: String = PHONE_CODE) :
      AttemptFirstFactorParams

    /**
     * Parameters for password verification strategy.
     *
     * @property password The user's password.
     */
    @AutoMap
    @Serializable
    data class Password(
      @SerialName("password") val password: String,
      override val strategy: String = PASSWORD,
    ) : AttemptFirstFactorParams

    /**
     * Parameters for passkey verification strategy.
     *
     * @property publicKeyCredential The passkey credential for authentication.
     * @see PasskeyService for generating the credential.
     */
    @AutoMap
    @Serializable
    data class Passkey(
      @SerialName("public_key_credential") val publicKeyCredential: String,
      override val strategy: String = PASSKEY,
    ) : AttemptFirstFactorParams

    /**
     * Parameters for reset password email code verification strategy.
     *
     * @property code The verification code received via email for password reset.
     */
    @AutoMap
    @Serializable
    data class ResetPasswordEmailCode(
      val code: String,
      override val strategy: String = RESET_PASSWORD_EMAIL_CODE,
    ) : AttemptFirstFactorParams

    /**
     * Parameters for reset password phone code verification strategy.
     *
     * @property code The verification code received via SMS for password reset.
     */
    @AutoMap
    @Serializable
    data class ResetPasswordPhoneCode(
      val code: String,
      override val strategy: String = RESET_PASSWORD_PHONE_CODE,
    ) : AttemptFirstFactorParams
  }

  /**
   * Parameters for second factor authentication strategies.
   *
   * Each implementation represents a different second factor verification method that can be used
   * to complete multi-factor authentication (MFA) during the sign-in process.
   */
  sealed interface AttemptSecondFactorParams {
    val strategy: String

    @AutoMap
    @Serializable
    data class PhoneCode(val code: String, override val strategy: String = PHONE_CODE) :
      AttemptSecondFactorParams

    @AutoMap
    @Serializable
    data class TOTP(val code: String, override val strategy: String = STRATEGY_TOTP) :
      AttemptSecondFactorParams

    @AutoMap
    @Serializable
    data class BackupCode(val code: String, override val strategy: String = BACKUP_CODE) :
      AttemptSecondFactorParams

    @AutoMap
    @Serializable
    data class EmailCode(val code: String, override val strategy: String = EMAIL_CODE) :
      AttemptSecondFactorParams
  }

  /**
   * Parameters for authenticating with a redirect to an external provider.
   *
   * This includes OAuth providers and Enterprise SSO configurations that require redirecting the
   * user to an external authentication provider.
   *
   * @property redirectUrl The URL to redirect to after authentication.
   * @property legalAccepted Whether the user has accepted the legal terms.
   * @property emailAddress The user's email address for pre-filling authentication forms.
   * @property identifier The user's identifier for authentication.
   */
  sealed interface AuthenticateWithRedirectParams {
    val redirectUrl: String
    val legalAccepted: Boolean?
    val emailAddress: String?
    val identifier: String?

    /**
     * Parameters for authenticating with an OAuth provider using a redirect flow.
     *
     * @param provider The OAuth provider to use for authentication. You can use a predefined
     *   [OAuthProvider] or a custom one.
     * @param redirectUrl The URL to redirect to after the user completes the authentication flow
     *   with the provider. Defaults to the first redirect URL configured in your Clerk Dashboard.
     * @param emailAddress The user's email address, which can be used to pre-fill the
     *   authentication form on the provider's site.
     * @param legalAccepted Indicates whether the user has accepted any legal terms, such as a
     *   privacy policy or terms of service.
     * @param identifier The user's identifier for authentication, which can be an email address or
     *   phone number.
     */
    @Serializable
    @AutoMap
    data class OAuth(
      @MapProperty("providerData?.strategy") val provider: OAuthProvider,
      @SerialName("redirect_url")
      override val redirectUrl: String = RedirectConfiguration.DEFAULT_REDIRECT_URL,
      @SerialName("email_address") override val emailAddress: String? = null,
      @SerialName("legal_accepted") override val legalAccepted: Boolean? = null,
      override val identifier: String? = null,
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
      val strategy: String = ENTERPRISE_SSO,
      @SerialName("redirect_url") override val redirectUrl: String,
      @SerialName("legal_accepted") override val legalAccepted: Boolean? = null,
      @SerialName("email_address") override val emailAddress: String? = null,
      override val identifier: String? = null,
    ) : AuthenticateWithRedirectParams
  }

  /**
   * A sealed interface defining parameter objects for preparing first factor verification.
   *
   * This interface is used to specify which verification strategy should be prepared before
   * attempting the first factor authentication.
   *
   * @property strategy The verification strategy to use for the first factor authentication.
   */
  sealed interface PrepareFirstFactorParams {
    /**
     * Enumeration of available first factor verification strategies.
     *
     * Each strategy represents a different method of verifying the user's identity during the first
     * factor authentication step.
     */
    val strategy: String

    @AutoMap
    @Serializable
    data class EmailCode(
      @SerialName("email_address_id")
      val emailAddressId: String =
        signIn?.supportedFirstFactors!!.find { it.strategy == EMAIL_CODE }?.emailAddressId!!,
      override val strategy: String = EMAIL_CODE,
    ) : PrepareFirstFactorParams

    @AutoMap
    @Serializable
    data class PhoneCode(
      @SerialName("phone_number_id")
      val phoneNumberId: String =
        signIn?.supportedFirstFactors!!.find { it.strategy == PHONE_CODE }!!.phoneNumberId!!,
      override val strategy: String = PHONE_CODE,
    ) : PrepareFirstFactorParams

    @AutoMap
    @Serializable
    data class ResetPasswordEmailCode(
      val emailAddressId: String =
        signIn?.supportedFirstFactors!!.find { it.strategy == EMAIL_CODE }?.emailAddressId!!,
      override val strategy: String = RESET_PASSWORD_EMAIL_CODE,
    ) : PrepareFirstFactorParams

    @AutoMap
    @Serializable
    data class ResetPasswordPhoneCode(
      val phoneNumberId: String =
        signIn?.supportedFirstFactors!!.find { it.strategy == PHONE_CODE }!!.phoneNumberId!!,
      override val strategy: String = RESET_PASSWORD_PHONE_CODE,
    ) : PrepareFirstFactorParams

    @AutoMap
    @Serializable
    data class OAuth(
      override val strategy: String,
      @SerialName("redirect_url") val redirectUrl: String,
    ) : PrepareFirstFactorParams

    @AutoMap
    @Serializable
    data class EnterpriseSSO(
      override val strategy: String = ENTERPRISE_SSO,
      @SerialName("redirect_url") val redirectUrl: String,
    ) : PrepareFirstFactorParams

    @AutoMap
    @Serializable
    data class Passkey(override val strategy: String = PASSKEY) : PrepareFirstFactorParams
  }

  /**
   * A parameter object for preparing the second factor verification.
   *
   * This class is used to specify the strategy and related details for preparing a second factor
   * authentication method. For example, if you want to use a one-time code sent to the user's
   * phone, you would set the strategy to "phone_code".
   *
   * @property strategy The strategy to use for second factor verification. Common values are
   *   `phone_code` and `email_code`.
   * @property phoneNumberId The ID of the phone number to use for `phone_code` verification. This
   *   is typically retrieved from the `supportedSecondFactors` list on the `SignIn` object.
   * @property emailAddressId The ID of the email address to use for `email_code` verification. This
   *   is typically retrieved from the `supportedSecondFactors` list on the `SignIn` object.
   * @see SignIn.prepareSecondFactor
   */
  @Serializable
  @AutoMap
  data class PrepareSecondFactorParams(
    /** The strategy used for second factor verification. */
    val strategy: String = PHONE_CODE,
    @SerialName("phone_number_id") val phoneNumberId: String? = null,
    @SerialName("email_address_id") val emailAddressId: String? = null,
  ) {
    companion object {
      const val PHONE_CODE = "phone_code"
      const val EMAIL_CODE = "email_code"
    }
  }

  sealed class PrepareSecondFactorStrategy {

    data class PhoneCode(val phoneNumberId: String? = null) : PrepareSecondFactorStrategy()

    data class EmailCode(val emailAddressId: String? = null) : PrepareSecondFactorStrategy()

    fun toParams(): PrepareSecondFactorParams =
      when (this) {
        is PhoneCode ->
          PrepareSecondFactorParams(
            strategy = PrepareSecondFactorParams.PHONE_CODE,
            phoneNumberId = phoneNumberId,
          )
        is EmailCode ->
          PrepareSecondFactorParams(
            strategy = PrepareSecondFactorParams.EMAIL_CODE,
            emailAddressId = emailAddressId,
          )
      }
  }

  /**
   * Parameters for resetting a user's password during the sign-in process.
   *
   * @property password The new password to set for the user.
   * @property signOutOfOtherSessions Whether to sign out of all other sessions when the password is
   *   reset.
   */
  @Serializable
  data class ResetPasswordParams(
    val password: String,
    @SerialName("sign_out_of_other_sessions") val signOutOfOtherSessions: Boolean = false,
  )

  /**
   * Container object for sign-in creation parameters and strategies.
   *
   * Note: if you want to sign in with OAuth or Enterprise SSO, use the
   * `SignIn.authenticateWithRedirect()` method instead. Relatedly, If you are using the
   * `SignIn.authenticateWithRedirect()` method, you do not need to call `SignIn.create()` first.
   * The `SignIn.authenticateWithRedirect()` method will handle the creation of the SignIn object
   * internally.
   */
  object CreateParams {

    /**
     * A sealed interface defining different strategies for creating a sign-in.
     *
     * Each implementation represents a different method of initiating the sign-in process.
     */
    sealed interface Strategy {
      /** The authentication strategy identifier. */
      val strategy: String

      /**
       * Email code sign-in strategy.
       *
       * @property identifier The email address to send the verification code to.
       */
      @AutoMap
      @Serializable
      data class EmailCode(val identifier: String, override val strategy: String = EMAIL_CODE) :
        Strategy

      /**
       * Phone code sign-in strategy.
       *
       * @property identifier The phone number to send the verification code to.
       */
      @AutoMap
      @Serializable
      data class PhoneCode(val identifier: String, override val strategy: String = PHONE_CODE) :
        Strategy

      /**
       * Password sign-in strategy.
       *
       * @property identifier The email address or username for password authentication.
       */
      @AutoMap
      @Serializable
      data class Password(
        val identifier: String,
        val password: String,
        override val strategy: String = PASSWORD,
      ) : Strategy

      @AutoMap
      @Serializable
      data class ResetPasswordEmailCode(
        val identifier: String,
        override val strategy: String = RESET_PASSWORD_EMAIL_CODE,
      ) : Strategy

      @AutoMap
      @Serializable
      data class ResetPasswordPhoneCode(
        val identifier: String,
        override val strategy: String = RESET_PASSWORD_PHONE_CODE,
      ) : Strategy

      /**
       * Transfer strategy for account transfer scenarios.
       *
       * This strategy is used when transferring an existing session or account state.
       */
      data class Transfer(override val strategy: String = TRANSFER) : Strategy

      /**
       * Ticket strategy for authentication using a ticket.
       *
       * @param strategy The strategy identifier for ticket authentication.
       * @param identifier The identifier for the ticket authentication.
       * @param ticket The ticket used for authentication. **
       */
      @AutoMap
      @Serializable
      data class Ticket(val ticket: String, override val strategy: String = TICKET) : Strategy

      /** Passkey strategy for authentication using a passkey. */
      data class Passkey(override val strategy: String = PASSKEY) : Strategy
    }
  }

  /** Enumerates the types of credential requests supported by the service. */
  @Serializable
  enum class CredentialType {
    /** Request for a public key credential (passkey). */
    PASSKEY,

    /** Request for a password credential. */
    PASSWORD,

    /** Request for a Google ID token credential. */
    GOOGLE,

    /** Unknown credential type - used as fallback for unrecognized types. */
    UNKNOWN,
  }

  companion object {
    /**
     * Starts the sign in process. The SignIn object holds the state of the current sign-in and
     * provides helper methods to navigate and complete the sign-in process. It is used to manage
     * the sign-in lifecycle, including the first and second factor verification, and the creation
     * of a new session.
     *
     * ## The sign-in process follows these steps:
     * 1. Initiate the sign-in process by collecting the user's authentication information and
     *    passing the appropriate parameters to the [create] method.
     * 2. Prepare the first factor verification by calling [SignIn.prepareFirstFactor]. Users
     *    **must** complete a first factor verification. This can be something like providing a
     *    password, an email link, a one-time code (OTP), a Web3 wallet address, or providing proof
     *    of their identity through an external social account (SSO/OAuth).
     * 3. Attempt to complete the first factor verification by calling [SignIn.attemptFirstFactor].
     * 4. Optionally, if you have enabled multi-factor for your application, you will need to
     *    prepare the second factor verification by calling [SignIn.prepareSecondFactor].
     * 5. Attempt to complete the second factor verification by calling
     *    [SignIn.attemptSecondFactor].
     * 6. If verification is successful, set the newly created session as the active session by
     *    passing the [SignIn.createdSessionId] to the `setActive()` method on the `Clerk` object.
     *
     * **Note:** If you want to sign in with OAuth or Enterprise SSO, use the
     * [SignIn.authenticateWithRedirect] method instead. If you are using the
     * [SignIn.authenticateWithRedirect] method, you do not need to call [SignIn.create] first. The
     * [SignIn.authenticateWithRedirect] method will handle the creation of the SignIn object
     * internally.
     *
     * @param params The strategy to authenticate with.
     * @return A [ClerkResult] containing the created [SignIn] object on success, or a
     *   [ClerkErrorResponse] on failure.
     * @see [SignIn.CreateParams]
     *
     * ### Example usage:
     * ```kotlin
     * SignIn.create(SignIn.CreateParams.Strategy.EmailCode("user@example.com"))
     *   .onSuccess { signIn ->
     *     // Do something with the signIn object
     *   }
     *   .onFailure { error ->
     *     // Handle the error
     *   }
     * ```
     */
    suspend fun create(params: CreateParams.Strategy): ClerkResult<SignIn, ClerkErrorResponse> {
      return when (params) {
        is CreateParams.Strategy.Passkey -> PasskeyService.signInWithPasskey()
        else -> {
          val baseMap =
            if (params is CreateParams.Strategy.Transfer) {
              mapOf(TRANSFER to "true")
            } else {
              params.toMap()
            }
          val paramMap = baseMap + ("locale" to Clerk.locale.value.orEmpty())
          ClerkApi.signIn.createSignIn(paramMap)
        }
      }
    }

    /**
     * Creates a new SignIn object with the provided parameters. This is the equivalent of calling
     * `SignIn.create()` with JSON.
     *
     * @param params The raw parameters to create the SignIn object with.
     * @return A [ClerkResult] containing the created [SignIn] object on success, or a
     *   [ClerkErrorResponse] on failure.
     *
     * ### Example usage:
     * ```kotlin
     * SignIn.create(mapOf("identifier" to "user@example.com"))
     *   .onSuccess { signIn ->
     *     // Do something with the signIn object
     *   }
     *   .onFailure { error ->
     *     // Handle the error
     *   }
     * ```
     */
    suspend fun create(params: Map<String, String>): ClerkResult<SignIn, ClerkErrorResponse> {
      return ClerkApi.signIn.createSignIn(params)
    }

    /**
     * Authenticates the user with a token generated from Google identity services.
     *
     * @return A [ClerkResult] containing the [OAuthResult] on success, or a [ClerkErrorResponse] on
     *   failure.
     */
    suspend fun authenticateWithGoogleOneTap(): ClerkResult<OAuthResult, ClerkErrorResponse> {
      return GoogleSignInService().signInWithGoogle()
    }

    /**
     * Initiates the sign-in process using an OAuth or Enterprise SSO redirect flow.
     *
     * This method is used for authentication strategies that require redirecting the user to an
     * external authentication provider (e.g., Google, Facebook, or an Enterprise SSO provider). The
     * user will be redirected to the specified [AuthenticateWithRedirectParams.redirectUrl] to
     * complete authentication.
     *
     * @param params The parameters for the redirect-based authentication.
     * @return A [ClerkResult] containing the result of the authentication flow. The [OAuthResult]
     *   could contain either a sign-in or sign-up result, depending on whether an account transfer
     *   took place (i.e. if the user didn't have an account and a sign up was created instead).
     * @see [AuthenticateWithRedirectParams]
     * @see [OAuthProvider](https://clerk.com/docs/references/javascript/types/sso)
     *
     * ### Example usage:
     * ```kotlin
     * SignIn.authenticateWithRedirect(
     *   AuthenticateWithRedirectParams.OAuth(provider = OAuthProvider.GOOGLE)
     * ).onSuccess { result ->
     *   // Handle the result
     * }.onFailure { error ->
     *   // Handle the error
     * }
     * ```
     */
    suspend fun authenticateWithRedirect(
      params: AuthenticateWithRedirectParams
    ): ClerkResult<OAuthResult, ClerkErrorResponse> {
      return SSOService.authenticateWithRedirect(
        strategy = params.toMap()["provider"]!!,
        redirectUrl = params.redirectUrl,
        identifier = params.identifier,
        emailAddress = params.emailAddress,
        legalAccepted = params.legalAccepted,
      )
    }

    /**
     * Authenticates using the Google Credential Manager. The allowed types are Passkeys, Passwords,
     * and Google One Tap. You can control which credentials are used by passing the correct
     * [SignIn.CredentialType]
     */
    suspend fun authenticateWithGoogleCredential(
      credentialTypes: List<CredentialType>
    ): ClerkResult<SignIn, ClerkErrorResponse> {
      return GoogleCredentialAuthenticationService.signInWithGoogleCredential(
        credentialTypes = credentialTypes
      )
    }
  }
}

/**
 * Begins the first factor verification process. This is a required step in order to complete a sign
 * in, as users should be verified at least by one factor of authentication.
 *
 * Common scenarios are one-time code (OTP) or social account (SSO) verification. This is determined
 * by the accepted strategy parameter values. Each authentication identifier supports different
 * strategies.
 *
 * Returns a SignIn object. Check the [SignIn.firstFactorVerification] attribute for the status of
 * the first factor verification process.
 *
 * @param params The parameters for preparing the first factor verification.
 * @return A [ClerkResult] containing the updated [SignIn] object with the prepared first factor
 *   verification on success, or a [ClerkErrorResponse] on failure.
 * @see [SignIn.PrepareFirstFactorParams]
 *
 * ### Example usage:
 * ```kotlin
 * signIn.prepareFirstFactor(strategy = PrepareFirstFactorParams.Strategy.EMAIL_CODE)
 *   .onSuccess { updatedSignIn ->
 *     // Handle the updated SignIn object
 *   }
 *   .onFailure { error ->
 *     // Handle the error
 *   }
 * ```
 */
suspend fun SignIn.prepareFirstFactor(
  params: SignIn.PrepareFirstFactorParams
): ClerkResult<SignIn, ClerkErrorResponse> {
  return ClerkApi.signIn.prepareSignInFirstFactor(this.id, params.toMap())
}

/**
 * Prepares the second factor verification for the sign-in process.
 *
 * This function is used to initiate the second factor verification process, which is required for
 * multi-factor authentication (MFA) during the sign-in process. It automatically selects a
 * supported strategy, prioritizing "phone_code" if available, otherwise "email_code".
 *
 * @return A [ClerkResult] containing the updated [SignIn] object with the prepared second factor
 *   verification on success, or a [ClerkErrorResponse] on failure.
 * @receiver The current [SignIn] object representing the sign-in session.
 */
suspend fun SignIn.prepareSecondFactor(): ClerkResult<SignIn, ClerkErrorResponse> {
  val strategy =
    when {
      supportedSecondFactors?.any { it.strategy == SignIn.PrepareSecondFactorParams.PHONE_CODE } ==
        true ->
        SignIn.PrepareSecondFactorStrategy.PhoneCode(
          phoneNumberId =
            supportedSecondFactors
              .find { it.strategy == SignIn.PrepareSecondFactorParams.PHONE_CODE }
              ?.phoneNumberId
        )
      supportedSecondFactors?.any { it.strategy == SignIn.PrepareSecondFactorParams.EMAIL_CODE } ==
        true ->
        SignIn.PrepareSecondFactorStrategy.EmailCode(
          emailAddressId =
            supportedSecondFactors
              .find { it.strategy == SignIn.PrepareSecondFactorParams.EMAIL_CODE }
              ?.emailAddressId
        )
      else -> error("No supported second factor found")
    }

  val params = strategy.toParams()
  return ClerkApi.signIn.prepareSecondFactor(id = id, params = params.toMap())
}

/**
 * Attempts to complete the first factor verification process. This is a required step in order to
 * complete a sign in, as users should be verified at least by one factor of authentication.
 *
 * Make sure that a SignIn object already exists before you call this method, either by first
 * calling [SignIn.create] or [SignIn.prepareFirstFactor]. The only strategy that does not require a
 * verification to have already been prepared before attempting to complete it is the password
 * strategy.
 *
 * Depending on the strategy that was selected when the verification was prepared, the method
 * parameters will be different.
 *
 * Returns a SignIn object. Check the [SignIn.firstFactorVerification] attribute for the status of
 * the first factor verification process.
 *
 * @param params The parameters for the first factor verification.
 * @return A [ClerkResult] containing the updated [SignIn] object with the first factor verification
 *   result on success, or a [ClerkErrorResponse] on failure.
 * @see [SignIn.AttemptFirstFactorParams]
 */
suspend fun SignIn.attemptFirstFactor(
  params: SignIn.AttemptFirstFactorParams
): ClerkResult<SignIn, ClerkErrorResponse> {
  return ClerkApi.signIn.attemptFirstFactor(id = this.id, params = params.toMap())
}

/**
 * Attempts to complete the second factor verification process. This is an optional step in order to
 * complete a sign in, as users should be verified at least by one factor of authentication.
 *
 * Make sure that a SignIn object already exists before you call this method, either by first
 * calling [SignIn.create] or [SignIn.prepareSecondFactor].
 *
 * Depending on the strategy that was selected when the verification was prepared, the method
 * parameters will be different.
 *
 * @param params The parameters for the second factor verification.
 * @return A [ClerkResult] containing the updated [SignIn] object with the second factor
 *   verification result on success, or a [ClerkErrorResponse] on failure.
 * @see [SignIn.AttemptSecondFactorParams]
 */
suspend fun SignIn.attemptSecondFactor(
  params: SignIn.AttemptSecondFactorParams
): ClerkResult<SignIn, ClerkErrorResponse> {
  return ClerkApi.signIn.attemptSecondFactor(id = this.id, params = params.toMap())
}

/**
 * Resets the password for the current sign in attempt.
 *
 * This function is used when a user needs to reset their password during the sign-in process,
 * typically after receiving a password reset verification code.
 *
 * @param params An instance of [SignIn.ResetPasswordParams] containing the new password and session
 *   options.
 * @return A [ClerkResult] containing the updated SignIn object after the password reset on success,
 *   or a [ClerkErrorResponse] on failure.
 * @see SignIn.ResetPasswordParams
 */
suspend fun SignIn.resetPassword(
  params: SignIn.ResetPasswordParams
): ClerkResult<SignIn, ClerkErrorResponse> {
  return ClerkApi.signIn.resetPassword(
    id = this.id,
    password = params.password,
    signOutOfOtherSessions = params.signOutOfOtherSessions,
  )
}

/**
 * Resets the password for the current sign in attempt.
 *
 * This function is used when a user needs to reset their password during the sign-in process,
 * typically after receiving a password reset verification code.
 *
 * @param password The new password to set for the user.
 * @param signOutOfOtherSessions Whether to sign out of other sessions after resetting the password.
 *   Defaults to `false`.
 * @return A [ClerkResult] containing the updated [SignIn] object after the password reset on
 *   success, or a [ClerkErrorResponse] on failure.
 * @see [SignIn.ResetPasswordParams]
 */
suspend fun SignIn.resetPassword(
  password: String,
  signOutOfOtherSessions: Boolean = false,
): ClerkResult<SignIn, ClerkErrorResponse> {
  return ClerkApi.signIn.resetPassword(id = this.id, password = password, signOutOfOtherSessions)
}

/**
 * Retrieves the current state of the SignIn object from the server.
 *
 * This function can be used to refresh the SignIn object and get the latest status and verification
 * information.
 *
 * @param rotatingTokenNonce Optional nonce for rotating token validation.
 * @return A [ClerkResult] containing the refreshed [SignIn] object on success, or a
 *   [ClerkErrorResponse] on failure.
 */
suspend fun SignIn.get(
  rotatingTokenNonce: String? = null
): ClerkResult<SignIn, ClerkErrorResponse> {
  return ClerkApi.signIn.fetchSignIn(id = this.id, rotatingTokenNonce = rotatingTokenNonce)
}
