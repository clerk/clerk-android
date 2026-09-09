package com.clerk.ui.userprofile.phone

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clerk.api.*
import com.clerk.ui.core.common.displayMessage
import com.clerk.ui.core.common.runUiOperation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

internal class UserProfileAddPhoneViewModel(private val clerk: Clerk) : ViewModel() {

  private val _state = MutableStateFlow<State>(State.Idle)
  val state = _state.asStateFlow()

  fun addPhoneNumber(phoneNumber: String) {
    _state.value = State.Loading
    viewModelScope.launch {
      runUiOperation {
          val user = clerk.user ?: throw CoreException("no_user", "User does not exist")
          user.createPhoneNumber(CreatePhoneNumberParams(phoneNumber))
        }
        .onSuccess { _state.value = State.Success(it) }
        .onFailure { _state.value = State.Error(it.displayMessage) }
    }
  }

  fun setAsPrimary(phoneNumber: PhoneNumber) {
    _state.value = State.Loading
    viewModelScope.launch {
      runUiOperation {
          val user = clerk.user ?: throw CoreException("no_user", "User does not exist")
          user.update(UpdateUserParams(primaryPhoneNumberId = Field.Value(phoneNumber.id)))
        }
        .onSuccess { _state.value = State.SetPhoneAsPrimarySuccess }
        .onFailure { _state.value = State.Error(it.displayMessage) }
    }
  }

  fun deletePhoneNumber(phoneNumber: PhoneNumber) {
    _state.value = State.Loading
    viewModelScope.launch {
      runUiOperation { phoneNumber.destroy() }
        .onSuccess { _state.value = State.DeletedPhoneNumber }
        .onFailure { _state.value = State.Error(it.displayMessage) }
    }
  }

  fun resetState() {
    _state.value = State.Idle
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
