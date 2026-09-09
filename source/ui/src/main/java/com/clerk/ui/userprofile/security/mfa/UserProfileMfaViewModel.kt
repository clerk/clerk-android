package com.clerk.ui.userprofile.security.mfa

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clerk.api.*
import com.clerk.ui.core.common.displayMessage
import com.clerk.ui.core.common.runUiOperation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

internal class UserProfileMfaViewModel(private val clerk: Clerk) : ViewModel() {

  private val _state = MutableStateFlow<State>(State.Idle)
  val state = _state.asStateFlow()

  fun makeDefaultSecondFactor(phoneNumber: PhoneNumber?) = perform {
    val phone =
      phoneNumber
        ?: throw CoreException("phone_not_found", "This phone number is no longer available.")
    phone.makeDefaultSecondFactor()
    State.Success
  }

  fun regenerateBackupCodes() = perform {
    val user = clerk.user ?: throw CoreException("no_user", "User does not exist")
    State.BackupCodesGenerated(user.createBackupCode().codes)
  }

  fun deleteTotp() = perform {
    val user = clerk.user ?: throw CoreException("no_user", "User does not exist")
    user.disableTOTP()
    State.TotpDeleted
  }

  fun deletePhoneNumber(phoneNumber: PhoneNumber) = perform {
    phoneNumber.destroy()
    State.PhoneNumberDeleted
  }

  private fun perform(operation: suspend () -> State) {
    _state.value = State.Loading
    viewModelScope.launch {
      runUiOperation { operation() }
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

    data class Error(val message: String?) : State

    data object Success : State

    data class BackupCodesGenerated(val codes: List<String>) : State

    data object TotpDeleted : State

    data object PhoneNumberDeleted : State
  }
}
