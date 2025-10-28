package com.clerk.ui.userprofile.security.passkey.rename

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clerk.api.network.serialization.errorMessage
import com.clerk.api.network.serialization.onFailure
import com.clerk.api.network.serialization.onSuccess
import com.clerk.api.passkeys.update
import com.clerk.ui.core.common.guardUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

internal class UserProfilePasskeyRenameViewModel : ViewModel() {

  private val _state = MutableStateFlow<State>(State.Idle)
  val state = _state.asStateFlow()

  fun renamePasskey(passkeyId: String) {
    guardUser(userDoesNotExist = { _state.value = State.Error("User does not exist") }) { user ->
      viewModelScope.launch {
        user.passkeys
          .find { it.id == passkeyId }
          ?.update()
          ?.onSuccess { _state.value = State.Success }
          ?.onFailure { _state.value = State.Error("Failed to rename passkey: ${it.errorMessage}") }
      }
    }
  }

  sealed interface State {
    data object Idle : State

    data object Loading : State

    data object Success : State

    data class Error(val message: String) : State
  }
}
