package com.clerk.sdk.network.requests

import kotlinx.serialization.Serializable

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
    /** A parameter object for preparing the second factor verification. */
    @Serializable
    data class PrepareSecondFactorParams(
      /** The strategy used for second factor verification. */
      val strategy: String
    )

    /** A strategy for preparing the second factor verification process. */
    enum class PrepareSecondFactorStrategy {
      /**
       * phoneCode: The user will receive a one-time authentication code via SMS. At least one phone
       * number should be on file for the user.
       */
      PHONE_CODE;

      val params: PrepareSecondFactorParams
        get() =
          when (this) {
            PHONE_CODE -> PrepareSecondFactorParams(strategy = "phone_code")
          }
    }

    /**
     * A parameter object for resetting a user's password.
     *
     * @param password The user's current password
     * @param signOutOfOtherSessions Whether to sign out of all other sessions after the password
     */
    @Serializable
    data class ResetPasswordParams(
      val password: String,
      val signOutOfOtherSessions: Boolean? = null,
    )
  }
}
