package com.clerk.sdk.network.requests

import com.clerk.automap.annotation.AutoMap
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

private const val PHONE_CODE = "phone_code"
private const val EMAIL_CODE = "email_code"

/** This file contains the data classes for inputs into Clerk API. functions */
object RequestParams {

  /** Request objects for the sign-in API. */
  //  object SignInRequest {
  //
  //    @Serializable
  //    enum class PrepareFirstFactor {
  //      EMAIL_CODE,
  //      PHONE_CODE,
  //      PASSWORD,
  //      PASSKEY,
  //      O_AUTH,
  //      RESET_PASSWORD_EMAIL_CODE,
  //      RESET_PASSWORD_PHONE_CODE,
  //    }
  //
  //    /** A parameter object for preparing the second factor verification. */
  //    @Serializable
  //    data class PrepareSecondFactor(
  //      /** The strategy used for second factor verification. */
  //      val strategy: String
  //    )
  //
  //    /** Represents an authentication identifier. */
  //    sealed interface Identifier {
  //      val value: String
  //
  //      /** Email address identifier. */
  //      @Serializable data class Email(override val value: String) : Identifier
  //
  //      /** Phone number identifier. */
  //      @Serializable data class Phone(override val value: String) : Identifier
  //
  //      /** Username identifier. */
  //      @Serializable data class Username(override val value: String) : Identifier
  //    }
  //
  //    /** A parameter object for attempting the first factor verification in the sign-in process.
  // */
  //    sealed interface AttemptFirstFactor {
  //
  //      /**
  //       * The [strategy] value depends on the object's identifier value. Each authentication
  //       * identifier supports different verification strategies.
  //       */
  //      val strategy: String
  //
  //      /** The strategy strategy depends on the object's identifier strategy. Each authentication
  // */
  //      @AutoMap
  //      @Serializable
  //      data class EmailCode(override val strategy: String = EMAIL_CODE, val code: String) :
  //        AttemptFirstFactor {
  //        constructor(code: String) : this(EMAIL_CODE, code)
  //      }
  //
  //      @AutoMap
  //      @Serializable
  //      data class PhoneCode(override val strategy: String = PHONE_CODE, val code: String) :
  //        AttemptFirstFactor {
  //        constructor(code: String) : this(PHONE_CODE, code)
  //      }
  //
  //      @AutoMap
  //      @Serializable
  //      data class Password(
  //        override val strategy: String = PASSWORD,
  //        @SerialName("password") val password: String,
  //      ) : AttemptFirstFactor {
  //        constructor(password: String) : this(PASSWORD, password)
  //      }
  //
  //      @AutoMap
  //      @Serializable
  //      data class Passkey(override val strategy: String = PASSKEY, val passkey: String) :
  //        AttemptFirstFactor {
  //        constructor(passkey: String) : this(PASSKEY, passkey)
  //      }
  //
  //      @AutoMap
  //      @Serializable
  //      data class ResetPasswordEmailCode(
  //        override val strategy: String = RESET_PASSWORD_EMAIL_CODE,
  //        val code: String,
  //      ) : AttemptFirstFactor {
  //        constructor(identifier: String) : this(RESET_PASSWORD_EMAIL_CODE, identifier)
  //      }
  //
  //      @AutoMap
  //      @Serializable
  //      data class ResetPasswordPhoneCode(
  //        override val strategy: String = RESET_PASSWORD_PHONE_CODE,
  //        val code: String,
  //      ) : AttemptFirstFactor {
  //        constructor(password: String) : this(RESET_PASSWORD_PHONE_CODE, password)
  //      }
  //    }
  //  }

  object SignUpRequest {

    /**
     * Represents the various strategies for initiating a `SignUp` request. This sealed class acts
     * as a factory for the different combinations of parameters you can send to the create method
     */
    sealed interface Create {

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
      ) : Create

      /**
       * The `SignUp` will be created without any parameters.
       *
       * This is useful for inspecting a newly created `SignUp` object before deciding on a
       * strategy.
       */
      object None : Create
    }

    /** Defines the parameters required to prepare a verification for the sign-up process. */
    enum class PrepareVerification(val strategy: String) {
      /** Send a text message with a unique token to input */
      PHONE_CODE("phone_code"),

      /** Send an email with a unique token to input */
      EMAIL_CODE("email_code"),
    }

    /** Defines the strategies for attempting verification during the sign-up process. */
    sealed interface AttemptVerification {
      /** The strategy used for verification (e.g., `email_code` or `phone_code`). */
      val strategy: String

      /** The verification code provided by the user. */
      val code: String

      /**
       * Attempts verification using a code sent to the user's email address.
       *
       * @param code The one-time code sent to the user's email address.
       */
      data class EmailCode(override val strategy: String = EMAIL_CODE, override val code: String) :
        AttemptVerification {
        constructor(code: String) : this(EMAIL_CODE, code)
      }

      /**
       * Attempts verification using a code sent to the user's phone number.
       *
       * @param code The one-time code sent to the user's phone number.
       */
      data class PhoneCode(override val strategy: String = PHONE_CODE, override val code: String) :
        AttemptVerification {
        constructor(code: String) : this(PHONE_CODE, code)
      }
    }
  }
}
