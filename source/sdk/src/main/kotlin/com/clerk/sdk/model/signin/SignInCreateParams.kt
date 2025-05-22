package com.clerk.sdk.model.signin

import com.clerk.sdk.network.requests.Requests.SignInRequest.PrepareFirstFactorStrategy

/**
 * Represents the parameters required to initiate a sign-in flow.
 *
 * This class encapsulates the various options for initiating a sign-in, including the
 * authentication strategy, user identifier, optional passwords, and additional settings for
 * redirect URLs or OAuth-specific parameters.
 */
sealed class SignInCreateParams {
  /**
   * The user will be authenticated either through SAML or OIDC depending on the configuration of
   * their enterprise SSO account.
   */
  data class EnterpriseSSO(val identifier: String, val redirectUrl: String? = null) :
    SignInCreateParams()

  /**
   * The user will be authenticated using an ID Token, typically obtained from third-party identity
   * providers like Apple.
   */
  data class IdToken(val provider: IDTokenProvider, val idToken: String) : SignInCreateParams()

  /** The user will be authenticated with the provided identifier. */
  data class Identifier(
    val identifier: String,
    val password: String? = null,
    val strategy: PrepareFirstFactorStrategy? = null,
  ) : SignInCreateParams()

  /** The user will be authenticated with their social connection account. */
  data class OAuth(val provider: OAuthProvider, val redirectUrl: String? = null) :
    SignInCreateParams()

  /** The user will be authenticated with their passkey. */
  object Passkey : SignInCreateParams()

  /** The user will be authenticated via the ticket or token generated from the Backend API. */
  data class Ticket(val ticket: String) : SignInCreateParams()

  /**
   * The `SignIn` will attempt to retrieve information from the active `SignUp` instance and use it
   * to complete the sign-in process.
   *
   * This is useful for seamlessly transitioning a user from a sign-up attempt to a sign-in attempt.
   */
  object Transfer : SignInCreateParams()

  /**
   * The `SignIn` will be created without any parameters.
   *
   * This is useful for inspecting a newly created `SignIn` object before deciding on a strategy.
   */
  object None : SignInCreateParams()
}
