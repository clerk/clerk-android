package com.clerk.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clerk.api.*
import com.clerk.ui.core.common.displayMessage
import com.clerk.ui.core.common.runUiOperation
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/** Presentation state for operations executed by the generated core. */
internal class AuthStartViewModel(private val clerk: Clerk) : ViewModel() {
  private val _state = MutableStateFlow<AuthState>(AuthState.Idle)
  val state: StateFlow<AuthState> = _state.asStateFlow()
  private var automaticPasskeySignInJob: Job? = null

  internal fun resetState() {
    _state.value = AuthState.Idle
  }

  internal fun startAutomaticPasskeySignIn() {
    if (automaticPasskeySignInJob?.isActive == true) return
    val job =
      viewModelScope.launch(start = CoroutineStart.LAZY) {
        try {
          runUiOperation {
              clerk.signIn.passkey(
                SignInPasskeyParams(
                  flow = SignInPasskeyParamsFlow.Discoverable,
                  preferImmediatelyAvailableCredentials = true,
                )
              )
            }
            .onSuccess {
              if (isActive) _state.value = AuthState.Success.SignInSuccess(clerk.signIn)
            }
            .onFailure { error ->
              val failure = error as? CoreException
              val suppressError =
                error.isSSOCancellation ||
                  failure?.passkeyStage == "requestingAuthorization" ||
                  failure?.code in
                    setOf(
                      "passkey_not_supported",
                      "capability_unavailable",
                      "presentation_unavailable",
                    )
              if (isActive && !suppressError) _state.value = AuthState.Error(error.displayMessage)
            }
        } finally {
          if (automaticPasskeySignInJob === coroutineContext[Job]) automaticPasskeySignInJob = null
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

  internal fun signInWithBiometrics(promptTitle: String? = null, promptSubtitle: String? = null) {
    cancelAutomaticPasskeySignIn()
    _state.value = AuthState.BiometricCredentialState.Loading
    viewModelScope.launch {
      runUiOperation {
          clerk.signIn.biometricCredential(
            SignInBiometricCredentialParams(reason = promptTitle, promptSubtitle = promptSubtitle)
          )
        }
        .onSuccess { _state.value = AuthState.Success.SignInSuccess(clerk.signIn) }
        .onFailure {
          _state.value =
            if (it.isSSOCancellation) AuthState.Idle else AuthState.Error(it.displayMessage)
        }
    }
  }

  internal fun startAuth(
    authMode: AuthMode,
    isPhoneNumberFieldActive: Boolean,
    phoneNumber: String,
    identifier: String,
    unsafeMetadata: JsonObject? = null,
  ) {
    cancelAutomaticPasskeySignIn()
    _state.value = AuthState.Loading
    viewModelScope.launch {
      runUiOperation {
          val result =
            clerk.startAuthentication(
              MobileIdentifierParams(
                identifier = if (isPhoneNumberFieldActive) phoneNumber else identifier,
                identifierType =
                  when {
                    isPhoneNumberFieldActive -> MobileIdentifierParamsIdentifierType.PhoneNumber
                    emailRegex.matches(identifier) ->
                      MobileIdentifierParamsIdentifierType.EmailAddress
                    else -> MobileIdentifierParamsIdentifierType.Username
                  },
                mode =
                  when (authMode) {
                    AuthMode.SignIn -> MobileIdentifierParamsMode.SignIn
                    AuthMode.SignUp -> MobileIdentifierParamsMode.SignUp
                    AuthMode.SignInOrUp -> MobileIdentifierParamsMode.SignInOrUp
                  },
                unsafeMetadata = unsafeMetadata,
              )
            )
          if (result is MobileAuthenticationResult.Case1) {
            val signIn = result.value.signIn
            val factor = signIn.startingFirstFactor(clerk)
            if (factor?.strategy == "enterprise_sso") {
              return@runUiOperation clerk.authenticateWithSSO(
                MobileSSOParams(
                  strategy = SignInSSOParamsStrategy.EnterpriseSso,
                  identifier = signIn.identifier,
                  enterpriseConnectionId = factor.enterpriseConnectionId,
                  start = MobileSSOParamsStart.SignIn,
                  transferable = authMode.transferable,
                  unsafeMetadata = unsafeMetadata,
                )
              )
            }
          }
          result
        }
        .onSuccess { result ->
          _state.value =
            when (result) {
              is MobileAuthenticationResult.Case1 ->
                AuthState.Success.SignInSuccess(result.value.signIn)
              is MobileAuthenticationResult.Case2 ->
                AuthState.Success.SignUpSuccess(result.value.signUp)
            }
        }
        .onFailure {
          _state.value =
            if (it.isSSOCancellation) AuthState.Idle else AuthState.Error(it.displayMessage)
        }
    }
  }

  internal fun authenticateWithSocialProvider(
    provider: OAuthProvider,
    transferable: Boolean = true,
    preferGoogleOneTap: Boolean = true,
    startOAuthWithSignUp: Boolean = false,
    unsafeMetadata: JsonObject? = null,
  ) {
    cancelAutomaticPasskeySignIn()
    _state.value = AuthState.OAuthState.Loading
    viewModelScope.launch {
      runUiOperation {
          clerk.authenticateWithSSO(
            MobileSSOParams(
              strategy =
                SignInSSOParamsStrategy.fromJson(
                  JsonPrimitive("oauth_${provider.rawValue}"),
                  clerk.context.requireRuntime(),
                ),
              start =
                if (startOAuthWithSignUp) MobileSSOParamsStart.SignUp
                else MobileSSOParamsStart.SignIn,
              transferable = transferable,
              unsafeMetadata = unsafeMetadata,
              preferGoogleOneTap = preferGoogleOneTap,
            )
          )
        }
        .onSuccess { result ->
          _state.value =
            when (result) {
              is MobileAuthenticationResult.Case1 ->
                AuthState.OAuthState.SignInSuccess(result.value.signIn)
              is MobileAuthenticationResult.Case2 ->
                AuthState.OAuthState.SignUpSuccess(result.value.signUp)
            }
        }
        .onFailure {
          _state.value =
            if (it.isSSOCancellation) AuthState.Idle
            else AuthState.OAuthState.Error(it.displayMessage)
        }
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

    /** States specific to biometric sign-in. */
    sealed interface BiometricCredentialState : AuthState {
      data object Loading : BiometricCredentialState
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

private val emailRegex = Regex("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$")
