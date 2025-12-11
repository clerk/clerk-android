package com.clerk.ui.userprofile.update

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clerk.api.log.ClerkLog
import com.clerk.api.network.serialization.errorMessage
import com.clerk.api.network.serialization.flatMap
import com.clerk.api.network.serialization.onFailure
import com.clerk.api.network.serialization.onSuccess
import com.clerk.api.user.User
import com.clerk.api.user.deleteProfileImage
import com.clerk.api.user.get
import com.clerk.api.user.setProfileImage
import com.clerk.api.user.update
import com.clerk.ui.core.common.guardUser
import java.io.File
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class UpdateProfileViewModel : ViewModel() {

  private val _state = MutableStateFlow<State>(State.Idle)
  val state = _state.asStateFlow()

  fun removeProfileImage() {
    guardUser({ _state.value = State.Error("User not authenticated") }) { user ->
      viewModelScope.launch {
        _state.value = State.Loading
        user
          .deleteProfileImage()
          .onFailure {
            _state.value = State.Error("Failed to delete profile image: ${it.errorMessage}")
          }
          .onSuccess {
            // Fetch the latest user and update local state so UI re-renders
            user
              .get()
              .onFailure {
                _state.value = State.Error("Failed to refresh user: ${it.errorMessage}")
              }
              .onSuccess { refreshed -> _state.value = State.Success }
          }
      }
    }
  }

  fun uploadProfileImage(file: File) {
    guardUser({ _state.value = State.Error("User not authenticated") }) { user ->
      viewModelScope.launch {
        _state.value = State.Loading
        user
          .setProfileImage(file)
          .onFailure {
            _state.value = State.Error("Failed to set profile image: ${it.errorMessage}")
          }
          .onSuccess {
            user
              .get()
              .onFailure {
                _state.value = State.Error("Failed to refresh user: ${it.errorMessage}")
              }
              .onSuccess { refreshed -> _state.value = State.Success }
          }
      }
    }
  }

  fun save(firstName: String?, lastName: String?, username: String?) {
    ClerkLog.e("saving with values: $firstName, $lastName, $username")
    _state.value = State.Loading
    guardUser({ _state.value = State.Error("User not authenticated") }) { user ->
      viewModelScope.launch {
        user
          .update(
            User.UpdateParams(firstName = firstName, lastName = lastName, username = username)
          )
          .flatMap { user.get() }
          .onFailure { _state.value = State.Error("Failed to update profile: ${it.errorMessage}") }
          .onSuccess { _state.value = State.Success }
      }
    }
  }

  fun reset() {
    _state.value = State.Idle
  }

  sealed interface State {
    data object Idle : State

    data object Loading : State

    data object Success : State

    data class Error(val message: String) : State
  }
}
