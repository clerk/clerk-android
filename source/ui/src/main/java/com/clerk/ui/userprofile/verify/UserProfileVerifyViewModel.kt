package com.clerk.ui.userprofile.verify

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clerk.api.emailaddress.EmailAddress
import com.clerk.api.emailaddress.attemptVerification
import com.clerk.api.emailaddress.prepareVerification
import com.clerk.api.network.serialization.errorMessage
import com.clerk.api.network.serialization.onFailure
import com.clerk.api.network.serialization.onSuccess
import com.clerk.api.phonenumber.PhoneNumber
import com.clerk.api.phonenumber.attemptVerification
import com.clerk.api.phonenumber.prepareVerification
import com.clerk.api.user.attemptTOTPVerification
import com.clerk.ui.core.common.guardUser
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
        .onSuccess { _verificationTextState.value = VerificationTextState.Verified }
        .onFailure { _verificationTextState.value = VerificationTextState.Error(it.errorMessage) }
    }
  }

  fun attemptPhoneNumber(phoneNumber: PhoneNumber, code: String) {
    _verificationTextState.value = VerificationTextState.Verifying
    viewModelScope.launch(Dispatchers.IO) {
      phoneNumber
        .attemptVerification(code)
        .onSuccess { _verificationTextState.value = VerificationTextState.Verified }
        .onFailure { _verificationTextState.value = VerificationTextState.Error(it.errorMessage) }
    }
  }

  fun attemptTotp(code: String) {
    _verificationTextState.value = VerificationTextState.Verifying
    guardUser(
      userDoesNotExist = {
        _verificationTextState.value = VerificationTextState.Error("User does not exist")
      }
    ) { user ->
      viewModelScope.launch(Dispatchers.IO) {
        user
          .attemptTOTPVerification(code)
          .onSuccess { _verificationTextState.value = VerificationTextState.Verified }
          .onFailure { _verificationTextState.value = VerificationTextState.Error(it.errorMessage) }
      }
    }
  }

  sealed interface VerificationTextState {
    data object Default : VerificationTextState

    data object Verifying : VerificationTextState

    data object Verified : VerificationTextState

    data class Error(val error: String?) : VerificationTextState
  }

  sealed interface AuthState {
    object Idle : AuthState

    object Loading : AuthState

    data object Success : AuthState

    data class Error(val error: String?) : AuthState
  }
}
