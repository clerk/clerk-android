package com.clerk.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clerk.api.network.serialization.longErrorMessageOrNull
import com.clerk.api.network.serialization.onFailure
import com.clerk.api.network.serialization.onSuccess
import com.clerk.api.signin.SignIn
import com.clerk.api.signin.prepareFirstFactor
import com.clerk.api.signin.startingFirstFactor
import com.clerk.api.signup.SignUp
import com.clerk.api.sso.OAuthProvider
import com.clerk.api.sso.ResultType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal class AuthViewModel : ViewModel() {

  private val _state = MutableStateFlow<AuthState>(AuthState.Idle)
  val state = _state.asStateFlow()

  fun startAuth(
    authMode: AuthMode,
    isPhoneNumberFieldActive: Boolean,
    phoneNumber: String,
    identifier: String,
  ) {
    when (authMode) {
      AuthMode.SignIn ->
        signIn(
          isPhoneNumberFieldActive = isPhoneNumberFieldActive,
          phoneNumber = phoneNumber,
          identifier = identifier,
        )
      AuthMode.SignUp ->
        signUp(
          isPhoneNumberFieldActive = isPhoneNumberFieldActive,
          identifier = identifier,
          phoneNumber = phoneNumber,
        )
      AuthMode.SignInOrUp -> TODO()
    }
  }

  private fun signIn(isPhoneNumberFieldActive: Boolean, phoneNumber: String, identifier: String) {
    viewModelScope.launch(Dispatchers.IO) {
      _state.value = AuthState.Loading
      val resolvedIdentifier = if (isPhoneNumberFieldActive) phoneNumber else identifier

      SignIn.create(SignIn.CreateParams.Strategy.Identifier(identifier = resolvedIdentifier))
        .onSuccess { signIn -> handleSignInSuccess(signIn) }
        .onFailure { throwable -> _state.value = AuthState.Error(throwable.longErrorMessageOrNull) }
    }
  }

  private fun signUp(isPhoneNumberFieldActive: Boolean, identifier: String, phoneNumber: String) {
    _state.value = AuthState.Loading
    viewModelScope.launch(Dispatchers.IO) {
      SignUp.create(
          signUpParams(
            isPhoneNumberFieldActive = isPhoneNumberFieldActive,
            identifier = identifier,
            phoneNumber = phoneNumber,
          )
        )
        .onSuccess {
          withContext(Dispatchers.Main) { _state.value = AuthState.Success(signUp = it) }
        }
        .onFailure {
          withContext(Dispatchers.Main) {
            _state.value = AuthState.Error(it.longErrorMessageOrNull)
          }
        }
    }
  }

  fun authenticateWithSocialProvider(provider: OAuthProvider) {
    _state.value = AuthState.Loading
    viewModelScope.launch(Dispatchers.IO) {
      SignIn.authenticateWithRedirect(
          SignIn.AuthenticateWithRedirectParams.OAuth(provider = provider)
        )
        .onSuccess {
          withContext(Dispatchers.Main) {
            _state.value =
              when (it.resultType) {
                ResultType.SIGN_IN -> AuthState.Success(signIn = it.signIn)
                ResultType.SIGN_UP -> AuthState.Success(signUp = it.signUp)
                ResultType.UNKNOWN -> AuthState.Error("Unknown result type")
              }
          }
        }
        .onFailure {
          withContext(Dispatchers.Main) {
            _state.value = AuthState.Error(it.longErrorMessageOrNull)
          }
        }
    }
  }

  private fun signUpParams(
    isPhoneNumberFieldActive: Boolean,
    identifier: String,
    phoneNumber: String,
  ): SignUp.CreateParams.Standard {
    return when {
      isPhoneNumberFieldActive -> SignUp.CreateParams.Standard(phoneNumber = phoneNumber)
      identifier.isEmailAddress -> SignUp.CreateParams.Standard(emailAddress = identifier)
      else -> SignUp.CreateParams.Standard(username = identifier)
    }
  }

  private suspend fun handleSignInSuccess(signIn: SignIn) {
    when {
      signIn.requiresEnterpriseSSO() -> handleEnterpriseSSO(signIn)
      else -> _state.value = withContext(Dispatchers.Main) { AuthState.Success(signIn = signIn) }
    }
  }

  private suspend fun handleEnterpriseSSO(signIn: SignIn) {
    signIn
      .prepareFirstFactor(SignIn.PrepareFirstFactorParams.EnterpriseSSO())
      .onSuccess {
        val redirectUrl = signIn.getExternalVerificationRedirectUrl()
        authenticateWithEnterpriseSSO(redirectUrl)
      }
      .onFailure { throwable ->
        _state.value =
          withContext(Dispatchers.Main) { AuthState.Error(throwable.longErrorMessageOrNull) }
      }
  }

  private suspend fun authenticateWithEnterpriseSSO(redirectUrl: String) {
    SignIn.authenticateWithRedirect(
      SignIn.AuthenticateWithRedirectParams.EnterpriseSSO(redirectUrl = redirectUrl)
    )
    TODO("Need to understand `authenticateWithRedirect` from the iOS SDK")
  }

  sealed interface AuthState {
    object Idle : AuthState

    object Loading : AuthState

    data class Success(val signIn: SignIn? = null, val signUp: SignUp? = null) : AuthState

    data class Error(val message: String?) : AuthState
  }
}

private fun SignIn.requiresEnterpriseSSO(): Boolean =
  startingFirstFactor?.strategy == "enterprise_sso"

private fun SignIn.getExternalVerificationRedirectUrl(): String =
  firstFactorVerification?.externalVerificationRedirectUrl
    ?: error("External verification redirect URL is null")

private val emailRegex = Regex("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$")

private val String.isEmailAddress: Boolean
  get() = emailRegex.matches(this)
