package com.clerk.api.auth

import android.net.Uri
import com.clerk.api.Clerk
import com.clerk.api.auth.builders.EnterpriseSsoBuilder
import com.clerk.api.auth.builders.SignInIdentifierBuilder
import com.clerk.api.auth.builders.SignInWithIdTokenBuilder
import com.clerk.api.auth.builders.SignInWithOtpBuilder
import com.clerk.api.auth.builders.SignInWithPasswordBuilder
import com.clerk.api.auth.builders.SignUpBuilder
import com.clerk.api.auth.builders.SignUpWithIdTokenBuilder
import com.clerk.api.auth.types.IdTokenProvider
import com.clerk.api.auth.types.Strategy
import com.clerk.api.hostedauth.HostedAuthCancellationException
import com.clerk.api.hostedauth.HostedAuthService
import com.clerk.api.log.ClerkLog
import com.clerk.api.magiclink.NativeMagicLinkAuthResult
import com.clerk.api.magiclink.NativeMagicLinkError
import com.clerk.api.magiclink.NativeMagicLinkManager
import com.clerk.api.magiclink.NativeMagicLinkService
import com.clerk.api.magiclink.canHandleNativeMagicLink
import com.clerk.api.network.ClerkApi
import com.clerk.api.network.api.SET_ACTIVE_INTENT_SELECT_ORG
import com.clerk.api.network.model.client.Client
import com.clerk.api.network.model.error.ClerkErrorResponse
import com.clerk.api.network.serialization.ClerkResult
import com.clerk.api.network.serialization.errorMessage
import com.clerk.api.passkeys.PasskeyService
import com.clerk.api.restorecredentials.RestoreCredentials
import com.clerk.api.session.GetTokenOptions
import com.clerk.api.session.Session
import com.clerk.api.session.fetchToken
import com.clerk.api.session.revoke
import com.clerk.api.signin.SignIn
import com.clerk.api.signout.SignOutService
import com.clerk.api.signup.SignUp
import com.clerk.api.sso.GoogleSignInService
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
public class Auth internal constructor() {

  private val _events = MutableSharedFlow<AuthEvent>(extraBufferCapacity = 64)

  /**
   * Flow of authentication events.
   *
   * Subscribe to this flow to receive notifications about authentication state changes, including
   * sign-in, sign-out, session changes, and errors.
   */
  public val events: Flow<AuthEvent> = _events.asSharedFlow()

  internal fun send(event: AuthEvent) {
    val emitted = _events.tryEmit(event)
    if (!emitted) {
      ClerkLog.w("Dropped auth event due to backpressure: ${event::class.simpleName}")
    }
  }

  internal fun emitAuthError(failure: ClerkResult.Failure<ClerkErrorResponse>) {
    send(AuthEvent.Error(message = failure.errorMessage, throwable = failure.throwable))
  }

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
  public val currentSignIn: SignIn?
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
  public val currentSignUp: SignUp?
    get() = if (Clerk.clientInitialized) Clerk.client.signUp else null

  /** Native magic-link manager for PKCE-bound email link flows. */
  public val nativeMagicLink: NativeMagicLinkManager
    get() = NativeMagicLinkService

  /**
   * The sessions currently known on this client.
   *
   * In multi-session mode this can include sessions for multiple accounts. The current session is
   * the one whose ID matches [Client.lastActiveSessionId].
   */
  public val sessions: List<Session>
    get() = if (Clerk.clientInitialized) Clerk.client.sessions else emptyList()

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
  public suspend fun signIn(
    block: SignInIdentifierBuilder.() -> Unit
  ): ClerkResult<SignIn, ClerkErrorResponse> {
    val builder = SignInIdentifierBuilder().apply(block)
    builder.validate()

    return createSignIn(
      SignIn.CreateParams.Strategy.Identifier(identifier = builder.getIdentifier())
    )
  }

