package com.clerk.ui.userprofile.phone

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clerk.api.log.ClerkLog
import com.clerk.api.network.serialization.errorMessage
import com.clerk.api.network.serialization.onFailure
import com.clerk.api.network.serialization.onSuccess
import com.clerk.api.user.createPhoneNumber
import com.clerk.ui.core.common.guardUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class UserProfileAddPhoneViewModel : ViewModel() {

  private val _state = MutableStateFlow<State>(State.Idle)
  val state = _state.asStateFlow()

  fun addPhoneNumber(phoneNumber: String) {
    _state.value = State.Loading
    guardUser({
      ClerkLog.e("UserProfileAddPhoneViewModel - User is null when adding phone number")
      _state.value = State.Error("User does not exist")
    }) { user ->
      viewModelScope.launch {
        user
          .createPhoneNumber(phoneNumber)
          .onSuccess { _state.value = State.Success }
          .onFailure { error ->
            ClerkLog.e(
              "UserProfileAddPhoneViewModel - Failed to add phone number: ${error.errorMessage}"
            )
            _state.value = State.Error(error.errorMessage)
          }
      }
    }
  }

  sealed interface State {
    data object Idle : State

    data object Loading : State

    data class Error(val message: String?) : State

    data object Success : State
  }
}
