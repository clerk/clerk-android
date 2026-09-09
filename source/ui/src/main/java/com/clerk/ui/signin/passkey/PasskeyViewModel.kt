package com.clerk.ui.signin.passkey

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clerk.api.*
import com.clerk.ui.auth.*
import com.clerk.ui.core.common.displayMessage
import com.clerk.ui.core.common.runUiOperation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

internal class PasskeyViewModel(private val clerk: Clerk) : ViewModel() {
  private val _state = MutableStateFlow<AuthenticationViewState>(AuthenticationViewState.Idle)
  val state = _state.asStateFlow()

  fun authenticate(signIn: SignIn? = null) {
    _state.value = AuthenticationViewState.Loading
    val attempt = signIn ?: clerk.signIn
    viewModelScope.launch {
      runUiOperation {
          attempt.passkey(SignInPasskeyParams(flow = SignInPasskeyParamsFlow.Discoverable))
        }
        .onSuccess { _state.value = AuthenticationViewState.Success.SignIn(attempt) }
        .onFailure {
          _state.value =
            if (it.isSSOCancellation) AuthenticationViewState.Idle
            else AuthenticationViewState.Error(it.displayMessage)
        }
    }
  }

  fun resetState() {
    _state.value = AuthenticationViewState.Idle
  }
}
