package com.clerk.ui.signin.backupcode

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clerk.api.Clerk
import com.clerk.api.network.serialization.longErrorMessageOrNull
import com.clerk.api.network.serialization.onFailure
import com.clerk.api.network.serialization.onSuccess
import com.clerk.api.signin.SignIn
import com.clerk.api.signin.attemptSecondFactor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class BackupCodeViewModel : ViewModel() {
  private val _state = MutableStateFlow<AuthenticationState>(AuthenticationState.Idle)
  val state = _state.asStateFlow()

  fun submit(backupCode: String) {
    if (Clerk.signIn == null) {
      _state.value = AuthenticationState.Error("No sign-in in progress")
      return
    }
    _state.value = AuthenticationState.Verifying
    val inProgressSignIn = Clerk.signIn!!
    viewModelScope.launch {
      inProgressSignIn
        .attemptSecondFactor(SignIn.AttemptSecondFactorParams.BackupCode(backupCode))
        .onSuccess { _state.value = AuthenticationState.Success }
        .onFailure { _state.value = AuthenticationState.Error(it.longErrorMessageOrNull) }
    }
  }

  sealed interface AuthenticationState {
    object Idle : AuthenticationState

    object Verifying : AuthenticationState

    data object Success : AuthenticationState

    data class Error(val message: String?) : AuthenticationState
  }
}
