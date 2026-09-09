package com.clerk.ui.userprofile.verify

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clerk.api.*
import com.clerk.ui.core.common.displayMessage
import com.clerk.ui.core.common.runUiOperation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

internal class UserProfileVerifyViewModel(private val clerk: Clerk) : ViewModel() {

  private val _state = MutableStateFlow<AuthState>(AuthState.Idle)
  val state = _state.asStateFlow()

  private val _verificationTextState =
    MutableStateFlow<VerificationTextState>(VerificationTextState.Default)
  val verificationTextState = _verificationTextState.asStateFlow()

  fun preparePhoneNumber(phoneNumber: PhoneNumber) {
    _state.value = AuthState.Loading
    viewModelScope.launch {
      runUiOperation { phoneNumber.prepareVerification() }
        .onSuccess { _state.value = AuthState.Success }
        .onFailure { _state.value = AuthState.Error(it.displayMessage) }
    }
  }

  fun prepareEmailAddress(emailAddress: EmailAddress) {
    _state.value = AuthState.Loading
    viewModelScope.launch {
      runUiOperation {
          emailAddress.prepareVerification(
            PrepareEmailAddressVerificationParams.Case1(
              EmailAddressPrepareVerificationParamsCase1()
            )
          )
        }
        .onSuccess { _state.value = AuthState.Success }
        .onFailure { _state.value = AuthState.Error(it.displayMessage) }
    }
  }

  fun attemptEmailAddress(emailAddress: EmailAddress, code: String) {
    _verificationTextState.value = VerificationTextState.Verifying
    viewModelScope.launch {
      runUiOperation {
          emailAddress.attemptVerification(AttemptEmailAddressVerificationParams(code))
        }
        .onSuccess { _verificationTextState.value = VerificationTextState.Verified() }
        .onFailure { _verificationTextState.value = VerificationTextState.Error(it.displayMessage) }
    }
  }

  fun resetState() {
    _verificationTextState.value = VerificationTextState.Default
    _state.value = AuthState.Idle
  }

  fun attemptPhoneNumber(phoneNumber: PhoneNumber, code: String) {
    _verificationTextState.value = VerificationTextState.Verifying
    viewModelScope.launch {
      runUiOperation { phoneNumber.attemptVerification(AttemptPhoneNumberVerificationParams(code)) }
        .onSuccess { _verificationTextState.value = VerificationTextState.Verified() }
        .onFailure { _verificationTextState.value = VerificationTextState.Error(it.displayMessage) }
    }
  }

  fun attemptTotp(code: String) {
    _verificationTextState.value = VerificationTextState.Verifying
    viewModelScope.launch {
      runUiOperation {
          val user = clerk.user ?: throw CoreException("no_user", "User does not exist")
          user.verifyTOTP(VerifyTOTPParams(code))
        }
        .onSuccess { _verificationTextState.value = VerificationTextState.Verified(it.backupCodes) }
        .onFailure { _verificationTextState.value = VerificationTextState.Error(it.displayMessage) }
    }
  }

  sealed interface VerificationTextState {
    data object Default : VerificationTextState

    data object Verifying : VerificationTextState

    data class Verified(val backupCodes: List<String>? = null) : VerificationTextState

    data class Error(val error: String?) : VerificationTextState
  }

  sealed interface AuthState {
    object Idle : AuthState

    object Loading : AuthState

    data object Success : AuthState

    data class Error(val error: String?) : AuthState
  }
}
