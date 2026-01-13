package com.clerk.customflows.emailpassword.signup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clerk.api.Clerk
import com.clerk.api.auth.types.VerificationType
import com.clerk.api.network.serialization.flatMap
import com.clerk.api.network.serialization.onFailure
import com.clerk.api.network.serialization.onSuccess
import com.clerk.api.signup.sendCode
import com.clerk.api.signup.verifyCode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.launch

class EmailPasswordSignUpViewModel : ViewModel() {
  private val _uiState =
    MutableStateFlow<EmailPasswordSignUpUiState>(EmailPasswordSignUpUiState.Loading)
  val uiState = _uiState.asStateFlow()

  init {
    combine(Clerk.userFlow, Clerk.isInitialized) { user, isInitialized ->
        _uiState.value =
          when {
            !isInitialized -> EmailPasswordSignUpUiState.Loading
            user != null -> EmailPasswordSignUpUiState.Verified
            else -> EmailPasswordSignUpUiState.Unverified
          }
      }
      .launchIn(viewModelScope)
  }

  fun submit(email: String, password: String) {
    viewModelScope.launch {
      Clerk.auth
        .signUp {
          this.email = email
          this.password = password
        }
        .flatMap { it.sendCode { this.email = email } }
        .onSuccess { _uiState.value = EmailPasswordSignUpUiState.Verifying }
        .onFailure {
          // See https://clerk.com/docs/custom-flows/error-handling
          // for more info on error handling
        }
    }
  }

  fun verify(code: String) {
    val inProgressSignUp = Clerk.auth.signUp ?: return
    viewModelScope.launch {
      inProgressSignUp
        .verifyCode(code, VerificationType.EMAIL)
        .onSuccess { _uiState.value = EmailPasswordSignUpUiState.Verified }
        .onFailure {
          // See https://clerk.com/docs/custom-flows/error-handling
          // for more info on error handling
        }
    }
  }

  sealed interface EmailPasswordSignUpUiState {
    data object Loading : EmailPasswordSignUpUiState

    data object Unverified : EmailPasswordSignUpUiState

    data object Verifying : EmailPasswordSignUpUiState

    data object Verified : EmailPasswordSignUpUiState
  }
}
