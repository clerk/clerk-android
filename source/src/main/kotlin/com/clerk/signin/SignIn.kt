@file:Suppress("unused")

package com.clerk.signin

import android.content.Context
import com.clerk.automap.annotations.AutoMap
import com.clerk.network.ClerkApi
import com.clerk.network.model.error.ClerkErrorResponse
import com.clerk.network.model.factor.Factor
import com.clerk.network.model.verification.Verification
import com.clerk.network.serialization.ClerkResult
import com.clerk.oauth.GoogleSignInService
import com.clerk.oauth.OAuthProvider
import com.clerk.oauth.OAuthResult
import com.clerk.oauth.OAuthService
import com.clerk.oauth.RedirectConfiguration
import com.clerk.passkeys.PasskeySignInService
import com.clerk.signin.SignIn.PrepareFirstFactorParams
import com.clerk.signin.internal.toFormData
import com.clerk.signin.internal.toMap
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

private const val PHONE_CODE = "phone_code"
private const val EMAIL_CODE = "email_code"
private const val STRATEGY_TOTP = "totp"
private const val BACKUP_CODE = "backup_code"
private const val PASSWORD = "password"
private const val PASSKEY = "passkey"
private const val RESET_PASSWORD_EMAIL_CODE = "reset_password_email_code"
private const val RESET_PASSWORD_PHONE_CODE = "reset_password_phone_code"
private const val TICKET = "ticket"
private const val GOOGLE_ONE_TAP = "google_one_tap"
private const val TRANSFER = "transfer"

/**
 * The `SignIn` object holds the state of the current sign-in process and provides helper methods to
 * navigate and complete the sign-in lifecycle. This includes managing the first and second factor
 * verifications, as well as creating a new session.
 *
 * ### The following steps outline the sign-in process:
 * 1. **Initiate the Sign-In Process**
 *
 *    Collect the user's authentication information and pass the appropriate parameters to the
 *    `SignIn.create()` method to start the sign-in.
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
 */
