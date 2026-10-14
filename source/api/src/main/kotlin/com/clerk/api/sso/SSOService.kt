package com.clerk.api.sso

import android.content.Context
import android.net.Uri
import androidx.core.net.toUri
import com.clerk.api.Clerk
import com.clerk.api.auth.createSignUp
import com.clerk.api.auth.types.Strategy
import com.clerk.api.externalaccount.ExternalAccount
import com.clerk.api.externalaccount.ExternalAccountService
import com.clerk.api.log.ClerkLogger
import com.clerk.api.network.model.error.ClerkErrorResponse
import com.clerk.api.network.model.error.Error
import com.clerk.api.network.model.verification.Verification
import com.clerk.api.network.serialization.ClerkResult
import com.clerk.api.network.serialization.LocalFailureCodes
import com.clerk.api.network.serialization.errorMessage
import com.clerk.api.network.serialization.localFailure
import com.clerk.api.redirect.PendingRedirect
import com.clerk.api.redirect.PendingRedirect.RedirectFlow
import com.clerk.api.redirect.RedirectCoordinator
import com.clerk.api.redirect.RedirectState
import com.clerk.api.signin.SignIn
import com.clerk.api.signin.prepareFirstFactorImpl
import com.clerk.api.signin.reload
import com.clerk.api.signup.SignUp
import com.clerk.api.signup.get
import com.clerk.api.signup.toUnsafeMetadataJsonString
import com.clerk.api.user.User.CreateExternalAccountParams

@Suppress("TooManyFunctions")
internal object SSOService {
  /**
   * Initiates an OAuth authentication flow with redirect to an external provider.
   *
   * This method handles redirect-based authentication flows by:
   * 1. Starting the authentication request with the specified strategy and redirect URL
   * 2. Launching the external provider's authentication page via [SSOReceiverActivity]
   * 3. Suspending until the user completes authentication and returns to the app
   * 4. Processing the authentication result and returning the appropriate [ClerkResult]
   *
   * Any pending redirect flow (SSO, external account or hosted auth) is cancelled first.
   *
   * @param strategy The OAuth strategy to use for authentication (e.g., "oauth_google",
   *   "oauth_facebook")
   * @param redirectUrl The URL to redirect to after authentication completes
   * @param identifier Optional identifier for the authentication request
   * @param emailAddress Optional email address for the authentication request
   * @param legalAccepted Optional flag indicating if legal terms have been accepted
   * @param transferable Whether this authentication flow allows transferring to a sign-up if the
   *   user doesn't have an account. Defaults to `true`.
   * @return A [ClerkResult] containing the [OAuthResult] on success, or [ClerkErrorResponse] on
   *   failure
   */
  suspend fun authenticateWithRedirect(
    strategy: String? = null,
    redirectUrl: String = RedirectConfiguration.DEFAULT_REDIRECT_URL,
    identifier: String? = null,
    emailAddress: String? = null,
    legalAccepted: Boolean? = null,
    transferable: Boolean = true,
  ): ClerkResult<OAuthResult, ClerkErrorResponse> {
    RedirectCoordinator.supersedePending()
    val resolvedStrategy =
      strategy
        ?: return localFailure(
          code = LocalFailureCodes.INVALID_ARGUMENTS,
          longMessage = "Strategy cannot be null for redirect authentication",
        )

    val initialResult =
      SignIn.create(
        buildMap {
          put("strategy", resolvedStrategy)
          put("redirect_url", redirectUrl)
          put("locale", Clerk.locale.value.orEmpty())
          identifier?.let { put("identifier", it) }
          emailAddress?.let { put("email_address", it) }
          legalAccepted?.let { put("legal_accepted", it.toString()) }
        }
      )

    return when (initialResult) {
      is ClerkResult.Failure -> {
        val message = initialResult.errorMessage
        ClerkLogger.e("Failed to authenticate with redirect: $message")
        initialResult.signInToOAuthResult()
      }
      is ClerkResult.Success -> {
        ClerkLogger.d("Successfully created sign-in for redirect: $initialResult")
        when (
          val prepareResult =
            initialResult.value.prepareFirstFactorImpl(
              firstFactorParams(strategy = resolvedStrategy, redirectUrl = redirectUrl)
            )
        ) {
          is ClerkResult.Failure -> {
            val message = prepareResult.errorMessage
            ClerkLogger.e("Failed to prepare redirect first factor: $message")
            prepareResult.signInToOAuthResult()
          }
          is ClerkResult.Success -> {
            prepareResult.value.firstFactorVerification?.externalVerificationRedirectUrl?.let {
              externalUrl ->
              authenticateWithPreparedRedirect(externalUrl, transferable)
            } ?: missingExternalUrlFailure()
          }
        }
      }
    }
  }

