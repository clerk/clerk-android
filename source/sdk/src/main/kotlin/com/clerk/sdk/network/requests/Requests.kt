package com.clerk.sdk.network.requests

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

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

  object SignUp {

    /**
     * Parameters used to create and configure a new sign-up process.
     *
     * The `CreateParams` class defines all the parameters that can be passed when initiating a
     * sign-up process. These parameters provide flexibility to support various authentication
     * strategies, user details, and custom configurations.
     */
    @Serializable
    data class CreateSignUpParams(
      /** The strategy to use for the sign-up flow. */
      val strategy: String? = null,

      /** The user's first name. Only supported if name is enabled. */
      val firstName: String? = null,

      /** The user's last name. Only supported if name is enabled. */
      val lastName: String? = null,

      /** The user's password. Only supported if password is enabled. */
      val password: String? = null,

      /**
       * The user's email address. Only supported if email address is enabled. Keep in mind that the
       * email address requires an extra verification process.
       */
      val emailAddress: String? = null,

      /**
       * The user's phone number in E.164 format. Only supported if phone number is enabled. Keep in
       * mind that the phone number requires an extra verification process.
       */
      val phoneNumber: String? = null,

      /**
       * Required if Web3 authentication is enabled. The Web3 wallet address, made up of 0x + 40
       * hexadecimal characters.
       */
      val web3Wallet: String? = null,

      /** The user's username. Only supported if usernames are enabled. */
      val username: String? = null,

      /**
       * Metadata that can be read and set from the frontend.
       *
       * Once the sign-up is complete, the value of this field will be automatically copied to the
       * newly created user's unsafe metadata. One common use case for this attribute is to use it
       * to implement custom fields that can be collected during sign-up and will automatically be
       * attached to the created User object.
       */
      val unsafeMetadata: JsonObject? = null,

      /**
       * If strategy is set to 'oauth_{provider}' or 'enterprise_sso', this specifies full URL or
       * path that the OAuth provider should redirect to after successful authorization on their
       * part.
       *
       * If strategy is set to 'email_link', this specifies The full URL that the user will be
       * redirected to when they visit the email link. See the custom flow for implementation
       * details.
       */
      val redirectUrl: String? = null,

      /**
       * Required if strategy is set to 'ticket'. The ticket or token generated from the Backend
       * API.
       */
      val ticket: String? = null,

      /**
       * When set to true, the SignUp will attempt to retrieve information from the active SignIn
       * instance and use it to complete the sign-up process.
       *
       * This is useful when you want to seamlessly transition a user from a sign-in attempt to a
       * sign-up attempt.
       */
      val transfer: Boolean? = null,

      /** A boolean indicating whether the user has agreed to the legal compliance documents. */
      val legalAccepted: Boolean? = null,

      /**
       * Optional if strategy is set to 'oauth_{provider}' or 'enterprise_sso'. The value to pass to
       * the OIDC prompt parameter in the generated OAuth redirect URL.
       */
      val oidcPrompt: String? = null,

      /**
       * Optional if strategy is set to 'oauth_<provider>' or 'enterprise_sso'. The value to pass to
       * the OIDC login_hint parameter in the generated OAuth redirect URL.
       */
      val oidcLoginHint: String? = null,

      /** The ID token from a provider used for authentication (e.g., SignInWithApple). */
      val token: String? = null,
    )

    /** Represents the various strategies for initiating a `SignUp` request. */
    sealed class CreateStrategy {
      /**
       * Standard sign-up strategy, allowing the user to provide common details such as email,
       * password, and personal information.
       */
      data class Standard(
        val emailAddress: String? = null,
        val password: String? = null,
        val firstName: String? = null,
        val lastName: String? = null,
        val username: String? = null,
        val phoneNumber: String? = null,
      ) : CreateStrategy()

      /** OAuth-based sign-up strategy, using an external provider for authentication. */
      data class OAuth(val provider: OAuthProvider, val redirectUrl: String? = null) :
        CreateStrategy()

      /**
       * Enterprise single sign-on (SSO) sign-up strategy, allowing authentication through an
       * enterprise identity provider.
       */
      data class EnterpriseSSO(val identifier: String, val redirectUrl: String? = null) :
        CreateStrategy()

      /** The user will be authenticated via the ticket or token generated from the Backend API. */
      data class Ticket(val ticket: String) : CreateStrategy()

      /**
       * Sign-up strategy using an ID Token, typically obtained from third-party identity providers
       * like Apple.
       */
      data class IdToken(
        val provider: IDTokenProvider,
        val idToken: String,
        val firstName: String? = null,
        val lastName: String? = null,
      ) : CreateStrategy()

      /** Transfers an active sign-in instance to a new sign-up process. */
      object Transfer : CreateStrategy()

      /**
       * The `SignUp` will be created without any parameters.
       *
       * This is useful for inspecting a newly created `SignUp` object before deciding on a
       * strategy.
       */
      object None : CreateStrategy()

      /**
       * Converts the strategy into the appropriate `CreateParams` object for a `SignUp` request.
       */
      fun toParams(): CreateSignUpParams =
        when (this) {
          is Standard ->
            CreateSignUpParams(
              firstName = firstName,
              lastName = lastName,
              password = password,
              emailAddress = emailAddress,
              phoneNumber = phoneNumber,
              username = username,
            )
          is OAuth ->
            CreateSignUpParams(
              strategy = provider.strategy,
              redirectUrl = redirectUrl ?: RedirectConfigDefaults.redirectUrl,
            )
          is EnterpriseSSO ->
            CreateSignUpParams(
              strategy = "enterprise_sso",
              emailAddress = identifier,
              redirectUrl = redirectUrl ?: RedirectConfigDefaults.redirectUrl,
            )
          is Ticket -> CreateSignUpParams(strategy = "ticket", ticket = ticket)
          is IdToken ->
            CreateSignUpParams(
              strategy = provider.strategy,
              firstName = firstName,
              lastName = lastName,
              token = idToken,
            )
          is Transfer -> CreateSignUpParams(transfer = true)
          is None -> CreateSignUpParams()
        }
    }

    /** UpdateParams is a mirror of CreateParams with the same fields and types. */
    typealias UpdateParams = CreateParams
  }
}
