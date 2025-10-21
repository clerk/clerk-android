package com.clerk.ui.userprofile.update

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clerk.api.Clerk
import com.clerk.api.network.serialization.errorMessage
import com.clerk.api.network.serialization.onFailure
import com.clerk.api.network.serialization.onSuccess
import com.clerk.api.user.deleteProfileImage
import com.clerk.api.user.get
import com.clerk.ui.core.common.guardUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class UpdateProfileViewModel : ViewModel() {

  private val _state = MutableStateFlow<State>(State.Idle)
  val state = _state.asStateFlow()

  private val _user = MutableStateFlow<com.clerk.api.user.User?>(Clerk.userFlow.value)
  val user = _user.asStateFlow()

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
              .onSuccess { refreshed ->
                _user.value = refreshed
                _state.value = State.Success
              }
          }
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
