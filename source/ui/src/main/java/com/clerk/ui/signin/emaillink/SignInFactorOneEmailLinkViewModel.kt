package com.clerk.ui.signin.emaillink

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clerk.api.*
import com.clerk.ui.auth.*
import com.clerk.ui.core.common.displayMessage
import com.clerk.ui.core.common.runUiOperation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

internal class SignInFactorOneEmailLinkViewModel(private val clerk: Clerk) : ViewModel() {
  private val _state = MutableStateFlow<AuthenticationViewState>(AuthenticationViewState.Idle)
  val state = _state.asStateFlow()
  private val attempt = clerk.signIn
  private val attemptId = attempt.id

  init {
    viewModelScope.launch { clerk.context.requireRuntime().changes.collect { syncCurrentState() } }
  }

  internal fun onHostResumed() {
    syncCurrentState()
  }

  fun sendLink(factor: FactorSelection) {
    if (attempt.id != attemptId || attemptId == null) {
      _state.value = AuthenticationViewState.NotStarted
      return
    }
    _state.value = AuthenticationViewState.Loading
    viewModelScope.launch {
      runUiOperation {
          attempt.emailLink.sendLink(
            SignInEmailLinkSendParams.Case2(
              SignInEmailLinkSendLinkParamsCase2(emailAddressId = factor.emailAddressId)
            )
          )
        }
        .onSuccess {
          if (_state.value is AuthenticationViewState.Loading)
            _state.value = AuthenticationViewState.Idle
        }
        .onFailure { _state.value = AuthenticationViewState.Error(it.displayMessage) }
    }
  }

  fun resetState() {
    _state.value = AuthenticationViewState.Idle
  }

  private fun syncCurrentState() {
    if (
      attempt.id == attemptId &&
        attempt.status in
          setOf(
            SignInStatus.Complete,
            SignInStatus.NeedsSecondFactor,
            SignInStatus.NeedsNewPassword,
            SignInStatus.NeedsClientTrust,
            SignInStatus.NeedsProtectCheck,
          )
    )
      _state.value = AuthenticationViewState.Success.SignIn(attempt)
  }
}
