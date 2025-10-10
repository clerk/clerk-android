package com.clerk.ui.signin.backupcode

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clerk.api.Clerk
import com.clerk.api.network.serialization.errorMessage
import com.clerk.api.network.serialization.onFailure
import com.clerk.api.network.serialization.onSuccess
import com.clerk.api.signin.SignIn
import com.clerk.api.signin.attemptSecondFactor
import com.clerk.ui.auth.AuthenticationViewState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

internal class BackupCodeViewModel : ViewModel() {
  private val _state = MutableStateFlow<AuthenticationViewState>(AuthenticationViewState.Idle)
  val state = _state.asStateFlow()

  fun submit(backupCode: String) {
    if (Clerk.signIn == null) {
      _state.value = AuthenticationViewState.NotStarted
      return
    }
    _state.value = AuthenticationViewState.Loading
    val inProgressSignIn = Clerk.signIn!!
    viewModelScope.launch {
      inProgressSignIn
        .attemptSecondFactor(SignIn.AttemptSecondFactorParams.BackupCode(backupCode))
        .onSuccess { _state.value = AuthenticationViewState.Success.SignIn(it) }
        .onFailure { _state.value = AuthenticationViewState.Error(it.errorMessage) }
    }
  }

  fun resetState() {
    _state.value = AuthenticationViewState.Idle
  }
}
