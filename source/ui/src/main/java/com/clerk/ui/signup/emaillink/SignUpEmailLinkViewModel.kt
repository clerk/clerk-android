package com.clerk.ui.signup.emaillink

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clerk.api.Clerk
import com.clerk.api.auth.AuthEvent
import com.clerk.api.network.serialization.errorMessage
import com.clerk.api.network.serialization.onFailure
import com.clerk.api.network.serialization.onSuccess
import com.clerk.api.signup.SignUp
import com.clerk.api.signup.prepareVerification
import com.clerk.ui.auth.AuthenticationViewState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

internal class SignUpEmailLinkViewModel : ViewModel() {
  private val _state = MutableStateFlow<AuthenticationViewState>(AuthenticationViewState.Idle)
  val state = _state.asStateFlow()

  init {
    viewModelScope.launch {
      Clerk.auth.events.collectLatest { event ->
        when (event) {
          is AuthEvent.SignUpCompleted -> {
            _state.value = AuthenticationViewState.Success.SignUp(event.signUp)
          }
          is AuthEvent.SignInCompleted -> {
            _state.value = AuthenticationViewState.Success.SignIn(event.signIn)
          }
          else -> Unit
        }
      }
    }
  }

  fun sendLink() {
    _state.value = AuthenticationViewState.Loading

    val signUp = Clerk.client.signUp
    if (signUp == null || signUp.emailAddress.isNullOrBlank()) {
      _state.value = AuthenticationViewState.Error(null)
      return
    }

    viewModelScope.launch(Dispatchers.IO) {
      signUp
        .prepareVerification(SignUp.PrepareVerificationParams.Strategy.EmailLink())
        .onSuccess { _state.value = AuthenticationViewState.Idle }
        .onFailure { failure -> _state.value = AuthenticationViewState.Error(failure.errorMessage) }
    }
  }

  fun resetState() {
    if (
      _state.value !is AuthenticationViewState.Success.SignUp &&
        _state.value !is AuthenticationViewState.Success.SignIn
    ) {
      _state.value = AuthenticationViewState.Idle
    }
  }
}
