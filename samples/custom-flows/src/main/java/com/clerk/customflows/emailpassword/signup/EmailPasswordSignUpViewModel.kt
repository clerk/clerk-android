package com.clerk.customflows.emailpassword.signup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clerk.Clerk
import com.clerk.network.serialization.flatMap
import com.clerk.network.serialization.onFailure
import com.clerk.network.serialization.onSuccess
import com.clerk.signup.SignUp
import com.clerk.signup.attemptVerification
import com.clerk.signup.prepareVerification
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
      SignUp.create(SignUp.CreateParams.Standard(emailAddress = email, password = password))
        .flatMap { it.prepareVerification(SignUp.PrepareVerificationParams.Strategy.EmailCode()) }
        .onSuccess { _uiState.value = EmailPasswordSignUpUiState.Verifying }
        .onFailure {
          // See https://clerk.com/docs/custom-flows/error-handling
          // for more info on error handling
        }
    }
  }

  fun verify(code: String) {
    val inProgressSignUp = Clerk.signUp ?: return
    viewModelScope.launch {
      inProgressSignUp
        .attemptVerification(SignUp.AttemptVerificationParams.EmailCode(code))
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
