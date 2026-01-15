package com.clerk.api.auth

import com.clerk.api.Clerk
import com.clerk.api.Constants.Strategy.EMAIL_CODE
import com.clerk.api.Constants.Strategy.PASSWORD
import com.clerk.api.Constants.Strategy.PHONE_CODE
import com.clerk.api.auth.builders.EnterpriseSsoBuilder
import com.clerk.api.auth.builders.SignInIdentifierBuilder
import com.clerk.api.auth.builders.SignInWithIdTokenBuilder
import com.clerk.api.auth.builders.SignInWithOtpBuilder
import com.clerk.api.auth.builders.SignInWithPasswordBuilder
import com.clerk.api.auth.types.IdTokenProvider
import com.clerk.api.network.ClerkApi
import com.clerk.api.network.model.error.ClerkErrorResponse
import com.clerk.api.network.serialization.ClerkResult
import com.clerk.api.passkeys.PasskeyService
import com.clerk.api.signin.SignIn
import com.clerk.api.signin.toMap
import com.clerk.api.sso.OAuthProvider
import com.clerk.api.sso.OAuthResult
import com.clerk.api.sso.RedirectConfiguration
import com.clerk.api.sso.SSOService

/**
 * Starts sign-in with an identifier (email, phone, or username).
 *
 * This method creates a sign-in attempt with the provided identifier. A separate [SignIn.sendCode]
 * call is required to send the verification code.
 *
 * @param block Builder block to configure the sign-in identifier.
 * @return A [ClerkResult] containing the [SignIn] object on success, or a [ClerkErrorResponse] on
 *   failure.
 *
 * ### Example usage:
 * ```kotlin
 * val signIn = clerk.auth.signIn { email = "user@email.com" }
 * // Then send code separately
 * signIn.sendCode { email = "user@email.com" }
 * signIn.verifyCode("123456")
 * ```
 */
suspend fun Auth.signIn(
  block: SignInIdentifierBuilder.() -> Unit
): ClerkResult<SignIn, ClerkErrorResponse> {
  val builder = SignInIdentifierBuilder().apply(block)
  builder.validate()

  val params =
    mapOf("identifier" to builder.getIdentifier(), "locale" to Clerk.locale.value.orEmpty())

  return ClerkApi.signIn.createSignIn(params)
}

/**
 * Signs in with password authentication.
 *
 * @param block Builder block to configure the identifier and password.
 * @return A [ClerkResult] containing the [SignIn] object on success, or a [ClerkErrorResponse] on
 *   failure.
 *
 * ### Example usage:
 * ```kotlin
 * val signIn = clerk.auth.signInWithPassword {
 *     identifier = "user@email.com"
 *     password = "secretpassword"
 * }
 * ```
 */
suspend fun Auth.signInWithPassword(
  block: SignInWithPasswordBuilder.() -> Unit
): ClerkResult<SignIn, ClerkErrorResponse> {
  val builder = SignInWithPasswordBuilder().apply(block)
  builder.validate()

  val params =
    mapOf(
      "identifier" to builder.identifier!!,
      "password" to builder.password!!,
      "strategy" to PASSWORD,
      "locale" to Clerk.locale.value.orEmpty(),
    )

  return ClerkApi.signIn.createSignIn(params)
}

/**
 * Signs in with OTP - automatically sends the verification code.
 *
 * This is a one-shot method that creates the sign-in and immediately prepares the first factor
 * verification, sending the code to the specified channel.
 *
 * @param block Builder block to configure the email or phone.
 * @return A [ClerkResult] containing the [SignIn] object on success, or a [ClerkErrorResponse] on
 *   failure.
 *
 * ### Example usage:
 * ```kotlin
 * val signIn = clerk.auth.signInWithOtp { email = "user@email.com" }
 * signIn.verifyCode("123456")
 * ```
 */
suspend fun Auth.signInWithOtp(
  block: SignInWithOtpBuilder.() -> Unit
): ClerkResult<SignIn, ClerkErrorResponse> {
  val builder = SignInWithOtpBuilder().apply(block)
  builder.validate()

  val identifier = builder.email ?: builder.phone!!
  val strategy = if (builder.email != null) EMAIL_CODE else PHONE_CODE

  val params =
    mapOf(
      "identifier" to identifier,
      "strategy" to strategy,
      "locale" to Clerk.locale.value.orEmpty(),
    )

  return when (val createResult = ClerkApi.signIn.createSignIn(params)) {
    is ClerkResult.Failure -> createResult
    is ClerkResult.Success -> {
      val signIn = createResult.value
      // Prepare first factor to send the code
      val prepareParams =
        if (builder.email != null) {
          SignIn.PrepareFirstFactorParams.EmailCode(
            emailAddressId =
              signIn.supportedFirstFactors?.find { it.strategy == EMAIL_CODE }?.emailAddressId ?: ""
          )
        } else {
          SignIn.PrepareFirstFactorParams.PhoneCode(
            phoneNumberId =
              signIn.supportedFirstFactors?.find { it.strategy == PHONE_CODE }?.phoneNumberId ?: ""
          )
        }
      ClerkApi.signIn.prepareSignInFirstFactor(signIn.id, prepareParams.toMap())
    }
  }
}

