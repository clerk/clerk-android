package com.clerk.ui.userprofile.phone

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clerk.api.log.ClerkLog
import com.clerk.api.network.serialization.errorMessage
import com.clerk.api.network.serialization.onFailure
import com.clerk.api.network.serialization.onSuccess
import com.clerk.api.phonenumber.PhoneNumber
import com.clerk.api.phonenumber.delete
import com.clerk.api.user.User
import com.clerk.api.user.createPhoneNumber
import com.clerk.api.user.update
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
          .onSuccess { _state.value = State.Success(it) }
          .onFailure { error ->
            ClerkLog.e(
              "UserProfileAddPhoneViewModel - Failed to add phone number: ${error.errorMessage}"
            )
            _state.value = State.Error(error.errorMessage)
          }
      }
    }
  }

  fun setAsPrimary(phoneNumber: PhoneNumber) {
    guardUser({ _state.value = State.Error("User does not exist") }) { user ->
      viewModelScope.launch {
        user
          .update(User.UpdateParams(primaryPhoneNumberId = phoneNumber.id))
          .onSuccess { _state.value = State.SetPhoneAsPrimarySuccess }
          .onFailure {
            ClerkLog.e("Failed to update primary phone ")
            _state.value = State.Error(it.errorMessage)
          }
      }
    }
  }

  fun deletePhoneNumber(phoneNumber: PhoneNumber) {
    viewModelScope.launch {
      phoneNumber
        .delete()
        .onSuccess { _state.value = State.DeletedPhoneNumber }
        .onFailure { _state.value = State.Error(it.errorMessage) }
    }
  }

  sealed interface State {
    data object Idle : State

    data object Loading : State

    data class Error(val message: String?) : State

    data class Success(val phoneNumber: PhoneNumber) : State

    data object SetPhoneAsPrimarySuccess : State

    data object DeletedPhoneNumber : State
  }
}
