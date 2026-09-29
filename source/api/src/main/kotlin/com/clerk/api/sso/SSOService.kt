package com.clerk.api.sso

import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.net.Uri
import androidx.core.net.toUri
import com.clerk.api.Clerk
import com.clerk.api.Constants.Strategy.ENTERPRISE_SSO
import com.clerk.api.externalaccount.ExternalAccount
import com.clerk.api.externalaccount.ExternalAccountService
import com.clerk.api.hostedauth.HOSTED_AUTH_CANCELLED_BY_NEW_FLOW
import com.clerk.api.hostedauth.HostedAuthService
import com.clerk.api.log.ClerkLog
import com.clerk.api.network.model.error.ClerkErrorResponse
import com.clerk.api.network.model.error.Error
import com.clerk.api.network.serialization.ClerkResult
import com.clerk.api.network.serialization.errorMessage
import com.clerk.api.signin.SignIn
import com.clerk.api.signin.get
import com.clerk.api.signin.prepareFirstFactor
import com.clerk.api.signup.SignUp
import com.clerk.api.signup.get
import com.clerk.api.signup.toUnsafeMetadataJsonString
import com.clerk.api.user.User.CreateExternalAccountParams
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred

@Suppress("TooManyFunctions")
internal object SSOService {
  private var currentPendingAuth:
    CompletableDeferred<ClerkResult<OAuthResult, ClerkErrorResponse>>? =
    null

  /**
   * Whether the current pending authentication flow allows transferring to a sign-up. When false, a
   * redirect callback that would normally trigger a sign-up transfer instead completes with an
   * error.
   */
  private var currentTransferable: Boolean = true

  private var currentRedirectFlow: RedirectFlow = RedirectFlow.SIGN_IN

  private var currentSignUp: SignUp? = null

