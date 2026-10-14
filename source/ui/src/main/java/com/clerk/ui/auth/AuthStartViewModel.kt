package com.clerk.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clerk.api.Clerk
import com.clerk.api.auth.types.Strategy
import com.clerk.api.biometriccredential.BiometricCredentialKeyManagerException
import com.clerk.api.credentials.resolvedCredentialFlowMessage
import com.clerk.api.credentials.shouldFallbackToOAuthFromGoogleOneTap
import com.clerk.api.credentials.shouldSuppressAutomaticCredentialFlowError
import com.clerk.api.credentials.shouldSuppressCredentialFlowError
import com.clerk.api.network.serialization.ClerkResult
import com.clerk.api.network.serialization.errorMessage
import com.clerk.api.network.serialization.onFailure
import com.clerk.api.network.serialization.onSuccess
import com.clerk.api.signin.SignIn
import com.clerk.api.signin.authenticateWithEnterpriseSso
import com.clerk.api.signin.startingFirstFactor
import com.clerk.api.signup.SignUp
import com.clerk.api.sso.OAuthProvider
import com.clerk.api.sso.OAuthResult
import com.clerk.ui.core.extensions.isEmailAddress
import com.clerk.ui.core.log.ClerkLog
import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.CoroutineDispatcher
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

internal class AuthStartViewModel(private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO) :
  ViewModel() {

  private val _state = MutableStateFlow<AuthState>(AuthState.Idle)
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
      viewModelScope.launch(ioDispatcher, start = CoroutineStart.LAZY) {
        try {
          when (
            val result = Clerk.auth.signInWithPasskey(preferImmediatelyAvailableCredentials = true)
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
   * Signs in with the locally enrolled biometric credential.
   *
   * User-canceled biometric prompts reset the state silently instead of surfacing an error.
   */
  internal fun signInWithBiometrics(
    promptTitle: String? = null,
    promptSubtitle: String? = null,
  ) {
    cancelAutomaticPasskeySignIn()
    _state.value = AuthState.BiometricCredentialState.Loading
    viewModelScope.launch(Dispatchers.IO) {
      Clerk.biometricCredentials
        .signIn(promptTitle = promptTitle, promptSubtitle = promptSubtitle)
        .onSuccess { signIn ->
          withContext(Dispatchers.Main) {
            _state.value = AuthState.Success.SignInSuccess(signIn = signIn)
          }
        }
        .onFailure { failure ->
          withContext(Dispatchers.Main) {
            _state.value =
              if (failure.isBiometricCredentialCancellation) {
                AuthState.Idle
              } else {
                AuthState.Error(failure.errorMessage)
              }
          }
        }
    }
  }

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
    viewModelScope.launch(ioDispatcher) {
      _state.value = AuthState.Loading
      val resolvedIdentifier = if (isPhoneNumberFieldActive) phoneNumber else identifier

      Clerk.auth
        .signIn { this.identifier = resolvedIdentifier }
        .onSuccess { signIn -> handleSignInSuccess(signIn, transferable) }
        .onFailure {
          val matchingCodes = listOf(FORM_IDENTIFIER_NOT_FOUND, INVITATION_ACCOUNT_NOT_EXISTS)
          val shouldSignUp =
            withSignUp && it.error?.errors?.any { error -> error.code in matchingCodes } == true
          if (shouldSignUp) {
            signUp(isPhoneNumberFieldActive, identifier, phoneNumber, unsafeMetadata)
          } else {
            _state.value = AuthState.Error(it.errorMessage)
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
    viewModelScope.launch(ioDispatcher) {
      Clerk.auth
        .signUp {
          when {
            isPhoneNumberFieldActive -> phone = phoneNumber
            identifier.isEmailAddress -> email = identifier
            else -> username = identifier
          }
          this.unsafeMetadata = unsafeMetadata
        }
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
    viewModelScope.launch(ioDispatcher) {
      Clerk.auth
        .signInWithGoogleOneTap(transferable)
        .onSuccess {
          withContext(Dispatchers.Main) {
            _state.value =
              when (val outcome = it.outcome) {
                is OAuthResult.Outcome.SignIn ->
                  AuthState.OAuthState.SignInSuccess(signIn = outcome.signIn)
                is OAuthResult.Outcome.SignUp ->
                  AuthState.OAuthState.SignUpSuccess(signUp = outcome.signUp)
                OAuthResult.Outcome.Empty -> {
                  ClerkLog.e("Google One Tap returned neither a sign-in nor a sign-up")
                  AuthState.OAuthState.Error(null)
                }
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
          Clerk.auth.signUpWithOAuth(provider = provider, unsafeMetadata = unsafeMetadata)
        } else {
          Clerk.auth.signInWithOAuth(provider = provider, transferable = transferable)
        }

      result
        .onSuccess {
          _state.value =
            when (val outcome = it.outcome) {
              is OAuthResult.Outcome.SignIn ->
                AuthState.OAuthState.SignInSuccess(signIn = outcome.signIn)
              is OAuthResult.Outcome.SignUp ->
                AuthState.OAuthState.SignUpSuccess(signUp = outcome.signUp)
              OAuthResult.Outcome.Empty -> {
                ClerkLog.e("OAuth provider returned neither a sign-in nor a sign-up")
                AuthState.OAuthState.Error(null)
              }
            }
        }
        .onFailure {
          _state.value =
            if (it.isSSOCancellation) {
              AuthState.Idle
            } else {
              AuthState.OAuthState.Error(it.errorMessage)
            }
        }
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
      .authenticateWithEnterpriseSso(transferable = transferable)
      .onSuccess {
        withContext(Dispatchers.Main) {
          val successType =
            when (val outcome = it.outcome) {
              is OAuthResult.Outcome.SignIn -> AuthState.Success.SignInSuccess(outcome.signIn)
              is OAuthResult.Outcome.SignUp -> AuthState.Success.SignUpSuccess(outcome.signUp)
              OAuthResult.Outcome.Empty -> {
                ClerkLog.e("SSO redirect returned neither a sign-in nor a sign-up")
                AuthState.Error("Unknown result type after SSO redirect")
              }
            }

          _state.value = successType
        }
      }
      .onFailure { failure ->
        withContext(Dispatchers.Main) {
          _state.value =
            if (failure.isSSOCancellation) {
              AuthState.Idle
            } else {
              AuthState.Error(failure.errorMessage)
            }
        }
      }
  }

  internal sealed interface AuthState {
    object Idle : AuthState

    object Loading : AuthState

    sealed interface Success : AuthState {
      data class SignInSuccess(val signIn: SignIn) : Success

      data class SignUpSuccess(val signUp: SignUp) : Success
    }

    sealed interface BiometricCredentialState : AuthState {
      data object Loading : BiometricCredentialState
    }

    data class Error(val message: String?) : AuthState

    sealed interface OAuthState : AuthState {
      data object Loading : OAuthState

      data class SignInSuccess(val signIn: SignIn) : AuthState

      data class SignUpSuccess(val signUp: SignUp) : AuthState

      data class Error(val message: String?) : AuthState
    }
  }
}

private fun SignIn.requiresEnterpriseSSO(): Boolean =
  startingFirstFactor?.strategyType == Strategy.EnterpriseSso

internal val ClerkResult.Failure<*>.isBiometricCredentialCancellation: Boolean
  get() =
    (throwable as? BiometricCredentialKeyManagerException)?.code ==
      BiometricCredentialKeyManagerException.Code.BIOMETRIC_AUTHENTICATION_CANCELED
