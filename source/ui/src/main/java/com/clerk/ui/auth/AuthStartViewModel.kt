package com.clerk.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clerk.api.Clerk
import com.clerk.api.credentials.resolvedCredentialFlowMessage
import com.clerk.api.credentials.shouldFallbackToOAuthFromGoogleOneTap
import com.clerk.api.credentials.shouldSuppressAutomaticCredentialFlowError
import com.clerk.api.credentials.shouldSuppressCredentialFlowError
import com.clerk.api.log.ClerkLog
import com.clerk.api.network.model.error.ClerkErrorResponse
import com.clerk.api.network.serialization.ClerkResult
import com.clerk.api.network.serialization.errorMessage
import com.clerk.api.network.serialization.onFailure
import com.clerk.api.network.serialization.onSuccess
import com.clerk.api.signin.SignIn
import com.clerk.api.signin.authenticateWithPreparedRedirect
import com.clerk.api.signin.prepareFirstFactor
import com.clerk.api.signin.startingFirstFactor
import com.clerk.api.signup.SignUp
import com.clerk.api.sso.OAuthProvider
import com.clerk.api.sso.ResultType
import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val FORM_IDENTIFIER_NOT_FOUND = "form_identifier_not_found"

private const val INVITATION_ACCOUNT_NOT_EXISTS = "invitation_account_not_exists"

/**
 * ViewModel responsible for handling the authentication logic for both sign-in and sign-up flows.
 *
 * This ViewModel manages the overall authentication state ([AuthState]) and interacts with the
 * Clerk SDK to perform operations like creating sign-in/sign-up attempts, and handling social
 * provider authentication.
 */
internal class AuthStartViewModel : ViewModel() {

  private val _state = MutableStateFlow<AuthState>(AuthState.Idle)
  /**
   * The current state of the authentication process, observed by the UI. See [AuthState] for
   * possible states.
   */
  val state: StateFlow<AuthState> = _state.asStateFlow()

  private var automaticPasskeySignInJob: Job? = null

  /**
   * Resets the current state back to [AuthState.Idle].
   *
   * This should be called by the UI after handling a terminal state (success or error) to avoid
   * re-triggering navigation/effects on recomposition or when navigating back.
   */
  internal fun resetState() {
    _state.value = AuthState.Idle
  }

  internal fun startAutomaticPasskeySignIn() {
    if (automaticPasskeySignInJob?.isActive == true) {
      ClerkLog.d("Automatic passkey sign-in already running; skipping duplicate start")
      return
    }

    ClerkLog.d("Starting automatic passkey sign-in")
    val job =
      viewModelScope.launch(Dispatchers.IO, start = CoroutineStart.LAZY) {
        try {
          when (
            val result =
              SignIn.create(
                SignIn.CreateParams.Strategy.Passkey(preferImmediatelyAvailableCredentials = true)
              )
          ) {
            is ClerkResult.Success -> {
              ClerkLog.d("Automatic passkey sign-in succeeded with status ${result.value.status}")
              if (isActive) handleSignInSuccess(result.value)
            }
            is ClerkResult.Failure -> {
              if (!isActive) return@launch
              if (result.shouldSuppressAutomaticCredentialFlowError) {
                ClerkLog.d(
                  "Automatic passkey sign-in finished without UI: " +
                    "${result.throwable?.javaClass?.simpleName}"
                )
                return@launch
              }
              ClerkLog.e(
                "Automatic passkey sign-in failed: ${result.resolvedCredentialFlowMessage}"
              )
              withContext(Dispatchers.Main) {
                _state.value = AuthState.Error(result.resolvedCredentialFlowMessage)
              }
            }
          }
        } finally {
          if (automaticPasskeySignInJob === coroutineContext[Job]) {
            automaticPasskeySignInJob = null
          }
        }
      }
    automaticPasskeySignInJob = job
    job.start()
  }

  internal fun cancelAutomaticPasskeySignIn() {
    val job = automaticPasskeySignInJob ?: return
    automaticPasskeySignInJob = null
    job.cancel()
  }

