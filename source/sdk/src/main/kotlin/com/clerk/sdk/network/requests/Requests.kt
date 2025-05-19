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
  object SignIn {

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
    @Serializable
    sealed interface Identifier {
      val value: String

      /** Email address identifier. */
      data class Email(override val value: String) : Identifier

      /** Phone number identifier. */
      data class Phone(override val value: String) : Identifier

      /** Username identifier. */
      data class Username(override val value: String) : Identifier
    }

    /** A parameter object for attempting the first factor verification in the sign-in process. */
    sealed interface AttemptFirstFactorParams {
      /** The identifier will be the user provided authentication value. */
      val identifier: String

      /**
       * The [value] will be the strategy value. The strategy value depends on the object's
       * identifier value. Each authentication identifier supports different verification
       * strategies.
       */
      val value: String

      /** The strategy value depends on the object's identifier value. Each authentication */
      @AutoMap
      @Serializable
      data class EmailCode(
        override val value: String = EMAIL_CODE,
        override val identifier: String,
      ) : AttemptFirstFactorParams

      @AutoMap
      @Serializable
      data class PhoneCode(
        override val value: String = PHONE_CODE,
        override val identifier: String,
      ) : AttemptFirstFactorParams

      @AutoMap
      @Serializable
      data class Password(override val value: String = PASSWORD, override val identifier: String) :
        AttemptFirstFactorParams

      @AutoMap
      @Serializable
      data class Passkey(override val value: String = PASSKEY, override val identifier: String) :
        AttemptFirstFactorParams

      @AutoMap
      @Serializable
      data class ResetPasswordEmailCode(
        override val value: String = RESET_PASSWORD_EMAIL_CODE,
        override val identifier: String,
      ) : AttemptFirstFactorParams

      @AutoMap
      @Serializable
      data class ResetPasswordPhoneCode(
        override val value: String = RESET_PASSWORD_PHONE_CODE,
        override val identifier: String,
      ) : AttemptFirstFactorParams
    }
  }
}
