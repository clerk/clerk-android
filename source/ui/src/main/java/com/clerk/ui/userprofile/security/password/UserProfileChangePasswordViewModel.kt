package com.clerk.ui.userprofile.security.password

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clerk.api.network.serialization.errorMessage
import com.clerk.api.network.serialization.onFailure
import com.clerk.api.network.serialization.onSuccess
import com.clerk.api.user.User
import com.clerk.api.user.updatePassword
import com.clerk.ui.core.common.guardUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

internal class UserProfileChangePasswordViewModel : ViewModel() {

  private val _state = MutableStateFlow<State>(State.Idle)
  val state = _state.asStateFlow()

  fun resetPassword(
    currentPassword: String?,
    newPassword: String,
    signOutOfOtherSessions: Boolean,
  ) {
    guardUser(userDoesNotExist = { _state.value = State.Error("User does not exist") }) { user ->
      viewModelScope.launch {
        user
          .updatePassword(
            User.UpdatePasswordParams(
              currentPassword = currentPassword,
              newPassword = newPassword,
              signOutOfOtherSessions = signOutOfOtherSessions,
            )
          )
          .onSuccess { _state.value = State.Success }
          .onFailure { error -> _state.value = State.Error(error.errorMessage) }
      }
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
