package com.clerk.ui.userprofile.mfa

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clerk.api.network.serialization.longErrorMessageOrNull
import com.clerk.api.network.serialization.onFailure
import com.clerk.api.network.serialization.onSuccess
import com.clerk.api.phonenumber.PhoneNumber
import com.clerk.api.phonenumber.setReservedForSecondFactor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

internal class MfaAddSmsViewModel : ViewModel() {

  private val _state = MutableStateFlow<State>(State.Idle)
  val state = _state.asStateFlow()

  fun reserveForSecondFactor(phoneNumber: PhoneNumber) {
    _state.value = State.Loading
    viewModelScope.launch {
      phoneNumber
        .setReservedForSecondFactor(true)
        .onSuccess { _state.value = State.Success }
        .onFailure { _state.value = State.Error(it.longErrorMessageOrNull) }
    }
  }

  sealed interface State {
    data object Idle : State

    data object Loading : State

    data object Success : State

    data class Error(val message: String?) : State
  }
}
