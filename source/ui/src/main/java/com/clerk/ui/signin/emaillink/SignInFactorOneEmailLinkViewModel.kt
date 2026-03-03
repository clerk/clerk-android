package com.clerk.ui.signin.emaillink

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clerk.api.Clerk
import com.clerk.api.auth.AuthEvent
import com.clerk.api.network.serialization.onFailure
import com.clerk.api.network.serialization.onSuccess
import com.clerk.ui.auth.AuthenticationViewState
import com.clerk.ui.auth.guardSignIn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

internal class SignInFactorOneEmailLinkViewModel : ViewModel() {
  private val _state: MutableStateFlow<AuthenticationViewState> =
    MutableStateFlow(AuthenticationViewState.Idle)
  val state = _state.asStateFlow()

  init {
    viewModelScope.launch {
      Clerk.auth.events.collectLatest { event ->
        if (event is AuthEvent.SignInCompleted) {
          _state.value = AuthenticationViewState.Success.SignIn(event.signIn)
        }
      }
    }
  }

  fun sendLink() {
    _state.value = AuthenticationViewState.Loading

    guardSignIn(_state) { inProgressSignIn ->
      val identifier = inProgressSignIn.identifier
      if (identifier.isNullOrBlank() || !identifier.isEmailAddress()) {
        _state.value = AuthenticationViewState.Error(null)
        return@guardSignIn
      }

      viewModelScope.launch(Dispatchers.IO) {
        Clerk.auth
          .startEmailLinkSignIn(identifier)
          .onSuccess { _state.value = AuthenticationViewState.Idle }
          .onFailure { failure ->
            _state.value = AuthenticationViewState.Error(failure.error?.message)
          }
      }
    }
  }

  fun resetState() {
    if (_state.value !is AuthenticationViewState.Success.SignIn) {
      _state.value = AuthenticationViewState.Idle
    }
  }
}

private val emailRegex = Regex("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$")

private fun String.isEmailAddress(): Boolean = emailRegex.matches(this)
