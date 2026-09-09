package com.clerk.customflows.emailpassword.mfa

import com.clerk.api.Clerk
import com.clerk.api.SignIn
import com.clerk.api.SignInPasswordParams
import com.clerk.api.SignInPasswordParamsCase1
import com.clerk.api.SignInStatus
import com.clerk.api.SignInTOTPVerifyParams
import com.clerk.customflows.CustomFlowFeedback
import com.clerk.customflows.CustomFlowViewModel
import com.clerk.ui.auth.AuthMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class MFASignInViewModel(clerk: Clerk, feedback: CustomFlowFeedback) :
  CustomFlowViewModel(clerk, feedback) {
  private val _uiState = MutableStateFlow<UiState>(UiState.Unverified)
  val uiState = _uiState.asStateFlow()

  init {
    observeSession { signedIn ->
      _uiState.value = if (signedIn) UiState.Verified else UiState.Unverified
    }
  }

  fun submit(email: String, password: String) = runOperation {
    clerk.signIn.password(SignInPasswordParams.Case1(SignInPasswordParamsCase1(password, email)))
    when (clerk.signIn.status) {
      SignInStatus.Complete -> clerk.signIn.finalize()
      SignInStatus.NeedsSecondFactor -> _uiState.value = UiState.NeedsSecondFactor
      else -> feedback.continuation.value = AuthMode.SignIn
    }
  }

  fun verify(code: String) = runOperation {
    clerk.signIn.mfa.verifyTOTP(SignInTOTPVerifyParams(code))
    if (clerk.signIn.status == SignInStatus.Complete) clerk.signIn.finalize()
    else feedback.continuation.value = AuthMode.SignIn
  }

  sealed interface UiState {
    data object Unverified : UiState

    data object Verified : UiState

    data object NeedsSecondFactor : UiState
  }
}
