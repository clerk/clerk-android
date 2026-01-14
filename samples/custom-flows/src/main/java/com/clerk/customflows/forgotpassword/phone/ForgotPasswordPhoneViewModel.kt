package com.clerk.customflows.forgotpassword.phone

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clerk.api.Clerk
import com.clerk.api.network.serialization.errorMessage
import com.clerk.api.network.serialization.onFailure
import com.clerk.api.network.serialization.onSuccess
import com.clerk.api.signin.SignIn
import com.clerk.api.signin.resetPassword
import com.clerk.api.signin.sendResetPasswordCode
import com.clerk.api.signin.verifyCode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.launch

class ForgotPasswordPhoneViewModel : ViewModel() {
  private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
  val uiState = _uiState.asStateFlow()

  init {
    combine(Clerk.isInitialized, Clerk.userFlow) { isInitialized, user ->
        _uiState.value =
          when {
            !isInitialized -> UiState.Loading
            user != null -> UiState.Complete
            else -> UiState.SignedOut
          }
      }
      .launchIn(viewModelScope)
  }

  fun createSignIn(phoneNumber: String) {
    viewModelScope.launch {
      Clerk.auth
        .signIn { phone = phoneNumber }
        .onSuccess { signIn ->
          signIn
            .sendResetPasswordCode { phone = phoneNumber }
            .onSuccess { updateStateFromStatus(it.status) }
            .onFailure {
              Log.e(ForgotPasswordPhoneViewModel::class.simpleName, it.errorMessage, it.throwable)
            }
        }
        .onFailure {
          // See https://clerk.com/docs/custom-flows/error-handling
          // for more info on error handling
          Log.e(ForgotPasswordPhoneViewModel::class.simpleName, it.errorMessage, it.throwable)
        }
    }
  }

  fun verify(code: String) {
    val inProgressSignIn = Clerk.auth.signIn ?: return
    viewModelScope.launch {
      inProgressSignIn
        .verifyCode(code)
        .onSuccess { updateStateFromStatus(it.status) }
        .onFailure {
          // See https://clerk.com/docs/custom-flows/error-handling
          // for more info on error handling
          Log.e(ForgotPasswordPhoneViewModel::class.simpleName, it.errorMessage, it.throwable)
        }
    }
  }

  fun setNewPassword(password: String) {
    val inProgressSignIn = Clerk.auth.signIn ?: return
    viewModelScope.launch {
      inProgressSignIn
        .resetPassword(password)
        .onSuccess { updateStateFromStatus(it.status) }
        .onFailure {
          // See https://clerk.com/docs/custom-flows/error-handling
          // for more info on error handling
          Log.e(ForgotPasswordPhoneViewModel::class.simpleName, it.errorMessage, it.throwable)
        }
    }
  }

  fun updateStateFromStatus(status: SignIn.Status) {
    val state =
      when (status) {
        SignIn.Status.COMPLETE -> UiState.Complete
        SignIn.Status.NEEDS_FIRST_FACTOR -> UiState.NeedsFirstFactor
        SignIn.Status.NEEDS_SECOND_FACTOR -> UiState.NeedsSecondFactor
        SignIn.Status.NEEDS_NEW_PASSWORD -> UiState.NeedsNewPassword
        else -> {
          UiState.SignedOut
        }
      }

    _uiState.value = state
  }

  sealed interface UiState {
    data object Loading : UiState

    data object SignedOut : UiState

    data object NeedsFirstFactor : UiState

    data object NeedsSecondFactor : UiState

    data object NeedsNewPassword : UiState

    data object Complete : UiState
  }
}
