package com.clerk.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clerk.api.signin.SignIn
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

internal class AuthViewModel : ViewModel() {

  private val _state = MutableStateFlow<AuthState>(AuthState.Idle)
  val state = _state.asStateFlow()

  fun startAuth(authMode: AuthMode) {
    when (authMode) {
      AuthMode.SignIn -> TODO()
      AuthMode.SignUp -> TODO()
      AuthMode.SignInOrUp -> TODO()
    }
  }

  fun signIn(isPhoneNumberFieldActive: Boolean, phoneNumber: String, identifier: String) {
    _state.value = AuthState.Loading
    val identifier = if (isPhoneNumberFieldActive) phoneNumber else identifier
    viewModelScope.launch {
      SignIn.create(SignIn.CreateParams.Strategy.Identifier(identifier = identifier))
    }
  }

  sealed interface AuthState {
    object Idle : AuthState

    object Loading : AuthState

    object Success : AuthState

    data class Error(val message: String?) : AuthState
  }
}
