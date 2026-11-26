package com.clerk.ui.userprofile.mfa

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clerk.api.Clerk
import com.clerk.api.network.serialization.errorMessage
import com.clerk.api.network.serialization.onFailure
import com.clerk.api.network.serialization.onSuccess
import com.clerk.api.phonenumber.PhoneNumber
import com.clerk.api.phonenumber.setReservedForSecondFactor
import com.clerk.ui.reverification.isReverificationCancelled
import com.clerk.ui.reverification.reverify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

internal class MfaAddSmsViewModel : ViewModel() {

  private val _state = MutableStateFlow<State>(State.Idle)
  val state = _state.asStateFlow()

  fun reserveForSecondFactor(phoneNumber: PhoneNumber) {
    _state.value = State.Loading
    viewModelScope.launch {
      Clerk.reverify { phoneNumber.setReservedForSecondFactor(true) }
        .onSuccess { _state.value = State.Success(it) }
        .onFailure {
          _state.value =
            if (it.isReverificationCancelled) State.Idle else State.Error(it.errorMessage)
        }
    }
  }

  fun resetState() {
    _state.value = State.Idle
  }

  sealed interface State {
    data object Idle : State

    data object Loading : State

    data class Success(val phoneNumber: PhoneNumber) : State

    data class Error(val message: String?) : State
  }
}
