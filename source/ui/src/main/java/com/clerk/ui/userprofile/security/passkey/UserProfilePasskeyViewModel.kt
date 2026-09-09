package com.clerk.ui.userprofile.security.passkey

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clerk.api.*
import com.clerk.ui.core.common.displayMessage
import com.clerk.ui.core.common.runUiOperation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

internal class UserProfilePasskeyViewModel(private val clerk: Clerk) : ViewModel() {

  private val _state = MutableStateFlow<State>(State.Idle)
  val state = _state.asStateFlow()

  fun deletePasskey(passkey: Passkey) {
    _state.value = State.Loading
    viewModelScope.launch {
      runUiOperation { passkey.delete() }
        .onSuccess { _state.value = State.Success }
        .onFailure { _state.value = State.Error(it.displayMessage) }
    }
  }

  fun createPasskey() {
    viewModelScope.launch {
      runUiOperation {
          val user = clerk.user ?: throw CoreException("no_user", "User does not exist")
          user.createPasskey()
        }
        .onSuccess { _state.value = State.Success }
        .onFailure { error ->
          _state.value =
            if (
              (error as? CoreException)?.let {
                it.kind == CoreFailureKind.Cancelled || it.code == "user_cancelled"
              } == true
            ) {
              State.Idle
            } else State.Error(error.displayMessage)
        }
    }
  }

  sealed interface State {
    data object Idle : State

    data object Loading : State

    data class Error(val message: String) : State

    data object Success : State
  }
}