  private fun missingExternalUrlFailure(): ClerkResult.Failure<ClerkErrorResponse> {
    ClerkLogger.e("Redirect authentication response is missing the external verification URL")
    return localFailure(
      code = LocalFailureCodes.MISSING_RESOURCE_DATA,
      longMessage = "Redirect authentication response is missing the external verification URL",
    )
  }

  private fun firstFactorParams(
    strategy: String,
    redirectUrl: String,
  ): SignIn.PrepareFirstFactorParams {
    return if (Strategy.from(strategy) == Strategy.EnterpriseSso) {
      SignIn.PrepareFirstFactorParams.EnterpriseSSO(redirectUrl = redirectUrl)
    } else {
      SignIn.PrepareFirstFactorParams.OAuth(strategy = strategy, redirectUrl = redirectUrl)
    }
  }

  suspend fun authenticateSignUpWithRedirect(
    strategy: String? = null,
    redirectUrl: String = RedirectConfiguration.DEFAULT_REDIRECT_URL,
    identifier: String? = null,
    emailAddress: String? = null,
    legalAccepted: Boolean? = null,
    unsafeMetadata: Map<String, Any>? = null,
  ): ClerkResult<OAuthResult, ClerkErrorResponse> {
    RedirectCoordinator.supersedePending()
    val state = RedirectState.generate()

    val initialResult =
      SignUp.create(
        buildMap {
          strategy?.let { put("strategy", it) }
          put("redirect_url", RedirectState.withState(redirectUrl, state))
          put("locale", Clerk.locale.value.orEmpty())
          identifier?.let { put("identifier", it) }
          emailAddress?.let { put("email_address", it) }
          legalAccepted?.let { put("legal_accepted", it.toString()) }
          unsafeMetadata?.let { put("unsafe_metadata", toUnsafeMetadataJsonString(it)) }
        }
      )

    return when (initialResult) {
      is ClerkResult.Failure -> {
        val message = initialResult.errorMessage
        ClerkLogger.e("Failed to authenticate sign-up with redirect: $message")
        initialResult.signUpToOAuthResult()
      }
      is ClerkResult.Success -> {
        val signUp = initialResult.value
        val externalUrl =
          signUp.verifications[EXTERNAL_ACCOUNT_VERIFICATION]?.externalVerificationRedirectUrl
            ?: return missingExternalUrlFailure()
        RedirectState.remember(externalUrl, state)

        authenticateWithPreparedRedirect(
          externalVerificationRedirectUrl = externalUrl,
          transferable = true,
          redirectFlow = RedirectFlow.SIGN_UP,
          signUp = signUp,
        )
      }
    }
  }

  /**
   * Continues an already prepared redirect flow.
   *
   * Some flows, like Enterprise SSO after `prepareFirstFactor`, already have an external provider
   * URL from the current sign-in attempt. In those cases we should launch that URL and wait for the
   * callback without creating a new sign-in redirect attempt.
   */
  suspend fun authenticateWithPreparedRedirect(
    externalVerificationRedirectUrl: String,
    transferable: Boolean = true,
    redirectFlow: RedirectFlow = RedirectFlow.SIGN_IN,
    signUp: SignUp? = null,
  ): ClerkResult<OAuthResult, ClerkErrorResponse> {
    val context = Clerk.applicationContext?.get()
    val expectedState = RedirectState.expectedFor(externalVerificationRedirectUrl)
    return when {
      context == null ->
        ClerkResult.unknownFailure(
          IllegalStateException("Clerk must be initialized before starting redirect authentication")
        )
      expectedState == null -> redirectNotPreparedFailure()
      else ->
        launchPreparedRedirect(
          context = context,
          externalVerificationRedirectUrl = externalVerificationRedirectUrl,
          pendingAuth =
            PendingRedirect.Sso(
              expectedState = expectedState,
              transferable = transferable,
              redirectFlow = redirectFlow,
              signUp = signUp,
            ),
        )
    }
  }

