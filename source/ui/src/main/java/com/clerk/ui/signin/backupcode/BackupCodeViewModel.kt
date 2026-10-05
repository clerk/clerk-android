package com.clerk.ui.signin.backupcode

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clerk.api.Clerk
import com.clerk.api.auth.types.MfaType
import com.clerk.api.network.serialization.errorMessage
import com.clerk.api.network.serialization.onFailure
import com.clerk.api.network.serialization.onSuccess
import com.clerk.api.signin.verifyMfaCode
import com.clerk.ui.auth.AuthenticationViewState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

internal class BackupCodeViewModel : ViewModel() {
  private val _state = MutableStateFlow<AuthenticationViewState>(AuthenticationViewState.Idle)
  val state = _state.asStateFlow()

  fun submit(backupCode: String) {
    if (Clerk.auth.currentSignIn == null) {
      _state.value = AuthenticationViewState.NotStarted
      return
    }
    _state.value = AuthenticationViewState.Loading
    val inProgressSignIn = Clerk.auth.currentSignIn!!
    viewModelScope.launch {
      inProgressSignIn
        .verifyMfaCode(backupCode, MfaType.BACKUP_CODE)
        .onSuccess { _state.value = AuthenticationViewState.Success.SignIn(it) }
        .onFailure { _state.value = AuthenticationViewState.Error(it.errorMessage) }
    }
  }

  fun resetState() {
    _state.value = AuthenticationViewState.Idle
  }
}
