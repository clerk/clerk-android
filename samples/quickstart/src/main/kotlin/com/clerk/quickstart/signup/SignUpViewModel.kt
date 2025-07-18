package com.clerk.quickstart.signup

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clerk.Clerk
import com.clerk.network.serialization.longErrorMessageOrNull
import com.clerk.network.serialization.onFailure
import com.clerk.network.serialization.onSuccess
import com.clerk.signup.SignUp
import com.clerk.signup.attemptVerification
import com.clerk.signup.prepareVerification
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SignUpViewModel : ViewModel() {
  private val _uiState = MutableStateFlow<SignUpUiState>(SignUpUiState.SignedOut)
  val uiState = _uiState.asStateFlow()

  fun signUp(email: String, password: String) {
    viewModelScope.launch {
      SignUp.create(SignUp.CreateParams.Standard(emailAddress = email, password = password))
        .onSuccess {
          if (it.status == SignUp.Status.COMPLETE) {
            // Handle successful sign-up
          } else {
            _uiState.value = SignUpUiState.NeedsVerification
            it.prepareVerification(SignUp.PrepareVerificationParams.Strategy.EmailCode())
          }
        }
        .onFailure {
          Log.e("SignUpViewModel", "${it.longErrorMessageOrNull}", it.throwable)
          _uiState.value = SignUpUiState.SignedOut
        }
    }
  }

  fun verify(code: String) {
    val inProgressSignUp = Clerk.signUp ?: return
    viewModelScope.launch {
      inProgressSignUp
        .attemptVerification(SignUp.AttemptVerificationParams.EmailCode(code))
        .onSuccess { _uiState.value = SignUpUiState.Success }
        .onFailure { Log.e("SignUpViewModel", "${it.longErrorMessageOrNull}", it.throwable) }
    }
  }
}

sealed interface SignUpUiState {
  data object SignedOut : SignUpUiState

  data object Success : SignUpUiState

  data object NeedsVerification : SignUpUiState
}