  private suspend fun launchPreparedRedirect(
    context: Context,
    externalVerificationRedirectUrl: String,
    pendingAuth: PendingRedirect.Sso,
  ): ClerkResult<OAuthResult, ClerkErrorResponse> {
    RedirectCoordinator.begin(pendingAuth)
    val result =
      RedirectCoordinator.launchAndAwait(
        redirect = pendingAuth,
        context = context,
        authorizationUri = externalVerificationRedirectUrl.toUri(),
      )
    if (result is ClerkResult.Success) RedirectState.forget(externalVerificationRedirectUrl)
    return result
  }

  private fun redirectNotPreparedFailure(): ClerkResult.Failure<ClerkErrorResponse> {
    ClerkLogger.e("Redirect was not prepared in this process; its callback cannot be verified")
    return localFailure(
      code = LocalFailureCodes.REDIRECT_NOT_PREPARED,
      longMessage =
        "This redirect was not prepared in this app session. Prepare the first factor again " +
          "before continuing the redirect.",
    )
  }

  /**
   * Completes [pendingAuth] from its redirect callback [uri]. [RedirectCoordinator] has already
   * checked the callback's state and runs this once per flow, in its process-wide scope, without
   * auth error reporting: the outcome is reported by the `authenticateWithRedirect` call awaiting
   * it.
   *
   * The method handles these authentication scenarios:
   * 1. **Sign In / Sign Up**: When the URI contains a `rotating_token_nonce` parameter
   * 2. **Transfer**: When the callback contains Clerk's explicit transfer marker
   * 3. **Bare callback**: When the callback carries no nonce, transfer marker, or provider error,
   *    the current sign-in or sign-up is reloaded and transferred if its external account
   *    verification is transferable. If the reload fails, that failure is returned.
   */
  internal suspend fun completeRedirect(pendingAuth: PendingRedirect.Sso, uri: Uri) {
    ClerkLogger.d("Completing authentication with redirect")
    val nonce = uri.getQueryParameter(ROTATING_TOKEN_NONCE)?.takeIf(String::isNotBlank)
    val result =
      when (pendingAuth.redirectFlow) {
        RedirectFlow.SIGN_IN -> completeSignInRedirect(uri, nonce, pendingAuth.transferable)
        RedirectFlow.SIGN_UP -> completeSignUpRedirect(uri, nonce, pendingAuth.signUp)
      }
    RedirectCoordinator.finish(pendingAuth, result)
  }

  suspend fun connectExternalAccount(
    params: CreateExternalAccountParams
  ): ClerkResult<ExternalAccount, ClerkErrorResponse> {
    return ExternalAccountService.connectExternalAccount(params)
  }

  private suspend fun completeSignInRedirect(
    uri: Uri,
    nonce: String?,
    transferable: Boolean,
  ): ClerkResult<OAuthResult, ClerkErrorResponse> =
    when {
      nonce != null -> handleSignIn(nonce)
      uri.isTransferCallbackFor(RedirectFlow.SIGN_IN) -> completeSignUpTransfer(transferable)
      uri.hasCallbackOutcome() -> cancellation(uri)
      else -> completeBareSignInCallback(uri, transferable)
    }

  private suspend fun completeSignUpRedirect(
    uri: Uri,
    nonce: String?,
    signUp: SignUp?,
  ): ClerkResult<OAuthResult, ClerkErrorResponse> =
    when {
      nonce != null -> handleSignUp(signUp, nonce)
      uri.isTransferCallbackFor(RedirectFlow.SIGN_UP) -> handleSignInTransfer(signUp)
      uri.hasCallbackOutcome() -> cancellation(uri)
      else -> completeBareSignUpCallback(uri, signUp)
    }

  /**
   * FAPI can return a bare callback (no nonce, no `__clerk_status`) when the external account has
   * no Clerk user yet. The sign-in's own state is then the source of truth, as in clerk-js.
   */
  private suspend fun completeBareSignInCallback(
    uri: Uri,
    transferable: Boolean,
  ): ClerkResult<OAuthResult, ClerkErrorResponse> {
    val signIn = Clerk.auth.currentSignIn ?: return cancellation(uri)
    return when (val reloaded = signIn.reload()) {
      is ClerkResult.Failure -> reloaded.signInToOAuthResult()
      is ClerkResult.Success ->
        if (reloaded.value.firstFactorVerification?.status == Verification.Status.TRANSFERABLE) {
          completeSignUpTransfer(transferable)
        } else {
          cancellation(uri)
        }
    }
  }