  /**
   * Initiates the authentication process based on the provided [authMode].
   *
   * @param authMode Determines whether to start a sign-in or sign-up flow. See [AuthMode].
   * @param isPhoneNumberFieldActive Indicates if the phone number input field is currently
   *   active/focused by the user.
   * @param phoneNumber The phone number entered by the user (if applicable).
   * @param identifier The primary identifier (email or username) entered by the user.
   */
  internal fun startAuth(
    authMode: AuthMode,
    isPhoneNumberFieldActive: Boolean,
    phoneNumber: String,
    identifier: String,
    unsafeMetadata: Map<String, Any>? = null,
  ) {
    cancelAutomaticPasskeySignIn()
    when (authMode) {
      AuthMode.SignIn ->
        signIn(
          isPhoneNumberFieldActive = isPhoneNumberFieldActive,
          phoneNumber = phoneNumber,
          identifier = identifier,
          transferable = authMode.transferable,
          unsafeMetadata = unsafeMetadata,
        )
      AuthMode.SignUp ->
        signUp(
          isPhoneNumberFieldActive = isPhoneNumberFieldActive,
          identifier = identifier,
          phoneNumber = phoneNumber,
          unsafeMetadata = unsafeMetadata,
        )
      AuthMode.SignInOrUp -> {
        signIn(
          isPhoneNumberFieldActive,
          phoneNumber,
          identifier,
          withSignUp = true,
          transferable = authMode.transferable,
          unsafeMetadata = unsafeMetadata,
        )
      }
    }
  }

  private fun signIn(
    isPhoneNumberFieldActive: Boolean,
    phoneNumber: String,
    identifier: String,
    withSignUp: Boolean = false,
    transferable: Boolean = true,
    unsafeMetadata: Map<String, Any>? = null,
  ) {
    viewModelScope.launch(Dispatchers.IO) {
      _state.value = AuthState.Loading
      val resolvedIdentifier = if (isPhoneNumberFieldActive) phoneNumber else identifier

      SignIn.create(SignIn.CreateParams.Strategy.Identifier(identifier = resolvedIdentifier))
        .onSuccess { signIn -> handleSignInSuccess(signIn, transferable) }
        .onFailure {
          if (withSignUp && it.error is ClerkErrorResponse) {
            val matchingCodes = listOf(FORM_IDENTIFIER_NOT_FOUND, INVITATION_ACCOUNT_NOT_EXISTS)
            val hasMatchingError = it.error?.errors?.any { it.code in matchingCodes } ?: false
            if (hasMatchingError) {
              signUp(isPhoneNumberFieldActive, identifier, phoneNumber, unsafeMetadata)
            } else {
              _state.value = AuthState.Error(it.errorMessage)
            }
          }
        }
    }
  }

  private fun signUp(
    isPhoneNumberFieldActive: Boolean,
    identifier: String,
    phoneNumber: String,
    unsafeMetadata: Map<String, Any>?,
  ) {
    _state.value = AuthState.Loading
    viewModelScope.launch(Dispatchers.IO) {
      SignUp.create(
          signUpParams(
            isPhoneNumberFieldActive = isPhoneNumberFieldActive,
            identifier = identifier,
            phoneNumber = phoneNumber,
            unsafeMetadata = unsafeMetadata,
          )
        )
        .onSuccess { signUp ->
          withContext(Dispatchers.Main) {
            _state.value = AuthState.Success.SignUpSuccess(signUp = signUp)
          }
        }
        .onFailure {
          withContext(Dispatchers.Main) { _state.value = AuthState.Error(it.errorMessage) }
        }
    }
  }

