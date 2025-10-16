package com.clerk.ui.userprofile.security.passkey

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clerk.api.network.serialization.errorMessage
import com.clerk.api.network.serialization.onFailure
import com.clerk.api.network.serialization.onSuccess
import com.clerk.api.passkeys.Passkey
import com.clerk.api.passkeys.delete
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class UserProfilePasskeyViewModel : ViewModel() {

  private val _state = MutableStateFlow<State>(State.Idle)
  val state = _state.asStateFlow()

  fun deletePasskey(passkey: Passkey) {
    _state.value = State.Loading
    viewModelScope.launch {
      passkey
        .delete()
        .onFailure { _state.value = State.Error(it.errorMessage) }
        .onSuccess { _state.value = State.Success }
    }
  }

  sealed interface State {
    data object Idle : State

    data object Loading : State

    data class Error(val message: String) : State

    data object Success : State
  }
}
