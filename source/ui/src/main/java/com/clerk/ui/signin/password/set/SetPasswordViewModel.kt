package com.clerk.ui.signin.password.set

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clerk.api.*
import com.clerk.ui.auth.*
import com.clerk.ui.core.common.displayMessage
import com.clerk.ui.core.common.runUiOperation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

internal class SetPasswordViewModel(private val clerk: Clerk) : ViewModel() {
  private val _state = MutableStateFlow<AuthenticationViewState>(AuthenticationViewState.Idle)
  val state = _state.asStateFlow()

  fun submitPassword(password: String) =
    guardSignIn(clerk, _state) { signIn ->
      _state.value = AuthenticationViewState.Loading
      viewModelScope.launch {
        runUiOperation {
            signIn.password(SignInPasswordParams.Case4(SignInPasswordParamsCase4(password)))
          }
          .onSuccess { _state.value = AuthenticationViewState.Success.SignIn(signIn) }
          .onFailure { _state.value = AuthenticationViewState.Error(it.displayMessage) }
      }
    }

  fun resetState() {
    _state.value = AuthenticationViewState.Idle
  }
}
