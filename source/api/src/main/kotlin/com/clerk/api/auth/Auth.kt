package com.clerk.api.auth

import android.net.Uri
import com.clerk.api.Clerk
import com.clerk.api.Constants.Strategy.EMAIL_CODE
import com.clerk.api.Constants.Strategy.PASSWORD
import com.clerk.api.Constants.Strategy.PHONE_CODE
import com.clerk.api.auth.builders.EnterpriseSsoBuilder
import com.clerk.api.auth.builders.SignInIdentifierBuilder
import com.clerk.api.auth.builders.SignInWithIdTokenBuilder
import com.clerk.api.auth.builders.SignInWithOtpBuilder
import com.clerk.api.auth.builders.SignInWithPasswordBuilder
import com.clerk.api.auth.builders.SignUpBuilder
import com.clerk.api.auth.builders.SignUpWithIdTokenBuilder
import com.clerk.api.auth.types.IdTokenProvider
import com.clerk.api.network.ClerkApi
import com.clerk.api.network.model.error.ClerkErrorResponse
import com.clerk.api.network.serialization.ClerkResult
import com.clerk.api.passkeys.PasskeyService
import com.clerk.api.session.GetTokenOptions
import com.clerk.api.session.Session
import com.clerk.api.session.fetchToken
import com.clerk.api.session.revoke
import com.clerk.api.signin.SignIn
import com.clerk.api.signin.toMap
import com.clerk.api.signout.SignOutService
import com.clerk.api.signup.SignUp
import com.clerk.api.sso.OAuthProvider
import com.clerk.api.sso.OAuthResult
import com.clerk.api.sso.RedirectConfiguration
import com.clerk.api.sso.SSOService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Main Auth class providing all authentication entry points.
 *
 * Access via `Clerk.auth`.
 *
 * This class provides a centralized, DSL-style API for all authentication operations including:
 * - Sign in with various methods (password, OTP, OAuth, passkey, etc.)
 * - Sign up with various methods
 * - Session management (sign out, set active session, get tokens)
 * - Deep link handling for OAuth/SSO callbacks
 *
 * ### Example usage:
 * ```kotlin
 * // Sign in with email (requires separate sendCode call)
 * val signIn = clerk.auth.signIn { email = "user@email.com" }
 *
 * // Sign in with password
 * val signIn = clerk.auth.signInWithPassword {
 *     identifier = "user@email.com"
 *     password = "password"
 * }
 *
 * // Sign in with OTP (automatically sends code)
 * val signIn = clerk.auth.signInWithOtp { email = "user@email.com" }
 *
 * // Sign out
 * clerk.auth.signOut()
 * ```
 */
@Suppress("TooManyFunctions")
class Auth internal constructor() {

  private val _events = MutableSharedFlow<AuthEvent>()

  /**
   * Flow of authentication events.
   *
   * Subscribe to this flow to receive notifications about authentication state changes, including
   * sign-in, sign-out, session changes, and errors.
   */
  val events: Flow<AuthEvent> = _events.asSharedFlow()

  // region Current Sign In/Sign Up State

  /**
   * The current sign-in attempt, if one is in progress.
   *
   * This represents an ongoing authentication flow and provides access to verification steps and
   * authentication state. Returns `null` when no sign-in is active or if the SDK is not
   * initialized.
   *
   * ### Example usage:
   * ```kotlin
   * val currentSignIn = Clerk.auth.currentSignIn
   * if (currentSignIn != null) {
   *     // Handle ongoing sign-in
   * }
   * ```
   */
  val currentSignIn: SignIn?
    get() = if (Clerk.clientInitialized) Clerk.client.signIn else null

  /**
   * The current sign-up attempt, if one is in progress.
   *
   * This represents an ongoing user registration flow and provides access to verification steps and
   * registration state. Returns `null` when no sign-up is active or if the SDK is not initialized.
   *
   * ### Example usage:
   * ```kotlin
   * val currentSignUp = Clerk.auth.currentSignUp
   * if (currentSignUp != null) {
   *     // Handle ongoing sign-up
   * }
   * ```
   */
  val currentSignUp: SignUp?
    get() = if (Clerk.clientInitialized) Clerk.client.signUp else null

  // endregion

  // region Sign In

