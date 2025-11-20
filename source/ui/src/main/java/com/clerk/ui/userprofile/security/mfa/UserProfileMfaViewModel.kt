package com.clerk.ui.userprofile.security.mfa

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clerk.api.log.ClerkLog
import com.clerk.api.network.serialization.errorMessage
import com.clerk.api.network.serialization.onFailure
import com.clerk.api.network.serialization.onSuccess
import com.clerk.api.phonenumber.PhoneNumber
import com.clerk.api.phonenumber.delete
import com.clerk.api.phonenumber.makeDefaultSecondFactor
import com.clerk.api.user.createBackupCodes
import com.clerk.api.user.disableTotp
import com.clerk.ui.core.common.guardUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class UserProfileMfaViewModel : ViewModel() {

  private val _state = MutableStateFlow<State>(State.Idle)
  val state = _state.asStateFlow()

  fun makeDefaultSecondFactor(phoneNumber: PhoneNumber?) {

    _state.value = State.Loading
    guardUser(userDoesNotExist = { _state.value = State.Error("User does not exist") }) { user ->
      viewModelScope.launch {
        phoneNumber
          ?.makeDefaultSecondFactor()
          ?.onSuccess { _state.value = State.Success }
          ?.onFailure { _state.value = State.Error(it.errorMessage) }
      }
    }
  }

  fun regenerateBackupCodes() {
    _state.value = State.Loading
    guardUser(userDoesNotExist = { _state.value = State.Error("User does not exist") }) { user ->
      viewModelScope.launch {
        user
          .createBackupCodes()
          .onSuccess { _state.value = State.BackupCodesGenerated(it.codes) }
          .onFailure { _state.value = State.Error(it.errorMessage) }
      }
    }
  }

  fun deleteTotp() {
    guardUser({ _state.value = State.Error("User does not exist") }) { user ->
      viewModelScope.launch {
        user
          .disableTotp()
          .onSuccess { _state.value = State.TotpDeleted }
          .onFailure {
            ClerkLog.e("Error deleting TOTP")
            _state.value = State.Error(it.errorMessage)
          }
      }
    }
  }

  fun resetState() {
    _state.value = State.Idle
  }

  fun deletePhoneNumber(phoneNumber: PhoneNumber) {
    viewModelScope.launch {
      phoneNumber
        .delete()
        .onSuccess { _state.value = State.PhoneNumberDeleted }
        .onFailure {
          ClerkLog.e("Error deleting phone number: ${it.errorMessage}")
          _state.value = State.Error(it.errorMessage)
        }
    }
  }

  sealed interface State {
    data object Idle : State

    data object Loading : State

    data class Error(val message: String?) : State

    data object Success : State

    data class BackupCodesGenerated(val codes: List<String>) : State

    data object TotpDeleted : State

    data object PhoneNumberDeleted : State
  }
}