  /**
   * Initiates authentication using a specified social (OAuth) provider.
   *
   * If [preferGoogleOneTap] is true, Google One Tap is enabled, and the provider is Google, it
   * attempts Google One Tap authentication. Otherwise, it proceeds with the standard OAuth redirect
   * flow for the given provider.
   *
   * @param provider The [OAuthProvider] to authenticate with (e.g., Google, Facebook).
   * @param transferable Whether the flow can transfer between sign-in and sign-up.
   * @param preferGoogleOneTap Whether Google should prefer native One Tap over browser OAuth.
   * @param startOAuthWithSignUp Whether browser OAuth should create a sign-up attempt first.
   * @param unsafeMetadata Custom metadata to attach when browser OAuth starts from sign-up.
   */
  internal fun authenticateWithSocialProvider(
    provider: OAuthProvider,
    transferable: Boolean = true,
    preferGoogleOneTap: Boolean = true,
    startOAuthWithSignUp: Boolean = false,
    unsafeMetadata: Map<String, Any>? = null,
  ) {
    cancelAutomaticPasskeySignIn()
    _state.value = AuthState.OAuthState.Loading
    if (preferGoogleOneTap && provider == OAuthProvider.GOOGLE && Clerk.isGoogleOneTapEnabled) {
      handleGoogleOneTap(provider, transferable, startOAuthWithSignUp, unsafeMetadata)
    } else {
      authenticateWithOAuthProvider(provider, transferable, startOAuthWithSignUp, unsafeMetadata)
    }
  }

  private fun handleGoogleOneTap(
    provider: OAuthProvider,
    transferable: Boolean,
    startOAuthWithSignUp: Boolean,
    unsafeMetadata: Map<String, Any>?,
  ) {
    viewModelScope.launch(Dispatchers.IO) {
      SignIn.authenticateWithGoogleOneTap(transferable)
        .onSuccess {
          withContext(Dispatchers.Main) {
            _state.value =
              when (it.resultType) {
                ResultType.SIGN_IN -> {
                  it.signIn?.let { signIn -> AuthState.OAuthState.SignInSuccess(signIn = signIn) }
                    ?: AuthState.OAuthState.Error("Unknown result type from Google One Tap")
                }
                ResultType.SIGN_UP -> {
                  it.signUp?.let { signUp -> AuthState.OAuthState.SignUpSuccess(signUp = signUp) }
                    ?: AuthState.OAuthState.Error("Unknown result type from Google One Tap")
                }
                ResultType.UNKNOWN ->
                  AuthState.OAuthState.Error("Unknown result type from Google One Tap")
              }
          }
        }
        .onFailure {
          withContext(Dispatchers.Main) {
            when {
              it.shouldSuppressCredentialFlowError -> _state.value = AuthState.Idle
              it.shouldFallbackToOAuthFromGoogleOneTap ->
                authenticateWithOAuthProvider(
                  provider = provider,
                  transferable = transferable,
                  startOAuthWithSignUp = startOAuthWithSignUp,
                  unsafeMetadata = unsafeMetadata,
                )
              else -> _state.value = AuthState.OAuthState.Error(it.resolvedCredentialFlowMessage)
            }
          }
        }
    }
  }

  private fun authenticateWithOAuthProvider(
    provider: OAuthProvider,
    transferable: Boolean,
    startOAuthWithSignUp: Boolean,
    unsafeMetadata: Map<String, Any>?,
  ) {
    viewModelScope.launch {
      val result =
        if (startOAuthWithSignUp) {
          SignUp.authenticateWithRedirect(
            SignUp.AuthenticateWithRedirectParams.OAuth(
              provider = provider,
              unsafeMetadata = unsafeMetadata,
            )
          )
        } else {
          SignIn.authenticateWithRedirect(
            SignIn.AuthenticateWithRedirectParams.OAuth(provider = provider),
            transferable = transferable,
          )
        }

      result
        .onSuccess {
          _state.value =
            when (it.resultType) {
              ResultType.SIGN_IN -> {
                it.signIn?.let { signIn -> AuthState.OAuthState.SignInSuccess(signIn = signIn) }
                  ?: AuthState.OAuthState.Error("Unknown result type from OAuthProvider")
              }
              ResultType.SIGN_UP -> {
                it.signUp?.let { signUp -> AuthState.OAuthState.SignUpSuccess(signUp = signUp) }
                  ?: AuthState.OAuthState.Error("Unknown result type from OAuth Provider")
              }
              ResultType.UNKNOWN ->
                AuthState.OAuthState.Error("Unknown result type from OAuth provider")
            }
        }
        .onFailure { _state.value = AuthState.OAuthState.Error(it.errorMessage) }
    }
  }

