package com.clerk.customflows.forgotpassword.phone

import com.clerk.api.Clerk
import com.clerk.api.SignIn
import com.clerk.api.SignInCreateParams
import com.clerk.api.SignInResetPasswordPhoneCodeVerifyParams
import com.clerk.api.SignInResetPasswordSubmitParams
import com.clerk.api.SignInStatus
import com.clerk.customflows.CustomFlowFeedback
import com.clerk.customflows.CustomFlowViewModel
import com.clerk.ui.auth.AuthMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class ForgotPasswordPhoneViewModel(clerk: Clerk, feedback: CustomFlowFeedback) :
  CustomFlowViewModel(clerk, feedback) {
  private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
  val uiState = _uiState.asStateFlow()

  init {
    observeSession { signedIn ->
      _uiState.value = if (signedIn) UiState.Complete else UiState.SignedOut
    }
  }

  fun createSignIn(phoneNumber: String) = runOperation {
    clerk.signIn.create(SignInCreateParams(identifier = phoneNumber))
    clerk.signIn.resetPasswordPhoneCode.sendCode()
    updateStateFromStatus()
  }

  fun verify(code: String) = runOperation {
    clerk.signIn.resetPasswordPhoneCode.verifyCode(SignInResetPasswordPhoneCodeVerifyParams(code))
    updateStateFromStatus()
  }

  fun setNewPassword(password: String) = runOperation {
    clerk.signIn.resetPasswordPhoneCode.submitPassword(SignInResetPasswordSubmitParams(password))
    updateStateFromStatus()
  }

  private suspend fun updateStateFromStatus() {
    when (clerk.signIn.status) {
      SignInStatus.Complete -> clerk.signIn.finalize()
      SignInStatus.NeedsFirstFactor -> _uiState.value = UiState.NeedsFirstFactor
      SignInStatus.NeedsNewPassword -> _uiState.value = UiState.NeedsNewPassword
      else -> feedback.continuation.value = AuthMode.SignIn
    }
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
