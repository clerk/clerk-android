package com.clerk.sdk.network.requests

import com.clerk.mapgenerator.annotation.AutoMap
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

private const val PHONE_CODE = "phone_code"
private const val EMAIL_CODE = "email_code"
private const val PASSWORD = "password"
private const val PASSKEY = "passkey"
private const val RESET_PASSWORD_EMAIL_CODE = "reset_password_email_code"
private const val RESET_PASSWORD_PHONE_CODE = "reset_password_phone_code"

/** This file contains the data classes for inputs into Clerk API. functions */
object Requests {

  /** Request objects for the sign-in API. */
  object SignInRequest {

    /**
     * Parameter object for preparing the first factor verification
     *
     * Note: you must convert this object to a map before sending it to the API, since it must be
     * form url encoded
     */
    @Serializable
    @AutoMap
    data class PrepareFirstFactorParams(
      /**
       * The strategy value depends on the object's identifier value. Each authentication identifier
       * supports different verification strategies.
       */
      val strategy: String?,

      /**
       * Unique identifier for the user's email address that will receive an email message with the
       * one-time authentication code. This parameter will work only when the `email_code` strategy
       * is specified.
       */
      val emailAddressId: String?,

      /**
       * Unique identifier for the user's phone number that will receive an SMS message with the
       * one-time authentication code. This parameter will work only when the `phone_code` strategy
       * is specified.
       */
      val phoneNumberId: String?,

      /**
       * The URL that the OAuth provider should redirect to, on successful authorization on their
       * part. This parameter is required only if you set the strategy param to an OAuth strategy
       * like `oauth_<provider>`.
       */
      val redirectUrl: String?,
    )

    /** A parameter object for preparing the second factor verification. */
    @Serializable
    data class PrepareSecondFactorParams(
      /** The strategy used for second factor verification. */
      val strategy: String
    )

    /**
     * A parameter object for resetting a user's password.
     *
     * @param password The user's current password
     * @param signOutOfOtherSessions Whether to sign out of all other sessions after the password
     */
    @Serializable
    @AutoMap
    data class ResetPasswordParams(
      val password: String,
      @SerialName("sign_out_of_other_sessions") val signOutOfOtherSessions: Boolean? = null,
    )

    /** Represents an authentication identifier. */
    sealed interface Identifier {
      val value: String

      /** Email address identifier. */
      @Serializable data class Email(override val value: String) : Identifier

      /** Phone number identifier. */
      @Serializable data class Phone(override val value: String) : Identifier

      /** Username identifier. */
      @Serializable data class Username(override val value: String) : Identifier
    }

    /** A parameter object for attempting the first factor verification in the sign-in process. */
    sealed interface AttemptFirstFactorParams {

      /**
       * The [strategy] value depends on the object's identifier value. Each authentication
       * identifier supports different verification strategies.
       */
      val strategy: String

      /** The strategy strategy depends on the object's identifier strategy. Each authentication */
      @AutoMap
      @Serializable
      data class EmailCode(
        override val strategy: String = EMAIL_CODE,
        @SerialName("email_code") val emailCode: String,
      ) : AttemptFirstFactorParams {
        constructor(emailCode: String) : this(EMAIL_CODE, emailCode)
      }

      @AutoMap
      @Serializable
      data class PhoneCode(
        override val strategy: String = PHONE_CODE,
        @SerialName("phone_code") val phoneCode: String,
      ) : AttemptFirstFactorParams {
        constructor(phoneCode: String) : this(PHONE_CODE, phoneCode)
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
        val identifier: String,
      ) : AttemptFirstFactorParams {
        constructor(identifier: String) : this(RESET_PASSWORD_EMAIL_CODE, identifier)
      }

      @AutoMap
      @Serializable
      data class ResetPasswordPhoneCode(
        override val strategy: String = RESET_PASSWORD_PHONE_CODE,
        val password: String,
      ) : AttemptFirstFactorParams {
        constructor(password: String) : this(RESET_PASSWORD_PHONE_CODE, password)
      }
    }
  }

  object SignUpRequest {

    /**
     * Represents the various strategies for initiating a `SignUp` request. This sealed class acts
     * as a factory for the different combinations of parameters you can send to the create method
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
      ) : CreateParams

      /**
       * The `SignUp` will be created without any parameters.
       *
       * This is useful for inspecting a newly created `SignUp` object before deciding on a
       * strategy.
       */
      object None : CreateParams
    }

    /** Defines the strategies for attempting verification during the sign-up process. */
    sealed interface AttemptVerificationParams {
      /**
       * Attempts verification using a code sent to the user's email address.
       *
       * @param code The one-time code sent to the user's email address.
       */
      data class EmailCode(val code: String) : AttemptVerificationParams

      /**
       * Attempts verification using a code sent to the user's phone number.
       *
       * @param code The one-time code sent to the user's phone number.
       */
      data class PhoneCode(val code: String) : AttemptVerificationParams

      /** Converts the selected strategy into [AttemptVerificationParams] for the API request. */
      val params: AttemptParams
        get() =
          when (this) {
            is EmailCode -> AttemptParams(strategy = "email_code", code = code)
            is PhoneCode -> AttemptParams(strategy = "phone_code", code = code)
          }
    }

    /**
     * Parameters used for the verification attempt during the sign-up process.
     *
     * @property strategy The strategy used for verification (e.g., `email_code` or `phone_code`).
     * @property code The verification code provided by the user.
     */
    @Serializable data class AttemptParams(val strategy: String, val code: String)
  }
}
