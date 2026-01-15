package com.clerk.quickstart.signup

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clerk.api.Clerk
import com.clerk.api.auth.types.VerificationType
import com.clerk.api.network.serialization.errorMessage
import com.clerk.api.network.serialization.onFailure
import com.clerk.api.network.serialization.onSuccess
import com.clerk.api.signup.SignUp
import com.clerk.api.signup.sendCode
import com.clerk.api.signup.verifyCode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SignUpViewModel : ViewModel() {
  private val _uiState = MutableStateFlow<SignUpUiState>(SignUpUiState.SignedOut)
  val uiState = _uiState.asStateFlow()

  fun signUp(email: String, password: String) {
    viewModelScope.launch {
      Clerk.auth
        .signUp {
          this.email = email
          this.password = password
        }
        .onSuccess {
          if (it.status == SignUp.Status.COMPLETE) {
            _uiState.value = SignUpUiState.Success
          } else {
            _uiState.value = SignUpUiState.NeedsVerification
            it.sendCode { this.email = email }
          }
        }
        .onFailure {
          Log.e("SignUpViewModel", "${it.errorMessage}", it.throwable)
          _uiState.value = SignUpUiState.SignedOut
        }
    }
  }

  fun verify(code: String) {
    val inProgressSignUp = Clerk.auth.currentSignUp ?: return
    viewModelScope.launch {
      inProgressSignUp
        .verifyCode(code, VerificationType.EMAIL)
        .onSuccess { _uiState.value = SignUpUiState.Success }
        .onFailure { Log.e("SignUpViewModel", "${it.errorMessage}", it.throwable) }
    }
  }
}

sealed interface SignUpUiState {
  data object SignedOut : SignUpUiState

  data object Success : SignUpUiState

  data object NeedsVerification : SignUpUiState
}
