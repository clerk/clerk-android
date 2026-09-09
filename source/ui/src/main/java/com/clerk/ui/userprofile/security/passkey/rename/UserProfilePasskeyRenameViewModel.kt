package com.clerk.ui.userprofile.security.passkey.rename

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clerk.api.*
import com.clerk.ui.core.common.displayMessage
import com.clerk.ui.core.common.runUiOperation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

internal class UserProfilePasskeyRenameViewModel(private val clerk: Clerk) : ViewModel() {

  private val _state = MutableStateFlow<State>(State.Idle)
  val state = _state.asStateFlow()

  fun renamePasskey(passkeyId: String, newName: String) {
    _state.value = State.Loading
    viewModelScope.launch {
      runUiOperation {
          val user = clerk.user ?: throw CoreException("no_user", "User does not exist")
          val passkey =
            user.passkeys.find { it.id == passkeyId }
              ?: throw CoreException("passkey_not_found", "This passkey is no longer available.")
          passkey.update(Partialtype(name = Field.Value(newName)))
        }
        .onSuccess { _state.value = State.Success }
        .onFailure { _state.value = State.Error("Failed to rename passkey: ${it.displayMessage}") }
    }
  }

  fun resetState() {
    _state.value = State.Idle
  }

  sealed interface State {
    data object Idle : State

    data object Loading : State

    data object Success : State

    data class Error(val message: String) : State
  }
}
