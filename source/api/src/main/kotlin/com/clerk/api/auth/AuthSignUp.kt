package com.clerk.api.auth

import com.clerk.api.Clerk
import com.clerk.api.auth.builders.EnterpriseSsoBuilder
import com.clerk.api.auth.builders.SignUpBuilder
import com.clerk.api.auth.builders.SignUpWithIdTokenBuilder
import com.clerk.api.auth.types.IdTokenProvider
import com.clerk.api.network.ClerkApi
import com.clerk.api.network.model.error.ClerkErrorResponse
import com.clerk.api.network.serialization.ClerkResult
import com.clerk.api.signup.SignUp
import com.clerk.api.sso.OAuthProvider
import com.clerk.api.sso.OAuthResult
import com.clerk.api.sso.RedirectConfiguration
import com.clerk.api.sso.SSOService

/**
 * Creates a new sign-up with the provided details.
 *
 * @param block Builder block to configure the sign-up details.
 * @return A [ClerkResult] containing the [SignUp] object on success, or a [ClerkErrorResponse] on
 *   failure.
 *
 * ### Example usage:
 * ```kotlin
 * val signUp = clerk.auth.signUp {
 *     email = "newuser@email.com"
 *     password = "secretpassword"
 *     firstName = "John"
 *     lastName = "Doe"
 * }
 * ```
 */
suspend fun Auth.signUp(block: SignUpBuilder.() -> Unit): ClerkResult<SignUp, ClerkErrorResponse> {
  val builder = SignUpBuilder().apply(block)

  val params = buildMap {
    builder.email?.let { put("email_address", it) }
    builder.phone?.let { put("phone_number", it) }
    builder.password?.let { put("password", it) }
    builder.firstName?.let { put("first_name", it) }
    builder.lastName?.let { put("last_name", it) }
    builder.username?.let { put("username", it) }
    builder.legalAccepted?.let { put("legal_accepted", it.toString()) }
    put("locale", Clerk.locale.value.orEmpty())
  }

  return ClerkApi.signUp.createSignUp(params)
}

/**
 * Signs up with OAuth provider.
 *
 * @param provider The OAuth provider to use for sign-up.
 * @return A [ClerkResult] containing the [OAuthResult] on success, or a [ClerkErrorResponse] on
 *   failure.
 *
 * ### Example usage:
 * ```kotlin
 * val result = clerk.auth.signUpWithOAuth(OAuthProvider.GOOGLE)
 * ```
 */
suspend fun Auth.signUpWithOAuth(
  provider: OAuthProvider
): ClerkResult<OAuthResult, ClerkErrorResponse> {
  return SSOService.authenticateWithRedirect(
    strategy = provider.providerData.strategy,
    redirectUrl = RedirectConfiguration.DEFAULT_REDIRECT_URL,
  )
}

/**
 * Signs up with an ID token from an identity provider.
 *
 * @param token The ID token from the identity provider.
 * @param provider The ID token provider.
 * @param block Optional builder block to provide additional sign-up details.
 * @return A [ClerkResult] containing the [SignUp] object on success, or a [ClerkErrorResponse] on
 *   failure.
 *
 * ### Example usage:
 * ```kotlin
 * val result = clerk.auth.signUpWithIdToken(idToken, IdTokenProvider.GOOGLE) {
 *     firstName = "John"
 *     lastName = "Doe"
 * }
 * ```
 */
suspend fun Auth.signUpWithIdToken(
  token: String,
  provider: IdTokenProvider,
  block: SignUpWithIdTokenBuilder.() -> Unit = {},
): ClerkResult<SignUp, ClerkErrorResponse> {
  val builder = SignUpWithIdTokenBuilder().apply(block)

  val strategy =
    when (provider) {
      IdTokenProvider.GOOGLE -> "google_one_tap"
    }

  val params = buildMap {
    put("strategy", strategy)
    put("token", token)
    builder.firstName?.let { put("first_name", it) }
    builder.lastName?.let { put("last_name", it) }
    put("locale", Clerk.locale.value.orEmpty())
  }

  return ClerkApi.signUp.createSignUp(params)
}

/**
 * Signs up via account portal.
 *
 * Opens the Clerk-hosted account portal for sign-up.
 *
 * @return A [ClerkResult] containing the [OAuthResult] on success, or a [ClerkErrorResponse] on
 *   failure.
 */
suspend fun Auth.signUpWithAccountPortal(): ClerkResult<OAuthResult, ClerkErrorResponse> {
  return SSOService.authenticateWithRedirect(
    redirectUrl = RedirectConfiguration.DEFAULT_REDIRECT_URL
  )
}

/**
 * Signs up with Enterprise SSO.
 *
 * @param block Builder block to configure the Enterprise SSO options.
 * @return A [ClerkResult] containing the [OAuthResult] on success, or a [ClerkErrorResponse] on
 *   failure.
 *
 * ### Example usage:
 * ```kotlin
 * val result = clerk.auth.signUpWithEnterpriseSso { email = "user@company.com" }
 * ```
 */
suspend fun Auth.signUpWithEnterpriseSso(
  block: EnterpriseSsoBuilder.() -> Unit
): ClerkResult<OAuthResult, ClerkErrorResponse> {
  val builder = EnterpriseSsoBuilder().apply(block)
  builder.validate()

  return SSOService.authenticateWithRedirect(
    strategy = com.clerk.api.Constants.Strategy.ENTERPRISE_SSO,
    redirectUrl = RedirectConfiguration.DEFAULT_REDIRECT_URL,
    emailAddress = builder.email,
  )
}

/**
 * Signs up with a ticket.
 *
 * @param ticket The sign-up ticket.
 * @return A [ClerkResult] containing the [SignUp] object on success, or a [ClerkErrorResponse] on
 *   failure.
 *
 * ### Example usage:
 * ```kotlin
 * val signUp = clerk.auth.signUpWithTicket(ticket)
 * ```
 */
suspend fun Auth.signUpWithTicket(ticket: String): ClerkResult<SignUp, ClerkErrorResponse> {
  val params =
    mapOf("strategy" to "ticket", "ticket" to ticket, "locale" to Clerk.locale.value.orEmpty())

  return ClerkApi.signUp.createSignUp(params)
}
