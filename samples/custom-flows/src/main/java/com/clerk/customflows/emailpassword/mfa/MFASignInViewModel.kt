package com.clerk.customflows.emailpassword.mfa

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clerk.Clerk
import com.clerk.network.serialization.onFailure
import com.clerk.network.serialization.onSuccess
import com.clerk.signin.SignIn
import com.clerk.signin.attemptSecondFactor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MFASignInViewModel : ViewModel() {
  private val _uiState = MutableStateFlow<UiState>(UiState.Unverified)
  val uiState = _uiState.asStateFlow()

  fun submit(email: String, password: String) {
    viewModelScope.launch {
      SignIn.create(SignIn.CreateParams.Strategy.Password(identifier = email, password = password))
        .onSuccess {
          if (it.status == SignIn.Status.NEEDS_SECOND_FACTOR) {
            // Display TOTP Form
            _uiState.value = UiState.NeedsSecondFactor
          } else {
            // If the status is not needsSecondFactor, check why. User may need to
            // complete different steps.
          }
        }
        .onFailure {
          // See https://clerk.com/docs/custom-flows/error-handling
          // for more info on error handling
        }
    }
  }

  fun verify(code: String) {
    val inProgressSignIn = Clerk.signIn ?: return
    viewModelScope.launch {
      inProgressSignIn
        .attemptSecondFactor(SignIn.AttemptSecondFactorParams.TOTP(code))
        .onSuccess {
          if (it.status == SignIn.Status.COMPLETE) {
            // User is now signed in and verified.
            // You can navigate to the next screen or perform other actions.
            _uiState.value = UiState.Verified
          }
        }
        .onFailure {
          // See https://clerk.com/docs/custom-flows/error-handling
          // for more info on error handling
        }
    }
  }

  sealed interface UiState {
    data object Unverified : UiState

    data object Verified : UiState

    data object NeedsSecondFactor : UiState
  }
}
