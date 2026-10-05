package com.clerk.api.sso

import android.net.Uri
import androidx.core.net.toUri
import com.clerk.api.Clerk
import com.clerk.api.auth.createSignUp
import com.clerk.api.auth.types.Strategy
import com.clerk.api.externalaccount.ExternalAccount
import com.clerk.api.externalaccount.ExternalAccountService
import com.clerk.api.log.ClerkLog
import com.clerk.api.network.model.error.ClerkErrorResponse
import com.clerk.api.network.model.error.Error
import com.clerk.api.network.serialization.ClerkResult
import com.clerk.api.network.serialization.errorMessage
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

/**
 * OAuth and enterprise SSO redirect flows. The pending flow is a [PendingRedirect.Sso] held by
 * [RedirectCoordinator], which also routes the callback back here.
 */
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
        ?: return ClerkResult.unknownFailure(
          Exception("Strategy cannot be null for redirect authentication")
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
        ClerkLog.e("Failed to authenticate with redirect: $message")
        initialResult.signInToOAuthResult()
      }
      is ClerkResult.Success -> {
        ClerkLog.d("Successfully created sign-in for redirect: $initialResult")
        // prepareFirstFactorImpl adds the flow's state to the redirect URL and remembers it for the
        // external URL it returns; authenticateWithPreparedRedirect picks it up from there.
        when (
          val prepareResult =
            initialResult.value.prepareFirstFactorImpl(
              firstFactorParams(strategy = resolvedStrategy, redirectUrl = redirectUrl)
            )
        ) {
          is ClerkResult.Failure -> {
            val message = prepareResult.errorMessage
            ClerkLog.e("Failed to prepare redirect first factor: $message")
            prepareResult.signInToOAuthResult()
          }
          is ClerkResult.Success -> {
            val externalUrl =
              requireNotNull(
                prepareResult.value.firstFactorVerification?.externalVerificationRedirectUrl
              ) {
                "External URL cannot be null"
              }

            authenticateWithPreparedRedirect(externalUrl, transferable)
          }
        }
      }
    }
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
        ClerkLog.e("Failed to authenticate sign-up with redirect: $message")
        initialResult.signUpToOAuthResult()
      }
      is ClerkResult.Success -> {
        val signUp = initialResult.value
        val externalUrl =
          requireNotNull(
            signUp.verifications["external_account"]?.externalVerificationRedirectUrl
          ) {
            "External URL cannot be null"
          }
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
   *
   * The callback must carry the state that was added to the redirect URL when the redirect was
   * prepared (see [RedirectState]). A redirect prepared without one accepts any callback.
   */
  suspend fun authenticateWithPreparedRedirect(
    externalVerificationRedirectUrl: String,
    transferable: Boolean = true,
    redirectFlow: RedirectFlow = RedirectFlow.SIGN_IN,
    signUp: SignUp? = null,
  ): ClerkResult<OAuthResult, ClerkErrorResponse> {
    val expectedState = RedirectState.take(externalVerificationRedirectUrl)
    val context =
      Clerk.applicationContext?.get()
        ?: return ClerkResult.unknownFailure(
          IllegalStateException("Clerk must be initialized before starting redirect authentication")
        )
    if (expectedState == null) {
      ClerkLog.w("Redirect was prepared without a state; its callback cannot be verified")
    }

    val pendingAuth =
      PendingRedirect.Sso(
        expectedState = expectedState,
        transferable = transferable,
        redirectFlow = redirectFlow,
        signUp = signUp,
      )
    RedirectCoordinator.begin(pendingAuth)
    return RedirectCoordinator.launchAndAwait(
      redirect = pendingAuth,
      context = context,
      authorizationUri = externalVerificationRedirectUrl.toUri(),
    )
  }

  /**
   * Completes [pendingAuth] from its redirect callback [uri]. [RedirectCoordinator] has already
   * checked the callback's state and runs this once per flow, in its process-wide scope, without
   * auth error reporting: the outcome is reported by the `authenticateWithRedirect` call awaiting
   * it.
   *
   * The method handles two authentication scenarios:
   * 1. **Sign In**: When the URI contains a `rotating_token_nonce` parameter
   * 2. **Sign Up Transfer**: When the callback contains Clerk's explicit transfer marker
   */
  internal suspend fun completeRedirect(pendingAuth: PendingRedirect.Sso, uri: Uri) {
    ClerkLog.d("Completing authentication with redirect")
    val nonce = uri.getQueryParameter(ROTATING_TOKEN_NONCE)?.takeIf(String::isNotBlank)
    val result =
      when (pendingAuth.redirectFlow) {
        RedirectFlow.SIGN_IN ->
          when {
            nonce != null -> handleSignIn(nonce)
            uri.isTransferCallbackFor(RedirectFlow.SIGN_IN) && pendingAuth.transferable ->
              handleSignUpTransfer()
            uri.isTransferCallbackFor(RedirectFlow.SIGN_IN) -> transferBlocked()
            else -> cancellation(uri)
          }
        RedirectFlow.SIGN_UP ->
          when {
            nonce != null -> handleSignUp(pendingAuth.signUp, nonce)
            uri.isTransferCallbackFor(RedirectFlow.SIGN_UP) ->
              handleSignInTransfer(pendingAuth.signUp)
            else -> cancellation(uri)
          }
      }
    RedirectCoordinator.finish(pendingAuth, result)
  }

  suspend fun connectExternalAccount(
    params: CreateExternalAccountParams
  ): ClerkResult<ExternalAccount, ClerkErrorResponse> {
    return ExternalAccountService.connectExternalAccount(params)
  }

  private suspend fun handleSignIn(nonce: String): ClerkResult<OAuthResult, ClerkErrorResponse> =
    requireNotNull(Clerk.auth.currentSignIn)
      .reload(rotatingTokenNonce = nonce)
      .signInToOAuthResult()

  private suspend fun handleSignUpTransfer(): ClerkResult<OAuthResult, ClerkErrorResponse> {
    ClerkLog.d("Handling sign-up transfer")
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
    ClerkLog.d("Handling sign-in transfer")
    return requireNotNull(signUp ?: Clerk.auth.currentSignUp)
      .get()
      .signUpToOAuthResultWithTransfer()
  }

  private fun transferBlocked(): ClerkResult<OAuthResult, ClerkErrorResponse> {
    ClerkLog.d("Sign-up transfer blocked: transferable is false")
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
    ClerkLog.d("Redirect authentication cancelled")
    return ClerkResult.unknownFailure(SSOCancellationException(reason))
  }

  fun cancelPendingAuthentication() {
    RedirectCoordinator.cancelPending { it is PendingRedirect.Sso }
  }

  fun hasPendingAuthentication(): Boolean = RedirectCoordinator.current() is PendingRedirect.Sso

  fun hasPendingExternalAccountConnection(): Boolean {
    return ExternalAccountService.hasPendingExternalAccountConnection()
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
  private const val ERROR_DESCRIPTION = "error_description"
}
