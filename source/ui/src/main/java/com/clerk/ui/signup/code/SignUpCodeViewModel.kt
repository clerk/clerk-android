package com.clerk.ui.signup.code

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clerk.api.Clerk
import com.clerk.api.network.serialization.errorMessage
import com.clerk.api.network.serialization.onFailure
import com.clerk.api.network.serialization.onSuccess
import com.clerk.api.signup.SignUp
import com.clerk.api.signup.attemptVerification
import com.clerk.api.signup.prepareVerification
import com.clerk.ui.auth.AuthenticationViewState
import com.clerk.ui.auth.VerificationUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

internal class SignUpCodeViewModel : ViewModel() {

  private val _verificationState = MutableStateFlow<VerificationUiState>(VerificationUiState.Idle)
  val verificationState = _verificationState.asStateFlow()

  private val _state = MutableStateFlow<AuthenticationViewState>(AuthenticationViewState.Idle)
  val state = _state.asStateFlow()

  fun prepare(field: SignUpCodeField) {
    val signUp = Clerk.client.signUp ?: return
    viewModelScope.launch {
      val signUp =
        when (field) {
          is SignUpCodeField.Email -> {
            signUp.prepareVerification(SignUp.PrepareVerificationParams.Strategy.EmailCode())
          }
          is SignUpCodeField.Phone -> {
            signUp.prepareVerification(SignUp.PrepareVerificationParams.Strategy.PhoneCode())
          }
        }
      signUp
        .onSuccess { _state.value = AuthenticationViewState.Success.SignUp(it) }
        .onFailure { _state.value = AuthenticationViewState.Error(it.errorMessage) }
    }
  }

  fun attempt(code: String, field: SignUpCodeField) {
    _verificationState.value = VerificationUiState.Verifying
    val signUp = Clerk.client.signUp ?: return
    viewModelScope.launch {
      val signUp =
        when (field) {
          is SignUpCodeField.Email ->
            signUp.attemptVerification(SignUp.AttemptVerificationParams.EmailCode(code))
          is SignUpCodeField.Phone ->
            signUp.attemptVerification(SignUp.AttemptVerificationParams.PhoneCode(code))
        }
      signUp
        .onSuccess {
          _verificationState.value = VerificationUiState.Verified
          _state.value = AuthenticationViewState.Success.SignUp(it)
        }
        .onFailure {
          _verificationState.value = VerificationUiState.Error(it.errorMessage)
          _state.value = AuthenticationViewState.Error(it.errorMessage)
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
