package com.clerk.ui.auth

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

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

  sealed interface AuthState {
    object Idle : AuthState

    object Loading : AuthState

    object Success : AuthState

    data class Error(val message: String?) : AuthState
  }
}
