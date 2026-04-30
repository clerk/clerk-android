package com.clerk.ui.signin.emaillink

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clerk.api.Clerk
import com.clerk.api.auth.AuthEvent
import com.clerk.api.network.serialization.onFailure
import com.clerk.api.network.serialization.onSuccess
import com.clerk.api.signin.SignIn
import com.clerk.ui.auth.AuthenticationViewState
import com.clerk.ui.auth.guardSignIn
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

internal class SignInFactorOneEmailLinkViewModel(
  private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : ViewModel() {
  private val _state: MutableStateFlow<AuthenticationViewState> =
    MutableStateFlow(AuthenticationViewState.Idle)
  val state = _state.asStateFlow()

  init {
    syncCurrentSignInState()
    viewModelScope.launch {
      Clerk.auth.events.collectLatest { event ->
        when (event) {
          is AuthEvent.SignInCompleted -> {
            _state.value = AuthenticationViewState.Success.SignIn(event.signIn)
          }
          is AuthEvent.SignInStarted -> {
            if (event.signIn.status == SignIn.Status.NEEDS_SECOND_FACTOR) {
              _state.value = AuthenticationViewState.Success.SignIn(event.signIn)
            }
          }
          else -> Unit
        }
      }
    }
  }

  internal fun onHostResumed() {
    syncCurrentSignInState()
  }

  fun sendLink() {
    _state.value = AuthenticationViewState.Loading

    guardSignIn(_state) { inProgressSignIn ->
      val identifier = inProgressSignIn.identifier
      if (identifier.isNullOrBlank() || !identifier.isEmailAddress()) {
        _state.value = AuthenticationViewState.Error(null)
        return@guardSignIn
      }

      viewModelScope.launch(ioDispatcher) {
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

  private fun syncCurrentSignInState() {
    val currentSignIn = Clerk.auth.currentSignIn ?: return
    when (currentSignIn.status) {
      SignIn.Status.COMPLETE,
      SignIn.Status.NEEDS_SECOND_FACTOR,
      SignIn.Status.NEEDS_NEW_PASSWORD,
      SignIn.Status.NEEDS_CLIENT_TRUST -> {
        _state.value = AuthenticationViewState.Success.SignIn(currentSignIn)
      }
      else -> Unit
    }
  }
}

private val emailRegex = Regex("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$")

private fun String.isEmailAddress(): Boolean = emailRegex.matches(this)
