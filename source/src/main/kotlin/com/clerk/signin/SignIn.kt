@file:Suppress("unused")

package com.clerk.signin

import android.content.Context
import com.clerk.automap.annotations.AutoMap
import com.clerk.model.error.ClerkErrorResponse
import com.clerk.model.factor.Factor
import com.clerk.model.verification.Verification
import com.clerk.network.ClerkApi
import com.clerk.network.serialization.ClerkApiResult
import com.clerk.signin.SignIn.PrepareFirstFactorParams
import com.clerk.signin.internal.toFormData
import com.clerk.signin.internal.toMap
import com.clerk.sso.RedirectConfiguration
import com.clerk.sso.SSOResult
import com.clerk.sso.SSOService
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

private const val PHONE_CODE = "phone_code"
private const val EMAIL_CODE = "email_code"
private const val PASSWORD = "password"
private const val PASSKEY = "passkey"
private const val RESET_PASSWORD_EMAIL_CODE = "reset_password_email_code"
private const val RESET_PASSWORD_PHONE_CODE = "reset_password_phone_code"

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

  /** A parameter object for attempting the first factor verification in the sign-in process. */
  sealed interface AttemptFirstFactorParams {

    /**
     * The [strategy] value depends on the object's identifier value. Each authentication identifier
     * supports different verification strategies.
     */
    val strategy: String

    @AutoMap
    @Serializable
    data class EmailCode(override val strategy: String = EMAIL_CODE, val code: String) :
      AttemptFirstFactorParams {
      constructor(code: String) : this(EMAIL_CODE, code)
    }

    @AutoMap
    @Serializable
    data class PhoneCode(override val strategy: String = PHONE_CODE, val code: String) :
      AttemptFirstFactorParams {
      constructor(code: String) : this(PHONE_CODE, code)
    }

    @AutoMap
    @Serializable
    data class Password(
      override val strategy: String = PASSWORD,
      @SerialName("password") val password: String,
    ) : AttemptFirstFactorParams {
      constructor(password: String) : this(PASSWORD, password)
    }

    @AutoMap
    @Serializable
    data class Passkey(override val strategy: String = PASSKEY, val passkey: String) :
      AttemptFirstFactorParams {
      constructor(passkey: String) : this(PASSKEY, passkey)
    }

    @AutoMap
    @Serializable
    data class ResetPasswordEmailCode(
      override val strategy: String = RESET_PASSWORD_EMAIL_CODE,
      val code: String,
    ) : AttemptFirstFactorParams {
      constructor(code: String) : this(RESET_PASSWORD_EMAIL_CODE, code)
    }

    @AutoMap
    @Serializable
    data class ResetPasswordPhoneCode(
      override val strategy: String = RESET_PASSWORD_PHONE_CODE,
      val code: String,
    ) : AttemptFirstFactorParams {
      constructor(code: String) : this(RESET_PASSWORD_PHONE_CODE, code)
    }
  }

  sealed interface AuthenticateWithRedirectParams {

    val strategy: String
    val redirectUrl: String

    data class OAuth(
      override val strategy: String,
      @SerialName("redirect_url")
      override val redirectUrl: String = RedirectConfiguration.REDIRECT_URL,
    ) : AuthenticateWithRedirectParams {
      constructor(strategy: String) : this(strategy, RedirectConfiguration.REDIRECT_URL)
    }

    data class EnterpriseSSO(
      override val strategy: String,
      @SerialName("redirect_url")
      override val redirectUrl: String = RedirectConfiguration.REDIRECT_URL,
    ) : AuthenticateWithRedirectParams {
      constructor(strategy: String) : this(strategy, RedirectConfiguration.REDIRECT_URL)
    }
  }

  sealed interface PrepareFirstFactorParams {

    @Serializable
    enum class Strategy {
      EMAIL_CODE,
      PHONE_CODE,
      PASSWORD,
      PASSKEY,
      O_AUTH,
      RESET_PASSWORD_EMAIL_CODE,
      RESET_PASSWORD_PHONE_CODE,
    }
  }

  /** A parameter object for preparing the second factor verification. */
  @Serializable
  data class PrepareSecondFactorParams(
    /** The strategy used for second factor verification. */
    val strategy: String
  )

  @Serializable
  data class ResetPasswordParams(
    val password: String,
    @SerialName("sign_out_of_other_sessions") val signOutOfOtherSessions: Boolean = false,
  )

  /** Represents an authentication identifier. */
  object SignInCreateParams {

    sealed interface Strategy {
      val strategy: String

      @AutoMap
      @Serializable
      data class EmailCode(override val strategy: String = EMAIL_CODE, val identifier: String) :
        Strategy {
        constructor(identifier: String) : this(strategy = EMAIL_CODE, identifier = identifier)
      }

      @AutoMap
      @Serializable
      data class PhoneCode(override val strategy: String = PHONE_CODE, val identifier: String) :
        Strategy {
        constructor(identifier: String) : this(strategy = PHONE_CODE, identifier = identifier)
      }

      @AutoMap
      @Serializable
      data class Password(override val strategy: String = PASSWORD, val identifier: String) :
        Strategy {
        constructor(identifier: String) : this(strategy = PASSWORD, identifier = identifier)
      }

      //      /**
      //       * OAuth identifier.
      //       *
      //       * @param [strategy] should be `oauth_google`, `oauth_facebook`, etc. When using Clerk
      // you can
      //       *   get this field from
      // [com.clerk.model.environment.UserSettings.SocialConfig.strategy],
      //       *   the available and configured social providers can be found via
      //       *   [com.clerk.Clerk.socialProviders]
      //       * @param [redirectUrl] The URL to redirect to after the OAuth flow completes.
      //       * @param context The context in which the authentication flow is initiated. Used to
      // open the
      //       *   in app browser.
      //       */
      //      data class OAuth(
      //        override val strategy: String,
      //        val redirectUrl: String,
      //        val context: Context,
      //      ) : Strategy

      data class Transfer(override val strategy: String = "transfer") : Strategy {
        constructor() : this(strategy = "transfer")
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
     * @see [SignIn.SignInCreateParams]
     */
    suspend fun create(
      params: SignInCreateParams.Strategy
    ): ClerkApiResult<SignIn, ClerkErrorResponse> {
      return when (params) {
        is SignInCreateParams.Strategy.Transfer ->
          ClerkApi.instance.createSignIn(mapOf("transfer" to "true"))
        else -> ClerkApi.instance.createSignIn(params.toMap())
      }
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
     *     - [AuthenticateWithRedirectParams.strategy]: The authentication strategy (e.g., OAuth
     *       provider or Enterprise SSO).
     *     - [AuthenticateWithRedirectParams.redirectUrl]: The URL to redirect the user to after
     *       initiating the authentication flow.
     *
     * Supported strategies include:
     * - OAuth providers (e.g., `oauth_google`, `oauth_facebook`)
     *
     * @return A [ClerkApiResult] containing the result of the authentication flow. The [SSOResult]
     *   could contain either a sign-in or sign-up result, depending on whether an account transfer
     *   took place (i.e. if the user didn't have an account and a sign up was created instead).
     *
     * **See Also:** [OAuthProviders](https://clerk.com/docs/references/javascript/types/sso)
     */
    suspend fun authenticateWithRedirect(
      context: Context,
      params: AuthenticateWithRedirectParams,
    ): ClerkApiResult<SSOResult, ClerkErrorResponse> {
      return SSOService.authenticateWithRedirect(context, params)
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
 * @see SignIn.PrepareFirstFactorParams
 */
suspend fun SignIn.prepareFirstFactor(
  strategy: PrepareFirstFactorParams.Strategy
): ClerkApiResult<SignIn, ClerkErrorResponse> {
  return ClerkApi.instance.prepareSignInFirstFactor(this.id, strategy.toFormData().toMap())
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
 * @see [SignIn.AttemptFirstFactorParams]
 */
suspend fun SignIn.attemptFirstFactor(
  params: SignIn.AttemptFirstFactorParams
): ClerkApiResult<SignIn, ClerkErrorResponse> {
  return ClerkApi.instance.attemptFirstFactor(id = this.id, params = params.toMap())
}

/**
 * Resets the password for the current sign in attempt.
 *
 * @param params an instance of [SignIn.ResetPasswordParams]
 * @see SignIn.ResetPasswordParams
 */
suspend fun SignIn.resetPassword(
  params: SignIn.ResetPasswordParams
): ClerkApiResult<SignIn, ClerkErrorResponse> {
  return ClerkApi.instance.resetPassword(
    id = this.id,
    password = params.password,
    signOutOfOtherSessions = params.signOutOfOtherSessions,
  )
}

suspend fun SignIn.get(
  rotatingTokenNonce: String? = null
): ClerkApiResult<SignIn, ClerkErrorResponse> {
  return ClerkApi.instance.fetchSignIn(id = this.id, rotatingTokenNonce = rotatingTokenNonce)
}

/** Converts the current [SignIn] instance to an [SSOResult]. */
fun SignIn.toSSOResult() = SSOResult(signIn = this)
