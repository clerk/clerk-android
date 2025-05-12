package com.clerk.sdk.network

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

    fun PrepareFirstFactorParams.toMap(): Map<String, String?> {
      return mapOf(
        "strategy" to strategy,
        "email_address_id" to emailAddressId,
        "phone_number_id" to phoneNumberId,
        "redirect_url" to redirectUrl,
      )
    }
  }
}