  /**
   * Opens Account Portal in a system browser and activates the session created there.
   *
   * The pending flow state lives only in this process: if the system kills the app process while
   * the browser is in the foreground, the flow is lost and its callback is ignored, matching the
   * behavior of the SDK's OAuth/SSO redirect flows.
   *
   * @param mode The Account Portal screen to open first. Defaults to sign-in.
   * @param redirectUrl The native callback URL. Defaults to the callback registered by the SDK.
   *   Custom values require a matching intent filter in the application manifest and must be
   *   forwarded to [handle].
   * @return The activated session, or a failure when creation, callback validation, redemption, or
   *   activation fails. When the user dismisses the browser or another flow supersedes this one,
   *   the failure's throwable is a [HostedAuthCancellationException].
   */
  public suspend fun startHostedAuth(
    mode: HostedAuthMode? = null,
    redirectUrl: String = RedirectConfiguration.DEFAULT_REDIRECT_URL,
  ): ClerkResult<Session, ClerkErrorResponse> {
    return when (val result = HostedAuthService.start(mode = mode, redirectUrl = redirectUrl)) {
      is ClerkResult.Failure -> {
        emitAuthError(result)
        result
      }
      is ClerkResult.Success ->
        setActive(
          sessionId = result.value.id,
          organizationId = result.value.lastActiveOrganizationId,
        )
    }
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
  public suspend fun signInWithPassword(
    block: SignInWithPasswordBuilder.() -> Unit
  ): ClerkResult<SignIn, ClerkErrorResponse> {
    val builder = SignInWithPasswordBuilder().apply(block)
    builder.validate()

    return createSignIn(
      SignIn.CreateParams.Strategy.Password(
        identifier = builder.identifier!!,
        password = builder.password!!,
      )
    )
  }

  /**
   * Signs in with OTP - automatically sends the verification code.
   *
   * This is a one-shot method that creates the sign-in with an OTP strategy, sending the code to
   * the specified channel.
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
  public suspend fun signInWithOtp(
    block: SignInWithOtpBuilder.() -> Unit
  ): ClerkResult<SignIn, ClerkErrorResponse> {
    val builder = SignInWithOtpBuilder().apply(block)
    builder.validate()

    val email = builder.email
    return createSignIn(
      if (email != null) {
        SignIn.CreateParams.Strategy.EmailCode(identifier = email)
      } else {
        SignIn.CreateParams.Strategy.PhoneCode(identifier = builder.phone!!)
      }
    )
  }

  /**
   * Signs in with OAuth provider.
   *
   * @param provider The OAuth provider to use for authentication.
   * @param transferable Whether the flow may turn into a sign-up when the provider account has no
   *   Clerk user yet. When `false`, that case fails instead. Defaults to `true`.
   * @param redirectUrl The native callback URL. Defaults to the callback registered by the SDK. A
   *   custom value needs an intent filter that routes it to `com.clerk.api.sso.SSOReceiverActivity`
   *   in the application manifest. [handle] only completes OAuth callbacks with a `clerk` scheme.
   * @return A [ClerkResult] containing the [OAuthResult] on success, or a [ClerkErrorResponse] on
   *   failure. The result holds a sign-up instead of a sign-in when the flow transferred.
   *
   * ### Example usage:
   * ```kotlin
   * val result = clerk.auth.signInWithOAuth(OAuthProvider.GOOGLE)
   * ```
   */
  public suspend fun signInWithOAuth(
    provider: OAuthProvider,
    transferable: Boolean = true,
    redirectUrl: String = RedirectConfiguration.DEFAULT_REDIRECT_URL,
  ): ClerkResult<OAuthResult, ClerkErrorResponse> {
    return authenticateSignInWithRedirect(
      SignIn.AuthenticateWithRedirectParams.OAuth(provider = provider, redirectUrl = redirectUrl),
      transferable = transferable,
    )
  }

  @Deprecated("Kept for binary compatibility.", level = DeprecationLevel.HIDDEN)
  public suspend fun signInWithOAuth(
    provider: OAuthProvider
  ): ClerkResult<OAuthResult, ClerkErrorResponse> = signInWithOAuth(provider, transferable = true)

  /**
   * Signs in with Google One Tap (Credential Manager's Sign in with Google).
   *
   * @param transferable Whether the flow may turn into a sign-up when the Google account has no
   *   Clerk user yet. Defaults to `true`.
   * @return A [ClerkResult] containing the [OAuthResult] on success, or a [ClerkErrorResponse] on
   *   failure.
   *
   * ### Example usage:
   * ```kotlin
   * val result = clerk.auth.signInWithGoogleOneTap()
   * ```
   */
  public suspend fun signInWithGoogleOneTap(
    transferable: Boolean = true
  ): ClerkResult<OAuthResult, ClerkErrorResponse> {
    return reportingFailures { GoogleSignInService().signInWithGoogle(transferable) }
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
  public suspend fun signInWithIdToken(
    block: SignInWithIdTokenBuilder.() -> Unit
  ): ClerkResult<OAuthResult, ClerkErrorResponse> {
    val builder = SignInWithIdTokenBuilder().apply(block)
    builder.validate()

    return reportingFailures {
      when (builder.provider!!) {
        IdTokenProvider.GOOGLE -> {
          when (val result = ClerkApi.signIn.authenticateWithGoogle(token = builder.token!!)) {
            is ClerkResult.Success -> ClerkResult.success(OAuthResult(signIn = result.value))
            is ClerkResult.Failure -> ClerkResult.apiFailure(result.error)
          }
        }
      }
    }
  }

  /**
   * Signs in with passkey.
   *
   * @param preferImmediatelyAvailableCredentials Whether the credential provider should only return
   *   credentials available without extra provider UI. Use `true` for sign-in attempts the user did
   *   not ask for, such as offering a passkey when a screen opens. Defaults to `false`.
   * @return A [ClerkResult] containing the [SignIn] object on success, or a [ClerkErrorResponse] on
   *   failure.
   *
   * ### Example usage:
   * ```kotlin
   * val signIn = clerk.auth.signInWithPasskey()
   * ```
   */
  public suspend fun signInWithPasskey(
    preferImmediatelyAvailableCredentials: Boolean = false
  ): ClerkResult<SignIn, ClerkErrorResponse> {
    return reportingFailures {
      PasskeyService.signInWithPasskey(
        preferImmediatelyAvailableCredentials = preferImmediatelyAvailableCredentials
      )
    }
  }

  @Deprecated("Kept for binary compatibility.", level = DeprecationLevel.HIDDEN)
  public suspend fun signInWithPasskey(): ClerkResult<SignIn, ClerkErrorResponse> =
    signInWithPasskey(preferImmediatelyAvailableCredentials = false)

  /**
   * Silently signs in with a Google Play restore credential transferred from another device.
   *
   * When no restore credential is available, the returned failure can be ignored and the app can
   * continue with its normal sign-in experience.
   */
  public suspend fun signInWithRestoreCredential(): ClerkResult<SignIn, ClerkErrorResponse> {
    return reportingFailures {
      RestoreCredentials.signIn()
    }
  }

  /**
   * Signs in with a locally enrolled biometric credential.
   *
   * The biometric-credential domain owns local credential selection, key access, challenge signing,
   * and stale local credential cleanup.
   *
   * @param id The biometric credential ID to use. When omitted, the available local credential is
   *   used.
   * @param identifierHint A local-only user identifier hint used to choose a matching credential.
   * @param promptTitle The title shown in the system authentication prompt.
   * @param promptSubtitle The subtitle shown in the system authentication prompt.
   * @return A [ClerkResult] containing the [SignIn] object on success, or a [ClerkErrorResponse] on
   *   failure.
   *
   * ### Example usage:
   * ```kotlin
   * val signIn = clerk.auth.signInWithBiometrics()
   * ```
   */
  public suspend fun signInWithBiometrics(
    id: String? = null,
    identifierHint: String? = null,
    promptTitle: String? = null,
    promptSubtitle: String? = null,
  ): ClerkResult<SignIn, ClerkErrorResponse> {
    return createSignIn(
      SignIn.CreateParams.Strategy.BiometricCredential(
        id = id,
        identifierHint = identifierHint,
        promptTitle = promptTitle,
        promptSubtitle = promptSubtitle,
      )
    )
  }

  /**
   * Signs in with Enterprise SSO.
   *
   * @param transferable Whether the flow may turn into a sign-up when the user has no Clerk account
   *   yet. Defaults to `true`.
   * @param block Builder block to configure the Enterprise SSO options.
   * @return A [ClerkResult] containing the [OAuthResult] on success, or a [ClerkErrorResponse] on
   *   failure.
   *
   * ### Example usage:
   * ```kotlin
   * val result = clerk.auth.signInWithEnterpriseSSO { email = "user@company.com" }
   * ```
   */
  public suspend fun signInWithEnterpriseSso(
    transferable: Boolean = true,
    block: EnterpriseSsoBuilder.() -> Unit,
  ): ClerkResult<OAuthResult, ClerkErrorResponse> {
    val builder = EnterpriseSsoBuilder().apply(block)
    builder.validate()

    return authenticateSignInWithRedirect(
      SignIn.AuthenticateWithRedirectParams.EnterpriseSSO(
        redirectUrl = builder.redirectUrl,
        emailAddress = builder.email,
      ),
      transferable = transferable,
    )
  }

  @Deprecated("Kept for binary compatibility.", level = DeprecationLevel.HIDDEN)
  public suspend fun signInWithEnterpriseSso(
    block: EnterpriseSsoBuilder.() -> Unit
  ): ClerkResult<OAuthResult, ClerkErrorResponse> =
    signInWithEnterpriseSso(transferable = true, block = block)

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
  public suspend fun signInWithTicket(ticket: String): ClerkResult<SignIn, ClerkErrorResponse> {
    return createSignIn(SignIn.CreateParams.Strategy.Ticket(ticket = ticket))
  }

  /**
   * Turns the current sign-up into a sign-in for its already existing account.
   *
   * The OAuth and Google One Tap flows do this on their own when `transferable` is `true`. Call it
   * after a flow that doesn't, such as [signUpWithIdToken], when the sign-up's external account
   * belongs to an existing user.
   *
   * @return A [ClerkResult] containing the [SignIn] object on success, or a [ClerkErrorResponse] on
   *   failure.
   */
  public suspend fun transferToSignIn(): ClerkResult<SignIn, ClerkErrorResponse> {
    return createSignIn(SignIn.CreateParams.Strategy.Transfer())
  }

  /**
   * Starts a native email-link sign-in flow secured by PKCE.
   *
   * The flow sends only a code challenge to Clerk and expects completion through a deep-link
   * callback carrying `flow_id` and `approval_token`.
   */
  public suspend fun startEmailLinkSignIn(
    email: String
  ): ClerkResult<SignIn, NativeMagicLinkError> {
    return nativeMagicLink.startEmailLinkSignIn(email)
  }

  /** Handles a native magic-link deep-link callback and completes the matching auth flow. */
  public suspend fun handleMagicLinkDeepLink(
    uri: Uri
  ): ClerkResult<NativeMagicLinkAuthResult, NativeMagicLinkError> {
    return nativeMagicLink.handleMagicLinkDeepLink(uri)
  }

  /** Completes a pending native magic-link flow using callback values from the deep link. */
  public suspend fun completeMagicLink(
    flowId: String,
    approvalToken: String,
  ): ClerkResult<NativeMagicLinkAuthResult, NativeMagicLinkError> {
    return nativeMagicLink.complete(flowId, approvalToken)
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
  public suspend fun signUp(
    block: SignUpBuilder.() -> Unit
  ): ClerkResult<SignUp, ClerkErrorResponse> {
    val builder = SignUpBuilder().apply(block)

    return createSignUp(
      SignUp.CreateParams.Standard(
        emailAddress = builder.email,
        phoneNumber = builder.phone,
        password = builder.password,
        firstName = builder.firstName,
        lastName = builder.lastName,
        username = builder.username,
        legalAccepted = builder.legalAccepted,
        unsafeMetadata = builder.unsafeMetadata,
      )
    )
  }

  /**
   * Signs up with OAuth provider.
   *
   * @param provider The OAuth provider to use for sign-up.
   * @param redirectUrl The native callback URL. Defaults to the callback registered by the SDK. A
   *   custom value needs an intent filter that routes it to `com.clerk.api.sso.SSOReceiverActivity`
   *   in the application manifest. [handle] only completes OAuth callbacks with a `clerk` scheme.
   * @param unsafeMetadata Custom metadata attached to the created user. Clerk does not validate it,
   *   so it must not hold sensitive information.
   * @return A [ClerkResult] containing the [OAuthResult] on success, or a [ClerkErrorResponse] on
   *   failure. The result holds a sign-in instead of a sign-up when the account already existed.
   *
   * ### Example usage:
   * ```kotlin
   * val result = clerk.auth.signUpWithOAuth(OAuthProvider.GOOGLE)
   * ```
   */
  public suspend fun signUpWithOAuth(
    provider: OAuthProvider,
    redirectUrl: String = RedirectConfiguration.DEFAULT_REDIRECT_URL,
    unsafeMetadata: Map<String, Any>? = null,
  ): ClerkResult<OAuthResult, ClerkErrorResponse> {
    return authenticateSignUpWithRedirect(
      SignUp.AuthenticateWithRedirectParams.OAuth(
        provider = provider,
        redirectUrl = redirectUrl,
        unsafeMetadata = unsafeMetadata,
      )
    )
  }

  @Deprecated("Kept for binary compatibility.", level = DeprecationLevel.HIDDEN)
  public suspend fun signUpWithOAuth(
    provider: OAuthProvider
  ): ClerkResult<OAuthResult, ClerkErrorResponse> =
    signUpWithOAuth(provider, redirectUrl = RedirectConfiguration.DEFAULT_REDIRECT_URL)

  /**
   * Signs up with Google One Tap.
   *
   * This native Google flow may resolve to either a sign-up or a sign-in, depending on whether the
   * selected Google account already exists in Clerk.
   *
   * @return A [ClerkResult] containing the [OAuthResult] on success, or a [ClerkErrorResponse] on
   *   failure.
   *
   * ### Example usage:
   * ```kotlin
   * val result = clerk.auth.signUpWithGoogleOneTap()
   * ```
   */
  public suspend fun signUpWithGoogleOneTap(): ClerkResult<OAuthResult, ClerkErrorResponse> {
    return signInWithGoogleOneTap(transferable = true)
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
  public suspend fun signUpWithIdToken(
    token: String,
    provider: IdTokenProvider,
    block: SignUpWithIdTokenBuilder.() -> Unit = {},
  ): ClerkResult<SignUp, ClerkErrorResponse> {
    val builder = SignUpWithIdTokenBuilder().apply(block)

    val strategy =
      when (provider) {
        IdTokenProvider.GOOGLE -> Strategy.GoogleOneTap.value
      }

    return postSignUp(
      buildMap {
        put("strategy", strategy)
        put("token", token)
        builder.firstName?.let { put("first_name", it) }
        builder.lastName?.let { put("last_name", it) }
      }
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
  public suspend fun signUpWithEnterpriseSso(
    block: EnterpriseSsoBuilder.() -> Unit
  ): ClerkResult<OAuthResult, ClerkErrorResponse> {
    val builder = EnterpriseSsoBuilder().apply(block)
    builder.validate()

    return authenticateSignUpWithRedirect(
      SignUp.AuthenticateWithRedirectParams.EnterpriseSSO(
        redirectUrl = builder.redirectUrl,
        emailAddress = builder.email,
      )
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
  public suspend fun signUpWithTicket(ticket: String): ClerkResult<SignUp, ClerkErrorResponse> {
    return createSignUp(SignUp.CreateParams.Ticket(ticket = ticket))
  }

  /**
   * Turns the current sign-in into a sign-up when its external account has no Clerk user yet.
   *
   * The OAuth and Google One Tap flows do this on their own when `transferable` is `true`. Call it
   * after a flow that doesn't, such as [signInWithIdToken].
   *
   * @return A [ClerkResult] containing the [SignUp] object on success, or a [ClerkErrorResponse] on
   *   failure.
   */
  public suspend fun transferToSignUp(): ClerkResult<SignUp, ClerkErrorResponse> {
    return createSignUp(SignUp.CreateParams.Transfer)
  }

  // endregion

  // region Session Management

  /**
   * Signs out all sessions or removes a specific session from the current client.
   *
   * @param sessionId Optional session ID to sign out. If null, signs out all sessions on this
   *   client.
   * @return A [ClerkResult] with Unit on success, or a [ClerkErrorResponse] on failure.
   *
   * ### Example usage:
   * ```kotlin
   * clerk.auth.signOut() // sign out all accounts
   * clerk.auth.signOut(sessionId = Clerk.session?.id) // sign out the current account only
   * ```
   */
  public suspend fun signOut(sessionId: String? = null): ClerkResult<Unit, ClerkErrorResponse> {
    return reportingFailures {
      if (sessionId != null) {
        when (val result = ClerkApi.session.removeSession(sessionId)) {
          is ClerkResult.Success -> {
            removeSessionLocally(sessionId)
            RestoreCredentials.clearSilently()
            refreshClientAfterSessionMutation()
            ClerkResult.success(Unit)
          }
          is ClerkResult.Failure -> ClerkResult.apiFailure(result.error)
        }
      } else {
        SignOutService.signOut()
      }
    }
  }

  private fun removeSessionLocally(sessionId: String) {
    Clerk.mutateClient { client ->
      val remainingSessions = client.sessions.filterNot { it.id == sessionId }
      val lastActiveSessionId =
        if (client.lastActiveSessionId == sessionId) {
          remainingSessions.firstOrNull { it.status == Session.SessionStatus.ACTIVE }?.id
            ?: remainingSessions.firstOrNull()?.id
        } else {
          client.lastActiveSessionId
        }
      client.copy(sessions = remainingSessions, lastActiveSessionId = lastActiveSessionId)
    }
  }

  private suspend fun refreshClientAfterSessionMutation(
    activeSessionFallbackId: String? = null,
    fallbackClient: Client? = null,
  ) {
    val updateCountAtStart = Clerk.clientUpdateCount
    when (val clientResult = Client.get()) {
      is ClerkResult.Success ->
        Clerk.updateClientIfUnchangedSince(
          expectedUpdateCount = updateCountAtStart,
          fetched =
            ClerkResult.success(
                clientResult.value.withActiveSessionFallback(
                  activeSessionFallbackId = activeSessionFallbackId,
                  fallbackClient = fallbackClient,
                )
              )
              .withTags(clientResult.tags),
        )
      is ClerkResult.Failure ->
        ClerkLog.w("Client refresh after session mutation failed: ${clientResult.errorMessage}")
    }
  }

  private fun Client.withActiveSessionFallback(
    activeSessionFallbackId: String?,
    fallbackClient: Client?,
  ): Client {
    if (activeSessionFallbackId == null) return this

    val fallbackSessions = fallbackClient?.sessions.orEmpty()
    val targetInFetchedSessions = sessions.any { it.id == activeSessionFallbackId }
    val fallbackTargetSession = fallbackSessions.firstOrNull { it.id == activeSessionFallbackId }
    val targetInFallbackSessions = fallbackTargetSession != null
    val sessionsWithFallbackTarget =
      if (targetInFetchedSessions && fallbackTargetSession != null) {
        sessions.map { session ->
          if (session.id == activeSessionFallbackId) {
            session.withSessionMutationFallback(fallbackTargetSession)
          } else {
            session
          }
        }
      } else {
        sessions
      }

    return when {
      // Fetched client is missing the target session entirely (e.g. cleared sessions list);
      // splice it back in from the fallback and force it active.
      !targetInFetchedSessions && targetInFallbackSessions -> {
        val missingFallbackSessions = fallbackSessions.filterNot { fallbackSession ->
          sessions.any { it.id == fallbackSession.id }
        }
        copy(
          sessions = sessions + missingFallbackSessions,
          lastActiveSessionId = activeSessionFallbackId,
        )
      }
      // Fetched client has the target session but `lastActiveSessionId` is stale
      // (read-after-write lag); force the just-activated session back to active.
      targetInFetchedSessions && lastActiveSessionId != activeSessionFallbackId ->
        copy(sessions = sessionsWithFallbackTarget, lastActiveSessionId = activeSessionFallbackId)
      targetInFetchedSessions && sessionsWithFallbackTarget != sessions ->
        copy(sessions = sessionsWithFallbackTarget)
      else -> this
    }
  }

  private fun Session.withSessionMutationFallback(fallbackSession: Session): Session =
    copy(
      user = user ?: fallbackSession.user,
      publicUserData = publicUserData ?: fallbackSession.publicUserData,
      lastActiveOrganizationId = fallbackSession.lastActiveOrganizationId,
      lastActiveToken = lastActiveToken ?: fallbackSession.lastActiveToken,
    )

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
  public suspend fun setActive(
    sessionId: String,
    organizationId: String? = null,
  ): ClerkResult<Session, ClerkErrorResponse> {
    val previousClient = if (Clerk.clientInitialized) Clerk.client else null
    if (organizationId.isNullOrBlank() && Clerk.organizationSelectionIsForced) {
      return Clerk.session?.let { ClerkResult.success(it) }
        ?: ClerkResult.unknownFailure(
          IllegalStateException("Cannot select a personal account without an active session.")
        )
    }

    val result =
      ClerkApi.client.setActive(
        sessionId = sessionId,
        organizationId = organizationId.orEmpty(),
        intent = SET_ACTIVE_INTENT_SELECT_ORG,
      )
    when (result) {
      is ClerkResult.Success -> {
        setActiveSessionLocally(
          sessionId,
          activeSession = result.value,
          sourceClient = previousClient,
          activeOrganizationId = organizationId,
        )
        refreshClientAfterSessionMutation(
          activeSessionFallbackId = sessionId,
          fallbackClient = if (Clerk.clientInitialized) Clerk.client else previousClient,
        )
      }
      is ClerkResult.Failure -> emitAuthError(result)
    }
    return result
  }

  private fun setActiveSessionLocally(
    sessionId: String,
    activeSession: Session? = null,
    sourceClient: Client? = null,
    activeOrganizationId: String? = null,
  ) {
    Clerk.mutateClient { current ->
      val client = current.takeIf { it.hasSession(sessionId) } ?: sourceClient ?: current
      val sessions = client.sessions.withUpdatedSession(activeSession, activeOrganizationId)
      if (sessions.none { it.id == sessionId }) null
      else client.copy(sessions = sessions, lastActiveSessionId = sessionId)
    }
  }

  private fun Client.hasSession(sessionId: String): Boolean = sessions.any { it.id == sessionId }

  private fun List<Session>.withUpdatedSession(
    activeSession: Session?,
    activeOrganizationId: String?,
  ): List<Session> {
    if (activeSession == null) return this

    var replacedSession = false
    val updatedSessions = map { existingSession ->
      if (existingSession.id == activeSession.id) {
        replacedSession = true
        activeSession.withSetActiveState(existingSession, activeOrganizationId)
      } else {
        existingSession
      }
    }

    return if (replacedSession) {
      updatedSessions
    } else {
      updatedSessions + activeSession.copy(lastActiveOrganizationId = activeOrganizationId)
    }
  }

  private fun Session.withSetActiveState(
    existingSession: Session,
    activeOrganizationId: String?,
  ): Session =
    copy(
      user = user ?: existingSession.user,
      publicUserData = publicUserData ?: existingSession.publicUserData,
      lastActiveOrganizationId = activeOrganizationId,
      lastActiveToken = lastActiveToken ?: existingSession.lastActiveToken,
    )

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
  public suspend fun getToken(
    options: GetTokenOptions? = null
  ): ClerkResult<String, ClerkErrorResponse> {
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
  public suspend fun revokeSession(session: Session): ClerkResult<Unit, ClerkErrorResponse> {
    return when (val result = session.revoke()) {
      is ClerkResult.Success -> ClerkResult.success(Unit)
      is ClerkResult.Failure -> ClerkResult.apiFailure(result.error)
    }
  }

  // endregion

  // region Deep Link Handling

  /**
   * Handles hosted auth, OAuth/SSO, and native magic-link deep link callbacks.
   *
   * Call this method from your Activity when receiving a deep link callback from Clerk
   * authentication flows.
   *
   * For hosted auth callbacks this method suspends until callback validation and session redemption
   * have finished. It returns before the redeemed session is activated, so [Clerk.session] may not
   * yet reflect the new session when this method returns `true`.
   *
   * @param uri The deep link URI received from the callback.
   * @return true if the URI was handled, false otherwise.
   *
   * ### Example usage:
   * ```kotlin
   * // In your Activity's onCreate or onNewIntent
   * lifecycleScope.launch {
   *   clerk.auth.handle(intent.data)
   * }
   * ```
   */
  public suspend fun handle(uri: Uri?): Boolean {
    val callbackUri = uri ?: return false
    val handledByMagicLink = canHandleNativeMagicLink(callbackUri)
    if (handledByMagicLink) {
      NativeMagicLinkService.handleMagicLinkDeepLink(callbackUri)
    }

    val handledByHostedAuth = !handledByMagicLink && HostedAuthService.complete(callbackUri) != null
    val isClerkCallback = callbackUri.scheme?.startsWith("clerk") == true
    if (!handledByMagicLink && !handledByHostedAuth && isClerkCallback) {
      SSOService.completeAuthenticateWithRedirect(callbackUri)
    }

    return handledByMagicLink || handledByHostedAuth || isClerkCallback
  }

  // endregion
}
