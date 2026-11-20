package com.clerk.ui.userprofile.verify

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clerk.api.emailaddress.EmailAddress
import com.clerk.api.emailaddress.attemptVerification
import com.clerk.api.emailaddress.prepareVerification
import com.clerk.api.network.model.error.ClerkErrorResponse
import com.clerk.api.network.serialization.ClerkResult
import com.clerk.api.network.serialization.errorMessage
import com.clerk.api.network.serialization.onFailure
import com.clerk.api.network.serialization.onSuccess
import com.clerk.api.phonenumber.PhoneNumber
import com.clerk.api.phonenumber.attemptVerification
import com.clerk.api.phonenumber.prepareVerification
import com.clerk.api.user.attemptTotpVerification
import com.clerk.ui.core.common.guardUser
import com.clerk.ui.core.error.isIncorrectCode
import com.clerk.ui.core.log.ClerkLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

internal class UserProfileVerifyViewModel : ViewModel() {

  private val _state = MutableStateFlow<AuthState>(AuthState.Idle)
  val state = _state.asStateFlow()

  private val _verificationTextState =
    MutableStateFlow<VerificationTextState>(VerificationTextState.Default)
  val verificationTextState = _verificationTextState.asStateFlow()

  fun preparePhoneNumber(phoneNumber: PhoneNumber) {
    _state.value = AuthState.Loading
    viewModelScope.launch(Dispatchers.IO) {
      phoneNumber
        .prepareVerification()
        .onSuccess { _state.value = AuthState.Success }
        .onFailure { _state.value = AuthState.Error(it.errorMessage) }
    }
  }

  fun prepareEmailAddress(emailAddress: EmailAddress) {
    _state.value = AuthState.Loading
    viewModelScope.launch(Dispatchers.IO) {
      emailAddress
        .prepareVerification(EmailAddress.PrepareVerificationParams.EmailCode())
        .onSuccess { _state.value = AuthState.Success }
        .onFailure { _state.value = AuthState.Error(it.errorMessage) }
    }
  }

  fun attemptEmailAddress(emailAddress: EmailAddress, code: String) {
    _verificationTextState.value = VerificationTextState.Verifying
    viewModelScope.launch(Dispatchers.IO) {
      emailAddress
        .attemptVerification(code)
        .onSuccess { _verificationTextState.value = VerificationTextState.Verified() }
        .onFailure { reportFailedAttempt(it) }
    }
  }

  fun resetState() {
    _verificationTextState.value = VerificationTextState.Default
    _state.value = AuthState.Idle
  }

  fun clearError(shownError: String?) {
    _state.compareAndSet(AuthState.Error(shownError), AuthState.Idle)
  }

  fun attemptPhoneNumber(phoneNumber: PhoneNumber, code: String) {
    _verificationTextState.value = VerificationTextState.Verifying
    viewModelScope.launch(Dispatchers.IO) {
      phoneNumber
        .attemptVerification(code)
        .onSuccess { _verificationTextState.value = VerificationTextState.Verified() }
        .onFailure { reportFailedAttempt(it) }
    }
  }

  fun attemptTotp(code: String) {
    _verificationTextState.value = VerificationTextState.Verifying
    guardUser(
      userDoesNotExist = {
        _state.value = AuthState.Error("User does not exist")
        _verificationTextState.value = VerificationTextState.Default
      }
    ) { user ->
      viewModelScope.launch(Dispatchers.IO) {
        user
          .attemptTotpVerification(code)
          .onSuccess {
            _verificationTextState.value = VerificationTextState.Verified(it.backupCodes)
          }
          .onFailure {
            ClerkLog.e("Error attempting TOTP verification ${it.errorMessage}")
            reportFailedAttempt(it)
          }
      }
    }
  }

  private fun reportFailedAttempt(failure: ClerkResult.Failure<ClerkErrorResponse>) {
    if (failure.isIncorrectCode) {
      _state.value = AuthState.Idle
      _verificationTextState.value = VerificationTextState.IncorrectCode
    } else {
      _state.value = AuthState.Error(failure.errorMessage)
      _verificationTextState.value = VerificationTextState.Default
    }
  }

  sealed interface VerificationTextState {
    data object Default : VerificationTextState

    data object Verifying : VerificationTextState

    data class Verified(val backupCodes: List<String>? = null) : VerificationTextState

    data object IncorrectCode : VerificationTextState
  }

  sealed interface AuthState {
    object Idle : AuthState

    object Loading : AuthState

    data object Success : AuthState

    data class Error(val error: String?) : AuthState
  }
}