  private fun signUpParams(
    isPhoneNumberFieldActive: Boolean,
    identifier: String,
    phoneNumber: String,
    unsafeMetadata: Map<String, Any>?,
  ): SignUp.CreateParams.Standard {
    return when {
      isPhoneNumberFieldActive ->
        SignUp.CreateParams.Standard(phoneNumber = phoneNumber, unsafeMetadata = unsafeMetadata)
      identifier.isEmailAddress ->
        SignUp.CreateParams.Standard(emailAddress = identifier, unsafeMetadata = unsafeMetadata)
      else -> SignUp.CreateParams.Standard(username = identifier, unsafeMetadata = unsafeMetadata)
    }
  }

  private suspend fun handleSignInSuccess(signIn: SignIn, transferable: Boolean = true) {
    when {
      signIn.requiresEnterpriseSSO() -> handleEnterpriseSSO(signIn, transferable)
      else -> {

        _state.value =
          withContext(Dispatchers.Main) { AuthState.Success.SignInSuccess(signIn = signIn) }
      }
    }
  }

  private suspend fun handleEnterpriseSSO(signIn: SignIn, transferable: Boolean) {
    signIn
      .prepareFirstFactor(SignIn.PrepareFirstFactorParams.EnterpriseSSO())
      .onSuccess { authenticateWithPreparedRedirect(it, transferable) }
      .onFailure { throwable ->
        _state.value = withContext(Dispatchers.Main) { AuthState.Error(throwable.errorMessage) }
      }
  }

  private suspend fun authenticateWithPreparedRedirect(signIn: SignIn, transferable: Boolean) {
    signIn
      .authenticateWithPreparedRedirect(transferable = transferable)
      .onSuccess {
        withContext(Dispatchers.Main) {
          val successType =
            when (it.resultType) {
              ResultType.SIGN_IN -> AuthState.Success.SignInSuccess(signIn = it.signIn)
              ResultType.SIGN_UP -> AuthState.Success.SignUpSuccess(signUp = it.signUp)
              ResultType.UNKNOWN -> {
                ClerkLog.e("Unknown result type after SSO redirect: ${it.resultType}")
                AuthState.Error("Unknown result type after SSO redirect")
              }
            }

          _state.value = successType
        }
      }
      .onFailure { failure ->
        withContext(Dispatchers.Main) { _state.value = AuthState.Error(failure.errorMessage) }
      }
  }

  /** Represents the various states of the authentication process. */
  internal sealed interface AuthState {
    /** The initial state before any authentication attempt has started. */
    object Idle : AuthState

    /** Indicates that an authentication operation is currently in progress. */
    object Loading : AuthState

    /**
     * Indicates that an authentication attempt was successful.
     *
     * @property signIn The [SignIn] object if the successful attempt was a sign-in.
     * @property signUp The [SignUp] object if the successful attempt was a sign-up.
     */
    sealed interface Success : AuthState {
      data class SignInSuccess(val signIn: SignIn?) : Success

      data class SignUpSuccess(val signUp: SignUp?) : Success
    }

    /**
     * Indicates that an authentication attempt failed.
     *
     * @property message A descriptive error message, if available.
     */
    data class Error(val message: String?) : AuthState

    /**
     * Represents the states specifically related to OAuth (social provider) authentication.
     * Inherits from [AuthState] as OAuth is a type of authentication.
     */
    sealed interface OAuthState : AuthState {
      data object Loading : OAuthState

      data class SignInSuccess(val signIn: SignIn) : AuthState

      data class SignUpSuccess(val signUp: SignUp) : AuthState

      data class Error(val message: String?) : AuthState
    }
  }
}

private fun SignIn.requiresEnterpriseSSO(): Boolean =
  startingFirstFactor?.strategy == "enterprise_sso"

private val emailRegex = Regex("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$")

private val String.isEmailAddress: Boolean
  get() = emailRegex.matches(this)
