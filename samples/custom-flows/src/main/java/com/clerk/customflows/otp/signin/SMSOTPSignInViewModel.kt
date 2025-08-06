package com.clerk.customflows.otp.signin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clerk.api.Clerk
import com.clerk.api.network.serialization.flatMap
import com.clerk.api.network.serialization.onFailure
import com.clerk.api.network.serialization.onSuccess
import com.clerk.api.signin.SignIn
import com.clerk.api.signin.attemptFirstFactor
import com.clerk.api.signin.prepareFirstFactor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.launch

class SMSOTPSignInViewModel : ViewModel() {
  private val _uiState = MutableStateFlow<UiState>(UiState.Unverified)
  val uiState = _uiState.asStateFlow()

  init {
    combine(Clerk.isInitialized, Clerk.userFlow) { isInitialized, user ->
        _uiState.value =
          when {
            !isInitialized -> UiState.Loading
            user == null -> UiState.Unverified
            else -> UiState.Verified
          }
      }
      .launchIn(viewModelScope)
  }

  fun submit(phoneNumber: String) {
    viewModelScope.launch {
      SignIn.create(SignIn.CreateParams.Strategy.PhoneCode(phoneNumber)).flatMap {
        it
          .prepareFirstFactor(SignIn.PrepareFirstFactorParams.PhoneCode())
          .onSuccess { _uiState.value = UiState.Verifying }
          .onFailure {
            // See https://clerk.com/docs/custom-flows/error-handling
            // for more info on error handling
          }
      }
    }
  }

  fun verify(code: String) {
    val inProgressSignIn = Clerk.signIn ?: return
    viewModelScope.launch {
      inProgressSignIn
        .attemptFirstFactor(SignIn.AttemptFirstFactorParams.PhoneCode(code))
        .onSuccess {
          if (it.status == SignIn.Status.COMPLETE) {
            _uiState.value = UiState.Verified
          } else {
            // The user may need to complete further steps
          }
        }
        .onFailure {
          // See https://clerk.com/docs/custom-flows/error-handling
          // for more info on error handling
        }
    }
  }

  sealed interface UiState {
    data object Loading : UiState

    data object Unverified : UiState

    data object Verifying : UiState

    data object Verified : UiState
  }
}