  /**
   * Initiates an OAuth authentication flow with redirect to an external provider.
   *
   * This method handles redirect-based authentication flows by:
   * 1. Starting the authentication request with the specified strategy and redirect URL
   * 2. Launching the external provider's authentication page via [SSOReceiverActivity]
   * 3. Suspending until the user completes authentication and returns to the app
   * 4. Processing the authentication result and returning the appropriate [ClerkResult]
   *
   * The method automatically cancels any existing pending authentication to prevent conflicts.
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
    cancelCompetingAuthenticationFlows()
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
        when (
          val prepareResult =
            initialResult.value.prepareFirstFactor(
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
    return if (strategy == ENTERPRISE_SSO) {
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
    cancelCompetingAuthenticationFlows()

    val initialResult =
      SignUp.create(
        buildMap {
          strategy?.let { put("strategy", it) }
          put("redirect_url", redirectUrl)
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
    val context =
      Clerk.applicationContext?.get()
        ?: return ClerkResult.unknownFailure(
          IllegalStateException("Clerk must be initialized before starting redirect authentication")
        )
    cancelCompetingAuthenticationFlows()

    val completableDeferred = CompletableDeferred<ClerkResult<OAuthResult, ClerkErrorResponse>>()

    currentPendingAuth = completableDeferred
    currentTransferable = transferable
    currentRedirectFlow = redirectFlow
    currentSignUp = signUp

    val intent =
      SSOManagerActivity.createAuthorizationIntent(
          context = context,
          authorizationUri = externalVerificationRedirectUrl.toUri(),
        )
        .apply { addFlags(FLAG_ACTIVITY_NEW_TASK) }
    context.startActivity(intent)

    return completableDeferred.await()
  }

  /**
   * Completes the authentication flow initiated by [authenticateWithRedirect].
   *
   * This method is called when the user is redirected back to the app after completing external
   * authentication (e.g., OAuth or SSO provider). It processes the redirect URI to retrieve the
   * authentication result and resolves the pending authentication flow.
   *
   * The method handles two authentication scenarios:
   * 1. **Sign In**: When the URI contains a `rotating_token_nonce` parameter
   * 2. **Sign Up Transfer**: When the callback contains Clerk's explicit transfer marker
   *
   * This method is typically triggered internally via [SSOReceiverActivity] when the app receives a
   * redirect URI containing authentication results.
   *
   * @param uri The redirect URI received after completion of the external authentication flow.
   *   Expected to contain a `rotating_token_nonce` query parameter for sign-in flows.
   */
  @Suppress("TooGenericExceptionCaught")
  suspend fun completeAuthenticateWithRedirect(uri: Uri) {
    try {
      ClerkLog.d("Completing authentication with redirect: $uri")

      if (currentPendingAuth == null) {
        ClerkLog.w("No pending authentication found for redirect: $uri")
        return
      }

      val nonce = uri.getQueryParameter(ROTATING_TOKEN_NONCE)?.takeIf(String::isNotBlank)

      when (currentRedirectFlow) {
        RedirectFlow.SIGN_IN -> {
          if (nonce != null) {
            handleSignIn(nonce)
          } else if (uri.isTransferCallbackFor(RedirectFlow.SIGN_IN) && currentTransferable) {
            handleSignUpTransfer()
          } else if (uri.isTransferCallbackFor(RedirectFlow.SIGN_IN)) {
            ClerkLog.d("Sign-up transfer blocked: transferable is false")
            currentPendingAuth?.complete(
              ClerkResult.apiFailure(
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
            )
            clearCurrentAuth()
          } else {
            completeCancellation(uri)
          }
        }
        RedirectFlow.SIGN_UP -> {
          if (nonce != null) {
            handleSignUp(nonce)
          } else if (uri.isTransferCallbackFor(RedirectFlow.SIGN_UP)) {
            handleSignInTransfer()
          } else {
            completeCancellation(uri)
          }
        }
      }
    } catch (e: CancellationException) {
      cancelPendingAuthentication()
      throw e
    } catch (e: Exception) {
      ClerkLog.e("Error completing authentication with redirect: ${e.message}")
      currentPendingAuth?.complete(ClerkResult.unknownFailure(e))
      clearCurrentAuth()
    }
  }

  suspend fun connectExternalAccount(
    params: CreateExternalAccountParams
  ): ClerkResult<ExternalAccount, ClerkErrorResponse> {
    return ExternalAccountService.connectExternalAccount(params)
  }

  suspend fun completeExternalConnection() {
    ExternalAccountService.completeExternalConnection()
  }

  private suspend fun handleSignIn(nonce: String) {
    val signInResult =
      requireNotNull(Clerk.auth.currentSignIn).get(rotatingTokenNonce = nonce).signInToOAuthResult()
    currentPendingAuth?.complete(signInResult)

    clearCurrentAuth()
  }

  private suspend fun handleSignUpTransfer() {
    ClerkLog.d("Handling sign-up transfer")
    val createResult = SignUp.create(SignUp.CreateParams.Transfer).signUpToOAuthResult()
    currentPendingAuth?.complete(createResult)

    clearCurrentAuth()
  }

  private suspend fun handleSignUp(nonce: String) {
    val signUpResult = requireNotNull(currentSignUp ?: Clerk.auth.currentSignUp).get(nonce)
    currentPendingAuth?.complete(signUpResult.signUpToOAuthResult())

    clearCurrentAuth()
  }

  private suspend fun handleSignInTransfer() {
    ClerkLog.d("Handling sign-in transfer")
    val signUpResult = requireNotNull(currentSignUp ?: Clerk.auth.currentSignUp).get()
    currentPendingAuth?.complete(signUpResult.signUpToOAuthResultWithTransfer())

    clearCurrentAuth()
  }

  private fun completeCancellation(uri: Uri) {
    val reason =
      uri.getQueryParameter(ERROR_DESCRIPTION)
        ?: uri.getQueryParameter(ERROR)
        ?: uri.getQueryParameter(CLERK_ERROR_CODE)
        ?: AUTHENTICATION_CANCELLED
    ClerkLog.d("Redirect authentication cancelled")
    currentPendingAuth?.complete(ClerkResult.unknownFailure(SSOCancellationException(reason)))
    clearCurrentAuth()
  }

  private fun clearCurrentAuth() {
    currentPendingAuth = null
    currentTransferable = true
    currentRedirectFlow = RedirectFlow.SIGN_IN
    currentSignUp = null
  }

  private fun cancelCompetingAuthenticationFlows() {
    currentPendingAuth?.complete(
      ClerkResult.unknownFailure(
        SSOCancellationException("New authentication started, cancelling previous attempt")
      )
    )
    HostedAuthService.cancelPendingAuthentication(HOSTED_AUTH_CANCELLED_BY_NEW_FLOW)
    clearCurrentAuth()
  }

  fun cancelPendingAuthentication() {
    currentPendingAuth?.complete(
      ClerkResult.unknownFailure(SSOCancellationException(AUTHENTICATION_CANCELLED))
    )
    clearCurrentAuth()
  }

  fun hasPendingAuthentication(): Boolean {
    return currentPendingAuth != null
  }

  fun hasPendingExternalAccountConnection(): Boolean {
    return ExternalAccountService.hasPendingExternalAccountConnection()
  }

  internal enum class RedirectFlow {
    SIGN_IN,
    SIGN_UP,
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
