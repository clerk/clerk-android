package com.clerk.ui.userprofile.security.mfa

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clerk.api.network.serialization.errorMessage
import com.clerk.api.network.serialization.onFailure
import com.clerk.api.phonenumber.PhoneNumber
import com.clerk.api.phonenumber.delete
import com.clerk.api.user.createBackupCodes
import com.clerk.api.user.disableTotp
import com.clerk.ui.core.common.guardUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class UserProfileMfaViewModel : ViewModel() {

  private val _state = MutableStateFlow<State>(State.Idle)
  val state = _state.asStateFlow()

  fun removeTotp() {
    _state.value = State.Loading
    guardUser(userDoesNotExist = { _state.value = State.Error("User does not exist") }) { user ->
      viewModelScope.launch {
        user.disableTotp().onFailure { _state.value = State.Error(it.errorMessage) }
      }
    }
  }

  fun removePhoneNumber(phoneNumber: PhoneNumber) {
    _state.value = State.Loading
    guardUser(userDoesNotExist = { _state.value = State.Error("User does not exist") }) { user ->
      viewModelScope.launch {
        phoneNumber.delete().onFailure { _state.value = State.Error(it.errorMessage) }
      }
    }
  }

  fun regenerateBackupCodes() {
    _state.value = State.Loading
    guardUser(userDoesNotExist = { _state.value = State.Error("User does not exist") }) { user ->
      viewModelScope.launch {
        user.createBackupCodes().onFailure { _state.value = State.Error(it.errorMessage) }
      }
    }
  }

  sealed interface State {
    data object Idle : State

    data object Loading : State

    data class Error(val message: String?) : State
  }
}