@Serializable
data class SignIn(
  /** Unique identifier for this sign in. */
  val id: String,

  /** The status of the current sign-in. */
  val status: Status,

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

  /** Represents the status of a sign-in process. */
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
    data class EmailCode(override val strategy: String = EMAIL_CODE, val code: String) :
      AttemptFirstFactorParams {
      constructor(code: String) : this(EMAIL_CODE, code)
    }

    /**
     * Parameters for phone code verification strategy.
     *
     * @property code The verification code received via SMS.
     */
    @AutoMap
    @Serializable
    data class PhoneCode(override val strategy: String = PHONE_CODE, val code: String) :
      AttemptFirstFactorParams {
      constructor(code: String) : this(PHONE_CODE, code)
    }

    /**
     * Parameters for password verification strategy.
     *
     * @property password The user's password.
     */
    @AutoMap
    @Serializable
    data class Password(
      override val strategy: String = PASSWORD,
      @SerialName("password") val password: String,
    ) : AttemptFirstFactorParams {
      constructor(password: String) : this(PASSWORD, password)
    }

    /**
     * Parameters for passkey verification strategy.
     *
     * @property publicKeyCredential The passkey credential for authentication.
     * @see PasskeySignInService for generating the credential.
     */
    @AutoMap
    @Serializable
    data class Passkey(
      override val strategy: String = PASSKEY,
      @SerialName("public_key_credential") val publicKeyCredential: String,
    ) : AttemptFirstFactorParams {
      constructor(passkey: String) : this(PASSKEY, passkey)
    }

    /**
     * Parameters for reset password email code verification strategy.
     *
     * @property code The verification code received via email for password reset.
     */
    @AutoMap
    @Serializable
    data class ResetPasswordEmailCode(
      override val strategy: String = RESET_PASSWORD_EMAIL_CODE,
      val code: String,
    ) : AttemptFirstFactorParams {
      constructor(code: String) : this(RESET_PASSWORD_EMAIL_CODE, code)
    }

    /**
     * Parameters for reset password phone code verification strategy.
     *
     * @property code The verification code received via SMS for password reset.
     */
    @AutoMap
    @Serializable
    data class ResetPasswordPhoneCode(
      override val strategy: String = RESET_PASSWORD_PHONE_CODE,
      val code: String,
    ) : AttemptFirstFactorParams {
      constructor(code: String) : this(RESET_PASSWORD_PHONE_CODE, code)
    }
  }

  /** Parameters for second factor authentication strategies. */
  sealed interface AttemptSecondFactorParams {
    val strategy: String

    @AutoMap
    @Serializable
    data class PhoneCode(override val strategy: String = PHONE_CODE, val code: String) :
      AttemptSecondFactorParams {
      constructor(code: String) : this(PHONE_CODE, code)
    }

    @AutoMap
    @Serializable
    data class TOTP(override val strategy: String = STRATEGY_TOTP, val code: String) :
      AttemptSecondFactorParams {
      constructor(code: String) : this(STRATEGY_TOTP, code)
    }

    @AutoMap
    @Serializable
    data class BackupCode(override val strategy: String = BACKUP_CODE, val code: String) :
      AttemptSecondFactorParams {
      constructor(code: String) : this(BACKUP_CODE, code)
    }
  }

  /**
   * A sealed interface defining parameter objects for redirect-based authentication strategies.
   *
   * This includes OAuth providers and Enterprise SSO configurations that require redirecting the
   * user to an external authentication provider.
   */
  sealed interface AuthenticateWithRedirectParams {

    /** The OAuth or SSO provider to authenticate with. */
    val provider: OAuthProvider

    /** The URL to redirect to after the authentication flow completes. */
    val redirectUrl: String

    /**
     * Parameters for OAuth authentication with redirect.
     *
     * @property provider The OAuth provider (e.g., Google, Facebook, GitHub).
     * @property redirectUrl The URL to redirect to after OAuth completion.
     */
    data class OAuth(
      override val provider: OAuthProvider,
      @SerialName("redirect_url")
      override val redirectUrl: String = RedirectConfiguration.DEFAULT_REDIRECT_URL,
    ) : AuthenticateWithRedirectParams {
      constructor(
        provider: OAuthProvider
      ) : this(provider, RedirectConfiguration.DEFAULT_REDIRECT_URL)
    }

    /**
     * Parameters for Enterprise SSO authentication with redirect.
     *
     * @property provider The Enterprise SSO provider.
     * @property redirectUrl The URL to redirect to after SSO completion.
     */
    data class EnterpriseSSO(
      override val provider: OAuthProvider,
      @SerialName("redirect_url")
      override val redirectUrl: String = RedirectConfiguration.DEFAULT_REDIRECT_URL,
    ) : AuthenticateWithRedirectParams {
      constructor(
        provider: OAuthProvider
      ) : this(provider, RedirectConfiguration.DEFAULT_REDIRECT_URL)
    }
  }

  /**
   * A sealed interface defining parameter objects for preparing first factor verification.
   *
   * This interface is used to specify which verification strategy should be prepared before
   * attempting the first factor authentication.
   */
  sealed interface PrepareFirstFactorParams {

    /** Enumeration of available first factor verification strategies. */
    @Serializable
    enum class Strategy {
      /** Email code verification strategy. */
      EMAIL_CODE,

      /** Phone code (SMS) verification strategy. */
      PHONE_CODE,

      /** Password verification strategy. */
      PASSWORD,

      /** Passkey verification strategy. */
      PASSKEY,

      /** OAuth verification strategy. */
      O_AUTH,

      /** Reset password email code verification strategy. */
      RESET_PASSWORD_EMAIL_CODE,

      /** Reset password phone code verification strategy. */
      RESET_PASSWORD_PHONE_CODE,
    }
  }

  /**
   * A parameter object for preparing the second factor verification.
   *
   * @property strategy The strategy used for second factor verification (e.g., "phone_code",
   *   "totp").
   */
  @AutoMap
  @Serializable
  data class PrepareSecondFactorParams(
    /** The strategy used for second factor verification. */
    val strategy: String = PHONE_CODE,
    @SerialName("phone_number_id") val phoneNumberId: String? = null,
  ) {
    constructor(phoneNumberId: String) : this(strategy = PHONE_CODE, phoneNumberId = phoneNumberId)
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

  /** Container object for sign-in creation parameters and strategies. */
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
      data class EmailCode(override val strategy: String = EMAIL_CODE, val identifier: String) :
        Strategy {
        constructor(identifier: String) : this(strategy = EMAIL_CODE, identifier = identifier)
      }

      /**
       * Phone code sign-in strategy.
       *
       * @property identifier The phone number to send the verification code to.
       */
      @AutoMap
      @Serializable
      data class PhoneCode(override val strategy: String = PHONE_CODE, val identifier: String) :
        Strategy {
        constructor(identifier: String) : this(strategy = PHONE_CODE, identifier = identifier)
      }

      /**
       * Password sign-in strategy.
       *
       * @property identifier The email address or username for password authentication.
       */
      @AutoMap
      @Serializable
      data class Password(override val strategy: String = PASSWORD, val identifier: String) :
        Strategy {
        constructor(identifier: String) : this(strategy = PASSWORD, identifier = identifier)
      }

      /**
       * Transfer strategy for account transfer scenarios.
       *
       * This strategy is used when transferring an existing session or account state.
       */
      data class Transfer(override val strategy: String = TRANSFER) : Strategy {
        constructor() : this(strategy = TRANSFER)
      }

      /**
       * Ticket strategy for authentication using a ticket.
       *
       * @param strategy The strategy identifier for ticket authentication.
       * @param identifier The identifier for the ticket authentication.
       * @param ticket The ticket used for authentication. **
       */
      @AutoMap
      @Serializable
      data class Ticket(override val strategy: String = TICKET, val ticket: String) : Strategy {
        constructor(ticket: String) : this(strategy = TICKET, ticket = ticket)
      }

      /** Passkey strategy for authentication using a passkey. */
      data class Passkey(override val strategy: String = PASSKEY, val context: Context) : Strategy {
        constructor(context: Context) : this(strategy = PASSKEY, context = context)
      }
    }
  }

  companion object {
    /**
     * Starts the sign in process. The SignIn object holds the state of the current sign-in and
     * provides helper methods to navigate and complete the sign-in process. It is used to manage
     * the sign-in lifecycle, including the first and second factor verification, and the creation
     * of a new session.
     *
     * The following steps outline the sign-in process:
     * 1. Initiate the sign-in process by collecting the user's authentication information and
     *    passing the appropriate parameters to the `create()` method.
     * 2. Prepare the first factor verification by calling `SignIn.prepareFirstFactor()`. Users
     *    *must* complete a first factor verification. This can be something like providing a
     *    password, an email link, a one-time code (OTP), a Web3 wallet address, or providing proof
     *    of their identity through an external social account (SSO/OAuth).
     * 3. Attempt to complete the first factor verification by calling [SignIn.attemptFirstFactor].
     * 4. Optionally, if you have enabled multi-factor for your application, you will need to
     *    prepare the second factor verification by calling `SignIn.prepareSecondFactor()`.
     * 5. Attempt to complete the second factor verification by calling
     *    [SignIn.attemptSecondFactor()]
     * 6. If verification is successful, set the newly created session as the active session by
     *    passing the `SignIn.createdSessionId` to the `setActive()` method on the `Clerk` object.
     *
     * NOTE: If you are using the `SignIn.authenticateWithRedirect()` method, you do not need to
     * call `SignIn.create()` first. The `SignIn.authenticateWithRedirect()` method will handle the
     * creation of the SignIn object internally.
     *
     * @param params The strategy to authenticate with.
     * @see [SignIn.CreateParams]
     *
     * Example usage:
     * ```kotlin
     * SignIn.create(SignInCreateParams.Strategy.EmailAddress("user@example.com"))
     *          .onSuccess { signIn -> // Do something with the signIn object }
     *          .onFailure { error -> // Handle the error }
     * ```
     */
    suspend fun create(params: CreateParams.Strategy): ClerkResult<SignIn, ClerkErrorResponse> {
      return when (params) {
        is CreateParams.Strategy.Transfer -> ClerkApi().createSignIn(mapOf(TRANSFER to "true"))
        is CreateParams.Strategy.Passkey -> PasskeySignInService().signInWithPasskey(params.context)
        else -> ClerkApi().createSignIn(params.toMap())
      }
    }

    /**
     * Creates a new SignIn object with the provided parameters. This is the equivalent of calling
     * `SignIn.create()` with JSON.
     *
     * @param params The raw parameters to create the SignIn object with.
     * @return A [ClerkResult] containing the created SignIn object.
     *
     * Example usage:
     *
     *  ```kotlin
     * SignIn.create(mapOf("identifier" to "user@example.com"))
     *          .onSuccess { signIn -> Do something with the signIn object }
     *          .onFailure { error -> Handle the error }
     *  ```
     */
    suspend fun create(params: Map<String, String>): ClerkResult<SignIn, ClerkErrorResponse> {
      return ClerkApi().createSignIn(params)
    }

    /**
     * Authenticates the user with a token generated from Google identity services.
     *
     * @param context The application context of the end users device.
     * @return A [ClerkResult] containing the result of the authentication flow. or
     *   [ClerkResult.Failure] if the authentication fails.
     */
    suspend fun authenticateWithGoogleOneTap(
      context: Context
    ): ClerkResult<OAuthResult, ClerkErrorResponse> {
      return GoogleSignInService().signInWithGoogle(context)
    }

    /**
     * Initiates the sign-in process using an OAuth or Enterprise SSO redirect flow.
     *
     * This method is used for authentication strategies that require redirecting the user to an
     * external authentication provider (e.g., Google, Facebook, or an Enterprise SSO provider). The
     * user will be redirected to the specified [AuthenticateWithRedirectParams.redirectUrl] to
     * complete authentication.
     *
     * @param context The context in which the authentication flow is initiated. Used to open the in
     *   app browser.
     * @param params The parameters for the redirect-based authentication.
     *   [AuthenticateWithRedirectParams.provider] an [OAuthProvider]
     *   [AuthenticateWithRedirectParams.redirectUrl] The URL to redirect the user to after
     *   initiating the authentication flow. Set by default to
     *   [RedirectConfiguration.DEFAULT_REDIRECT_URL]
     * @return A [ClerkResult] containing the result of the authentication flow. The [OAuthResult]
     *   could contain either a sign-in or sign-up result, depending on whether an account transfer
     *   took place (i.e. if the user didn't have an account and a sign up was created instead).
     *
     * **See Also:** [OAuthProviders](https://clerk.com/docs/references/javascript/types/sso) \n \n
     * Example usage:
     * ```kotlin
     * SignIn.authenticateWithRedirect(context, AuthenticateWithRedirectParams(provider = OAuthProvider.GOOGLE))
     *   .onSuccess { result ->  // Handle the result }
     *   .onFailure { error ->  // Handle the error }
     * ```
     */
    suspend fun authenticateWithRedirect(
      context: Context,
      params: AuthenticateWithRedirectParams,
    ): ClerkResult<OAuthResult, ClerkErrorResponse> {
      return OAuthService.authenticateWithRedirect(context, params)
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
 * Returns a SignIn object. Check the firstFactorVerification attribute for the status of the first
 * factor verification process.
 *
 * @param strategy The strategy to authenticate with.
 * @return A [ClerkResult] containing the updated SignIn object with the prepared first factor
 *   verification.
 * @see SignIn.PrepareFirstFactorParams
 *
 * Example usage:
 * ```kotlin
 * Clerk.signIn.prepareFirstFactor(strategy = PrepareFirstFactorParams.Strategy.EmailCode)
 *   .onSuccess { updatedSignIn ->  // Handle the updated SignIn object }
 *   .onFailure { error ->  // Handle the error }
 * ```
 */
suspend fun SignIn.prepareFirstFactor(
  strategy: PrepareFirstFactorParams.Strategy
): ClerkResult<SignIn, ClerkErrorResponse> {
  return ClerkApi().prepareSignInFirstFactor(this.id, strategy.toFormData().toMap())
}

/**
 * Prepares the second factor verification for the sign in process.
 *
 * This function is used to initiate the second factor verification process, which is required for
 * multi-factor authentication (MFA) during the sign-in process.
 *
 * @return A [ClerkResult] containing the updated SignIn object with the prepared second factor
 *   verification.
 */
suspend fun SignIn.prepareSecondFactor(): ClerkResult<SignIn, ClerkErrorResponse> {
  val params =
    SignIn.PrepareSecondFactorParams(
      phoneNumberId =
        this.supportedSecondFactors?.find { it.strategy == "phone_code" }?.phoneNumberId
    )
  return ClerkApi().prepareSecondFactor(id = this.id, params = params.toMap())
}

/**
 * Attempts to complete the first factor verification process. This is a required step in order to
 * complete a sign in, as users should be verified at least by one factor of authentication.
 *
 * Make sure that a SignIn object already exists before you call this method, either by first
 * calling SignIn.create() or SignIn.prepareFirstFactor(). The only strategy that does not require a
 * verification to have already been prepared before attempting to complete it is the password
 * strategy.
 *
 * Depending on the strategy that was selected when the verification was prepared, the method
 * parameters will be different.
 *
 * Returns a SignIn object. Check the firstFactorVerification attribute for the status of the first
 * factor verification process.
 *
 * @param params The parameters for the first factor verification.
 * @return A [ClerkResult] containing the updated SignIn object with the first factor verification
 *   result.
 * @see [SignIn.AttemptFirstFactorParams]
 */
suspend fun SignIn.attemptFirstFactor(
  params: SignIn.AttemptFirstFactorParams
): ClerkResult<SignIn, ClerkErrorResponse> {
  return ClerkApi().attemptFirstFactor(id = this.id, params = params.toMap())
}

/**
 * Attempts to complete the second factor verification process. This is an optional step in order to
 * complete a sign in, as users should be verified at least by one factor of authentication.
 *
 * Make sure that a SignIn object already exists before you call this method, either by first
 * calling SignIn.create() or SignIn.prepareSecondFactor().
 *
 * Depending on the strategy that was selected when the verification was prepared, the method
 * parameters will be different.
 *
 * Returns a SignIn object.
 *
 * @param params The parameters for the second factor verification.
 * @return A [ClerkResult] containing the updated SignIn object with the second factor verification
 *   result.
 * @see [SignIn.AttemptSecondFactorParams]
 */
suspend fun SignIn.attemptSecondFactor(
  params: SignIn.AttemptSecondFactorParams
): ClerkResult<SignIn, ClerkErrorResponse> {
  return ClerkApi().attemptSecondFactor(id = this.id, params = params.toMap())
}

/**
 * Resets the password for the current sign in attempt.
 *
 * This function is used when a user needs to reset their password during the sign-in process,
 * typically after receiving a password reset verification code.
 *
 * @param params An instance of [SignIn.ResetPasswordParams] containing the new password and session
 *   options.
 * @return A [ClerkResult] containing the updated SignIn object after the password reset.
 * @see SignIn.ResetPasswordParams
 */
suspend fun SignIn.resetPassword(
  params: SignIn.ResetPasswordParams
): ClerkResult<SignIn, ClerkErrorResponse> {
  return ClerkApi()
    .resetPassword(
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
 * @param password An instance of [SignIn.ResetPasswordParams] containing the new password and
 *   session options.
 * @param signOutOfOtherSessions Whether to sign out of other sessions after resetting the password.
 *   Defaults to false.
 * @return A [ClerkResult] containing the updated SignIn object after the password reset.
 * @see SignIn.ResetPasswordParams
 */
suspend fun SignIn.resetPassword(
  password: String,
  signOutOfOtherSessions: Boolean = false,
): ClerkResult<SignIn, ClerkErrorResponse> {
  return ClerkApi().resetPassword(id = this.id, password = password, signOutOfOtherSessions)
}

/**
 * Retrieves the current state of the SignIn object from the server.
 *
 * This function can be used to refresh the SignIn object and get the latest status and verification
 * information.
 *
 * @param rotatingTokenNonce Optional nonce for rotating token validation.
 * @return A [ClerkResult] containing the refreshed SignIn object.
 */
suspend fun SignIn.get(
  rotatingTokenNonce: String? = null
): ClerkResult<SignIn, ClerkErrorResponse> {
  return ClerkApi().fetchSignIn(id = this.id, rotatingTokenNonce = rotatingTokenNonce)
}
