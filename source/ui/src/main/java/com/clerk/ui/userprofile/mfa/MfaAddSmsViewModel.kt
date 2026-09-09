package com.clerk.ui.userprofile.mfa

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clerk.api.*
import com.clerk.ui.core.common.displayMessage
import com.clerk.ui.core.common.runUiOperation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

internal class MfaAddSmsViewModel(private val clerk: Clerk) : ViewModel() {

  private val _state = MutableStateFlow<State>(State.Idle)
  val state = _state.asStateFlow()

  fun reserveForSecondFactor(phoneNumber: PhoneNumber) {
    _state.value = State.Loading
    viewModelScope.launch {
      runUiOperation {
          val phone = phoneNumber.setReservedForSecondFactor(SetReservedForSecondFactorParams(true))
          State.Success(phone, phone.backupCodes())
        }
        .onSuccess { _state.value = it }
        .onFailure { _state.value = State.Error(it.displayMessage) }
    }
  }

  fun resetState() {
    _state.value = State.Idle
  }

  sealed interface State {
    data object Idle : State

    data object Loading : State

    data class Success(val phoneNumber: PhoneNumber, val backupCodes: List<String>?) : State

    data class Error(val message: String?) : State
  }
}