  /**
   * Starts sign-in with an identifier (email, phone, or username).
   *
   * This method creates a sign-in attempt with the provided identifier. A separate
   * [SignIn.sendCode] call is required to send the verification code.
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
  suspend fun signIn(
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
  suspend fun signInWithPassword(
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
  suspend fun signInWithOtp(
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
                signIn.supportedFirstFactors?.find { it.strategy == EMAIL_CODE }?.emailAddressId
                  ?: ""
            )
          } else {
            SignIn.PrepareFirstFactorParams.PhoneCode(
              phoneNumberId =
                signIn.supportedFirstFactors?.find { it.strategy == PHONE_CODE }?.phoneNumberId
                  ?: ""
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
  suspend fun signInWithOAuth(
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
  suspend fun signInWithIdToken(
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
  suspend fun signInWithPasskey(): ClerkResult<SignIn, ClerkErrorResponse> {
    return PasskeyService.signInWithPasskey()
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
  suspend fun signInWithEnterpriseSso(
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
  suspend fun signInWithTicket(ticket: String): ClerkResult<SignIn, ClerkErrorResponse> {
    val params =
      mapOf(
        "strategy" to com.clerk.api.Constants.Strategy.TICKET,
        "ticket" to ticket,
        "locale" to Clerk.locale.value.orEmpty(),
      )

    return ClerkApi.signIn.createSignIn(params)
  }

  // endregion

  // region Sign Up

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
  suspend fun signUp(block: SignUpBuilder.() -> Unit): ClerkResult<SignUp, ClerkErrorResponse> {
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
  suspend fun signUpWithOAuth(
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
  suspend fun signUpWithIdToken(
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
  suspend fun signUpWithEnterpriseSso(
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
  suspend fun signUpWithTicket(ticket: String): ClerkResult<SignUp, ClerkErrorResponse> {
    val params =
      mapOf("strategy" to "ticket", "ticket" to ticket, "locale" to Clerk.locale.value.orEmpty())

    return ClerkApi.signUp.createSignUp(params)
  }

  // endregion

  // region Session Management

  /**
   * Signs out the current session or a specific session.
   *
   * @param sessionId Optional session ID to sign out. If null, signs out the current session.
   * @return A [ClerkResult] with Unit on success, or a [ClerkErrorResponse] on failure.
   *
   * ### Example usage:
   * ```kotlin
   * clerk.auth.signOut()
   * ```
   */
  suspend fun signOut(sessionId: String? = null): ClerkResult<Unit, ClerkErrorResponse> {
    return if (sessionId != null) {
      // Sign out a specific session
      when (val result = ClerkApi.session.removeSession(sessionId)) {
        is ClerkResult.Success -> ClerkResult.success(Unit)
        is ClerkResult.Failure -> ClerkResult.apiFailure(result.error)
      }
    } else {
      SignOutService.signOut()
    }
  }

  /**
   * Sets the active session.
   *
   * @param sessionId The ID of the session to set as active.
   * @param organizationId Optional organization ID to set as active for the session.
   * @return A [ClerkResult] containing the [Session] on success, or a [ClerkErrorResponse] on
   *   failure.
   *
   * ### Example usage:
   * ```kotlin
   * clerk.auth.setActive(sessionId, organizationId)
   * ```
   */
  suspend fun setActive(
    sessionId: String,
    organizationId: String? = null,
  ): ClerkResult<Session, ClerkErrorResponse> {
    return ClerkApi.client.setActive(sessionId, organizationId)
  }

  /**
   * Gets a token for the current session.
   *
   * @param options Optional token retrieval options.
   * @return A [ClerkResult] containing the token string on success, or a [ClerkErrorResponse] on
   *   failure.
   *
   * ### Example usage:
   * ```kotlin
   * val token = clerk.auth.getToken()
   * // or with options
   * val token = clerk.auth.getToken(GetTokenOptions(template = "my-template"))
   * ```
   */
  suspend fun getToken(options: GetTokenOptions? = null): ClerkResult<String, ClerkErrorResponse> {
    val session =
      Clerk.session
        ?: return ClerkResult.apiFailure(
          ClerkErrorResponse(errors = emptyList(), clerkTraceId = "no-session")
        )

    return when (val result = session.fetchToken(options ?: GetTokenOptions())) {
      is ClerkResult.Success -> ClerkResult.success(result.value.jwt)
      is ClerkResult.Failure -> ClerkResult.apiFailure(result.error)
    }
  }

  /**
   * Revokes a session.
   *
   * @param session The session to revoke.
   * @return A [ClerkResult] with Unit on success, or a [ClerkErrorResponse] on failure.
   *
   * ### Example usage:
   * ```kotlin
   * clerk.auth.revokeSession(session)
   * ```
   */
  suspend fun revokeSession(session: Session): ClerkResult<Unit, ClerkErrorResponse> {
    return when (val result = session.revoke()) {
      is ClerkResult.Success -> ClerkResult.success(Unit)
      is ClerkResult.Failure -> ClerkResult.apiFailure(result.error)
    }
  }

  // endregion

  // region Deep Link Handling

  /**
   * Handles OAuth/SSO deep link callbacks.
   *
   * Call this method from your Activity when receiving a deep link callback from an OAuth or SSO
   * provider.
   *
   * @param uri The deep link URI received from the callback.
   * @return true if the URI was handled, false otherwise.
   *
   * ### Example usage:
   * ```kotlin
   * // In your Activity's onCreate or onNewIntent
   * clerk.auth.handle(intent.data)
   * ```
   */
  fun handle(uri: Uri?): Boolean {
    // Check if this is a Clerk OAuth callback
    val isClerkCallback = uri?.scheme?.startsWith("clerk") == true

    if (isClerkCallback) {
      // Let the SSO service handle the callback
      kotlinx.coroutines.runBlocking { SSOService.completeAuthenticateWithRedirect(uri) }
    }

    return isClerkCallback
  }

  // endregion
}
