package com.clerk.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clerk.api.network.serialization.longErrorMessageOrNull
import com.clerk.api.network.serialization.onFailure
import com.clerk.api.network.serialization.onSuccess
import com.clerk.api.signin.SignIn
import com.clerk.api.signin.prepareFirstFactor
import com.clerk.api.signin.startingFirstFactor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

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
      AuthMode.SignUp -> TODO()
      AuthMode.SignInOrUp -> TODO()
    }
  }

  private fun signIn(isPhoneNumberFieldActive: Boolean, phoneNumber: String, identifier: String) {
    executeWithState {
      val resolvedIdentifier = if (isPhoneNumberFieldActive) phoneNumber else identifier

      SignIn.create(SignIn.CreateParams.Strategy.Identifier(identifier = resolvedIdentifier))
        .onSuccess { signIn -> handleSignInSuccess(signIn) }
        .onFailure { throwable -> _state.value = AuthState.Error(throwable.longErrorMessageOrNull) }
    }
  }

  private suspend fun handleSignInSuccess(signIn: SignIn) {
    when {
      signIn.requiresEnterpriseSSO() -> handleEnterpriseSSO(signIn)
      else -> _state.value = AuthState.Success(signIn)
    }
  }

  private suspend fun handleEnterpriseSSO(signIn: SignIn) {
    signIn
      .prepareFirstFactor(SignIn.PrepareFirstFactorParams.EnterpriseSSO())
      .onSuccess {
        val redirectUrl = signIn.getExternalVerificationRedirectUrl()
        authenticateWithEnterpriseSSO(redirectUrl)
      }
      .onFailure { throwable -> _state.value = AuthState.Error(throwable.longErrorMessageOrNull) }
  }

  private suspend fun authenticateWithEnterpriseSSO(redirectUrl: String) {
    SignIn.authenticateWithRedirect(
      SignIn.AuthenticateWithRedirectParams.EnterpriseSSO(redirectUrl = redirectUrl)
    )
    TODO("Need to understand `authenticateWithRedirect` from the iOS SDK")
  }

  private fun executeWithState(block: suspend () -> Unit) {
    _state.value = AuthState.Loading
    viewModelScope.launch { block() }
  }

  sealed interface AuthState {
    object Idle : AuthState

    object Loading : AuthState

    data class Success(val signIn: SignIn) : AuthState

    data class Error(val message: String?) : AuthState
  }
}

private fun SignIn.requiresEnterpriseSSO(): Boolean =
  startingFirstFactor?.strategy == "enterprise_sso"

private fun SignIn.getExternalVerificationRedirectUrl(): String =
  firstFactorVerification?.externalVerificationRedirectUrl
    ?: error("External verification redirect URL is null")