/**
 * Signs in with OAuth provider.
 *
 * @param provider The OAuth provider to use for authentication.
 * @return A [ClerkResult] containing the [OAuthResult] on success, or a [ClerkErrorResponse] on
 *   failure.
 *
 * ### Example usage:
 * ```kotlin
 * val result = clerk.auth.signInWithOAuth(OAuthProvider.GOOGLE)
 * ```
 */
suspend fun Auth.signInWithOAuth(
  provider: OAuthProvider
): ClerkResult<OAuthResult, ClerkErrorResponse> {
  return SSOService.authenticateWithRedirect(
    strategy = provider.providerData.strategy,
    redirectUrl = RedirectConfiguration.DEFAULT_REDIRECT_URL,
  )
}

/**
 * Signs in with an ID token from an identity provider.
 *
 * @param block Builder block to configure the token and provider.
 * @return A [ClerkResult] containing the [OAuthResult] on success, or a [ClerkErrorResponse] on
 *   failure.
 *
 * ### Example usage:
 * ```kotlin
 * val result = clerk.auth.signInWithIdToken {
 *     token = idToken
 *     provider = IdTokenProvider.GOOGLE
 * }
 * ```
 */
suspend fun Auth.signInWithIdToken(
  block: SignInWithIdTokenBuilder.() -> Unit
): ClerkResult<OAuthResult, ClerkErrorResponse> {
  val builder = SignInWithIdTokenBuilder().apply(block)
  builder.validate()

  return when (builder.provider!!) {
    IdTokenProvider.GOOGLE -> {
      when (val result = ClerkApi.signIn.authenticateWithGoogle(token = builder.token!!)) {
        is ClerkResult.Success -> ClerkResult.success(OAuthResult(signIn = result.value))
        is ClerkResult.Failure -> ClerkResult.apiFailure(result.error)
      }
    }
  }
}

/**
 * Signs in with passkey.
 *
 * @return A [ClerkResult] containing the [SignIn] object on success, or a [ClerkErrorResponse] on
 *   failure.
 *
 * ### Example usage:
 * ```kotlin
 * val signIn = clerk.auth.signInWithPasskey()
 * ```
 */
suspend fun Auth.signInWithPasskey(): ClerkResult<SignIn, ClerkErrorResponse> {
  return PasskeyService.signInWithPasskey()
}

/**
 * Signs in via account portal.
 *
 * Opens the Clerk-hosted account portal for authentication.
 *
 * @return A [ClerkResult] containing the [OAuthResult] on success, or a [ClerkErrorResponse] on
 *   failure.
 */
suspend fun Auth.signInWithAccountPortal(): ClerkResult<OAuthResult, ClerkErrorResponse> {
  // Account portal uses OAuth-like redirect flow
  return SSOService.authenticateWithRedirect(
    redirectUrl = RedirectConfiguration.DEFAULT_REDIRECT_URL
  )
}

/**
 * Signs in with Enterprise SSO.
 *
 * @param block Builder block to configure the Enterprise SSO options.
 * @return A [ClerkResult] containing the [OAuthResult] on success, or a [ClerkErrorResponse] on
 *   failure.
 *
 * ### Example usage:
 * ```kotlin
 * val result = clerk.auth.signInWithEnterpriseSSO { email = "user@company.com" }
 * ```
 */
suspend fun Auth.signInWithEnterpriseSso(
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
 * Signs in with a ticket.
 *
 * @param ticket The authentication ticket.
 * @return A [ClerkResult] containing the [SignIn] object on success, or a [ClerkErrorResponse] on
 *   failure.
 *
 * ### Example usage:
 * ```kotlin
 * val signIn = clerk.auth.signInWithTicket(ticket)
 * ```
 */
suspend fun Auth.signInWithTicket(ticket: String): ClerkResult<SignIn, ClerkErrorResponse> {
  val params =
    mapOf(
      "strategy" to com.clerk.api.Constants.Strategy.TICKET,
      "ticket" to ticket,
      "locale" to Clerk.locale.value.orEmpty(),
    )

  return ClerkApi.signIn.createSignIn(params)
}
