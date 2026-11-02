package com.clerk.ui.signup.emaillink

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clerk.api.Clerk
import com.clerk.api.auth.AuthEvent
import com.clerk.api.network.serialization.errorMessage
import com.clerk.api.network.serialization.onFailure
import com.clerk.api.network.serialization.onSuccess
import com.clerk.api.signup.SignUp
import com.clerk.api.signup.sendEmailLink
import com.clerk.ui.auth.AuthenticationViewState
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

internal class SignUpEmailLinkViewModel(
  private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : ViewModel() {
  private val _state = MutableStateFlow<AuthenticationViewState>(AuthenticationViewState.Idle)
  val state = _state.asStateFlow()
  @Volatile private var linkSent = false
  private val sendsInFlight = AtomicInteger(0)

  init {
    syncCurrentSignUpState()
    viewModelScope.launch {
      Clerk.auth.events.collectLatest { event ->
        when (event) {
          is AuthEvent.SignUpStarted -> {
            if (event.signUp.status == SignUp.Status.MISSING_REQUIREMENTS) {
              _state.value = AuthenticationViewState.Success.SignUp(event.signUp)
            }
          }
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

  internal fun onHostResumed() {
    syncCurrentSignUpState()
  }

  fun sendInitialLink() {
    if (linkSent || sendsInFlight.get() > 0) return
    sendLink()
  }

  fun sendLink() {
    _state.value = AuthenticationViewState.Loading

    val signUp = Clerk.auth.currentSignUp
    if (signUp == null || signUp.emailAddress.isNullOrBlank()) {
      _state.value = AuthenticationViewState.Error(null)
      return
    }

    sendsInFlight.incrementAndGet()
    viewModelScope.launch(ioDispatcher) {
      signUp
        .sendEmailLink()
        .onSuccess {
          linkSent = true
          _state.value = AuthenticationViewState.Idle
        }
        .onFailure { failure -> _state.value = AuthenticationViewState.Error(failure.errorMessage) }
      sendsInFlight.decrementAndGet()
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

  private fun syncCurrentSignUpState() {
    val currentSignUp = Clerk.auth.currentSignUp ?: return
    when (currentSignUp.status) {
      SignUp.Status.MISSING_REQUIREMENTS,
      SignUp.Status.COMPLETE -> {
        _state.value = AuthenticationViewState.Success.SignUp(currentSignUp)
      }
      else -> Unit
    }
  }
}
