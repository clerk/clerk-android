package com.clerk.ui.userprofile.security.mfa

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clerk.api.Clerk
import com.clerk.api.network.model.error.ClerkErrorResponse
import com.clerk.api.network.serialization.ClerkResult
import com.clerk.api.network.serialization.errorMessage
import com.clerk.api.network.serialization.onFailure
import com.clerk.api.network.serialization.onSuccess
import com.clerk.api.phonenumber.PhoneNumber
import com.clerk.api.phonenumber.delete
import com.clerk.api.phonenumber.makeDefaultSecondFactor
import com.clerk.api.user.createBackupCodes
import com.clerk.api.user.disableTotp
import com.clerk.ui.core.common.guardUser
import com.clerk.ui.core.log.ClerkLog
import com.clerk.ui.reverification.isReverificationCancelled
import com.clerk.ui.reverification.reverify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

internal class UserProfileMfaViewModel : ViewModel() {

  private val _state = MutableStateFlow<State>(State.Idle)
  val state = _state.asStateFlow()

  fun makeDefaultSecondFactor(phoneNumber: PhoneNumber?) {

    _state.value = State.Loading
    guardUser(userDoesNotExist = { _state.value = State.Error("User does not exist") }) { user ->
      viewModelScope.launch {
        if (phoneNumber == null) return@launch
        Clerk.reverify { phoneNumber.makeDefaultSecondFactor() }
          .onSuccess { _state.value = State.Success }
          .onFailure { _state.value = failureState(it) }
      }
    }
  }

  fun regenerateBackupCodes() {
    _state.value = State.Loading
    guardUser(userDoesNotExist = { _state.value = State.Error("User does not exist") }) { user ->
      viewModelScope.launch {
        Clerk.reverify { user.createBackupCodes() }
          .onSuccess { _state.value = State.BackupCodesGenerated(it.codes) }
          .onFailure { _state.value = failureState(it) }
      }
    }
  }

  fun deleteTotp() {
    guardUser({ _state.value = State.Error("User does not exist") }) { user ->
      viewModelScope.launch {
        Clerk.reverify { user.disableTotp() }
          .onSuccess { _state.value = State.TotpDeleted }
          .onFailure {
            if (it.isReverificationCancelled) return@onFailure
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
      Clerk.reverify { phoneNumber.delete() }
        .onSuccess { _state.value = State.PhoneNumberDeleted }
        .onFailure {
          if (it.isReverificationCancelled) return@onFailure
          ClerkLog.e("Error deleting phone number: ${it.errorMessage}")
          _state.value = State.Error(it.errorMessage)
        }
    }
  }

  private fun failureState(failure: ClerkResult.Failure<ClerkErrorResponse>): State =
    if (failure.isReverificationCancelled) State.Idle else State.Error(failure.errorMessage)

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
