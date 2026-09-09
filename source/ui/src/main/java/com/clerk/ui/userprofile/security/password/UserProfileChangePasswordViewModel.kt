package com.clerk.ui.userprofile.security.password

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clerk.api.*
import com.clerk.ui.core.common.displayMessage
import com.clerk.ui.core.common.runUiOperation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

internal class UserProfileChangePasswordViewModel(private val clerk: Clerk) : ViewModel() {

  private val _state = MutableStateFlow<State>(State.Idle)
  val state = _state.asStateFlow()

  fun resetPassword(
    currentPassword: String?,
    newPassword: String,
    signOutOfOtherSessions: Boolean,
  ) {
    viewModelScope.launch {
      runUiOperation {
          val user = clerk.user ?: throw CoreException("no_user", "User does not exist")
          user.updatePassword(
            UpdateUserPasswordParams(newPassword, currentPassword, signOutOfOtherSessions)
          )
        }
        .onSuccess { _state.value = State.Success }
        .onFailure { _state.value = State.Error(it.displayMessage) }
    }
  }

  fun resetState() {
    _state.value = State.Idle
  }

  sealed interface State {
    object Idle : State

    object Loading : State

    object Success : State

    data class Error(val message: String) : State
  }
}
