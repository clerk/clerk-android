package com.clerk.sdk.model.signin

import com.clerk.sdk.model.error.ClerkErrorResponse
import com.clerk.sdk.model.factor.Factor
import com.clerk.sdk.model.response.ClientPiggybackedResponse
import com.clerk.sdk.model.verification.Verification
import com.clerk.sdk.network.ClerkApi
import com.clerk.sdk.network.requests.RequestParams
import com.clerk.sdk.network.requests.toMap
import com.clerk.sdk.network.serialization.ClerkApiResult
import com.google.i18n.phonenumbers.PhoneNumberUtil
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

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
     */
    suspend fun create(
      identifier: RequestParams.SignInRequest.Identifier
    ): ClerkApiResult<ClientPiggybackedResponse<SignIn>, ClerkErrorResponse> {
      val input =
        if (identifier == RequestParams.SignInRequest.Identifier.Phone) {
          PhoneNumberUtil.getInstance().parse(identifier.value, "US").nationalNumber.toString()
        } else {
          identifier.value
        }
      return ClerkApi.instance.signIn(input)
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
 */
suspend fun SignIn.prepareFirstFactor(
  strategy: RequestParams.SignInRequest.PrepareFirstFactor
): ClerkApiResult<ClientPiggybackedResponse<SignIn>, ClerkErrorResponse> {
  return ClerkApi.instance.prepareSignInFirstFactor(
    this.id,
    PrepareFirstFactorParams.fromStrategy(this, strategy).toMap(),
  )
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
 * @see [RequestParams.SignInRequest.AttemptFirstFactor]
 */
suspend fun SignIn.attemptFirstFactor(
  params: RequestParams.SignInRequest.AttemptFirstFactor
): ClerkApiResult<ClientPiggybackedResponse<SignIn>, ClerkErrorResponse> {
  return ClerkApi.instance.attemptFirstFactor(id = this.id, params = params.toMap())
}

/**
 * Resets the password for the current sign in attempt.
 *
 * @param password The users new password
 * @param signOutOfOtherSessions Whether to sign out of other sessions. Defaults to false.
 */
suspend fun SignIn.resetPassword(
  password: String,
  signOutOfOtherSessions: Boolean = false,
): ClerkApiResult<ClientPiggybackedResponse<SignIn>, ClerkErrorResponse> {
  return ClerkApi.instance.resetPassword(
    id = this.id,
    password = password,
    signOutOfOtherSessions = signOutOfOtherSessions,
  )
}
