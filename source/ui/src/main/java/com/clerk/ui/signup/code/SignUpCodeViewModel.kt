package com.clerk.ui.signup.code

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clerk.api.*
import com.clerk.ui.auth.*
import com.clerk.ui.core.common.displayMessage
import com.clerk.ui.core.common.runUiOperation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

internal class SignUpCodeViewModel(private val clerk: Clerk) : ViewModel() {
  private val _verificationState = MutableStateFlow<VerificationUiState>(VerificationUiState.Idle)
  val verificationState = _verificationState.asStateFlow()
  private val _state = MutableStateFlow<AuthenticationViewState>(AuthenticationViewState.Idle)
  val state = _state.asStateFlow()

  fun prepare(field: SignUpCodeField) =
    guardSignUp(clerk, _state) { signUp ->
      if (
        field is SignUpCodeField.Email && signUp.emailVerificationStrategy(clerk) == "email_link"
      ) {
        _state.value = AuthenticationViewState.Success.SignUp(signUp)
        return@guardSignUp
      }
      _state.value = AuthenticationViewState.Loading
      viewModelScope.launch {
        runUiOperation {
            when (field) {
              is SignUpCodeField.Email -> signUp.verifications.sendEmailCode()
              is SignUpCodeField.Phone -> signUp.verifications.sendPhoneCode()
            }
          }
          .onSuccess { _state.value = AuthenticationViewState.Idle }
          .onFailure { _state.value = AuthenticationViewState.Error(it.displayMessage) }
      }
    }

  fun attempt(code: String, field: SignUpCodeField) =
    guardSignUp(clerk, _state) { signUp ->
      _verificationState.value = VerificationUiState.Verifying
      viewModelScope.launch {
        runUiOperation {
            when (field) {
              is SignUpCodeField.Email ->
                signUp.verifications.verifyEmailCode(SignUpEmailCodeVerifyParams(code))
              is SignUpCodeField.Phone ->
                signUp.verifications.verifyPhoneCode(SignUpPhoneCodeVerifyParams(code))
            }
          }
          .onSuccess {
            _verificationState.value = VerificationUiState.Verified
            _state.value = AuthenticationViewState.Success.SignUp(signUp)
          }
          .onFailure {
            _verificationState.value = VerificationUiState.Error(it.displayMessage)
            _state.value = AuthenticationViewState.Error(it.displayMessage)
          }
      }
    }

  fun reset() {
    _state.value = AuthenticationViewState.Idle
  }

  fun resetVerificationState() {
    _verificationState.value = VerificationUiState.Idle
  }
}