  private suspend fun completeBareSignUpCallback(
    uri: Uri,
    signUp: SignUp?,
  ): ClerkResult<OAuthResult, ClerkErrorResponse> {
    val currentSignUp = signUp ?: Clerk.auth.currentSignUp ?: return cancellation(uri)
    return when (val reloaded = currentSignUp.get()) {
      is ClerkResult.Failure -> reloaded.signUpToOAuthResult()
      is ClerkResult.Success ->
        if (
          reloaded.value.verifications[EXTERNAL_ACCOUNT_VERIFICATION]?.status ==
            Verification.Status.TRANSFERABLE
        ) {
          reloaded.signUpToOAuthResultWithTransfer()
        } else {
          cancellation(uri)
        }
    }
  }

  private suspend fun completeSignUpTransfer(
    transferable: Boolean
  ): ClerkResult<OAuthResult, ClerkErrorResponse> =
    if (transferable) handleSignUpTransfer() else transferBlocked()

  private suspend fun handleSignIn(nonce: String): ClerkResult<OAuthResult, ClerkErrorResponse> =
    requireNotNull(Clerk.auth.currentSignIn)
      .reload(rotatingTokenNonce = nonce)
      .signInToOAuthResult()

  private suspend fun handleSignUpTransfer(): ClerkResult<OAuthResult, ClerkErrorResponse> {
    ClerkLogger.d("Handling sign-up transfer")
    return Clerk.auth.createSignUp(SignUp.CreateParams.Transfer).signUpToOAuthResult()
  }

  private suspend fun handleSignUp(
    signUp: SignUp?,
    nonce: String,
  ): ClerkResult<OAuthResult, ClerkErrorResponse> =
    requireNotNull(signUp ?: Clerk.auth.currentSignUp).get(nonce).signUpToOAuthResult()

  private suspend fun handleSignInTransfer(
    signUp: SignUp?
  ): ClerkResult<OAuthResult, ClerkErrorResponse> {
    ClerkLogger.d("Handling sign-in transfer")
    return requireNotNull(signUp ?: Clerk.auth.currentSignUp)
      .get()
      .signUpToOAuthResultWithTransfer()
  }

  private fun transferBlocked(): ClerkResult<OAuthResult, ClerkErrorResponse> {
    ClerkLogger.d("Sign-up transfer blocked: transferable is false")
    return ClerkResult.apiFailure(
      ClerkErrorResponse(
        errors =
          listOf(
            Error(
              code = EXTERNAL_ACCOUNT_NOT_FOUND,
              message = "The External Account was not found.",
              longMessage = "The External Account was not found.",
            )
          )
      )
    )
  }

  private fun cancellation(uri: Uri): ClerkResult<OAuthResult, ClerkErrorResponse> {
    val reason =
      uri.getQueryParameter(ERROR_DESCRIPTION)
        ?: uri.getQueryParameter(ERROR)
        ?: uri.getQueryParameter(CLERK_ERROR_CODE)
        ?: AUTHENTICATION_CANCELLED
    ClerkLogger.d("Redirect authentication cancelled")
    return ClerkResult.unknownFailure(SSOCancellationException(reason))
  }

  private fun Uri.hasCallbackOutcome(): Boolean =
    listOf(ROTATING_TOKEN_NONCE, CLERK_STATUS, CLERK_ERROR_CODE, ERROR, ERROR_DESCRIPTION).any {
      !getQueryParameter(it).isNullOrBlank()
    }

  private fun Uri.isTransferCallbackFor(redirectFlow: RedirectFlow): Boolean {
    if (getQueryParameter(CLERK_STATUS) != CLERK_STATUS_FAILED) return false

    return when (redirectFlow) {
      RedirectFlow.SIGN_IN -> getQueryParameter(CLERK_ERROR_CODE) == EXTERNAL_ACCOUNT_NOT_FOUND
      RedirectFlow.SIGN_UP -> getQueryParameter(CLERK_ERROR_CODE) == EXTERNAL_ACCOUNT_EXISTS
    }
  }

  private const val AUTHENTICATION_CANCELLED = "Authentication cancelled"
  private const val ROTATING_TOKEN_NONCE = "rotating_token_nonce"
  private const val CLERK_STATUS = "__clerk_status"
  private const val CLERK_STATUS_FAILED = "failed"
  private const val CLERK_ERROR_CODE = "__clerk_error_code"
  private const val EXTERNAL_ACCOUNT_NOT_FOUND = "external_account_not_found"
  private const val EXTERNAL_ACCOUNT_EXISTS = "external_account_exists"
  private const val ERROR = "error"
  private const val EXTERNAL_ACCOUNT_VERIFICATION = "external_account"
  private const val ERROR_DESCRIPTION = "error_description"
}
