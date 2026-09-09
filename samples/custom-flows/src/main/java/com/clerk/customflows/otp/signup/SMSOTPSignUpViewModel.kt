package com.clerk.customflows.otp.signup

import com.clerk.api.Clerk
import com.clerk.api.SignUp
import com.clerk.api.SignUpCreateParams
import com.clerk.api.SignUpPhoneCodeVerifyParams
import com.clerk.api.SignUpStatus
import com.clerk.customflows.CustomFlowFeedback
import com.clerk.customflows.CustomFlowViewModel
import com.clerk.ui.auth.AuthMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class SMSOTPSignUpViewModel(clerk: Clerk, feedback: CustomFlowFeedback) :
  CustomFlowViewModel(clerk, feedback) {
  private val _uiState = MutableStateFlow<UiState>(UiState.Unverified)
  val uiState = _uiState.asStateFlow()

  init {
    observeSession { signedIn ->
      _uiState.value = if (signedIn) UiState.Verified else UiState.Unverified
    }
  }

  fun submit(phoneNumber: String) = runOperation {
    clerk.signUp.create(SignUpCreateParams(phoneNumber = phoneNumber))
    clerk.signUp.verifications.sendPhoneCode()
    _uiState.value = UiState.Verifying
  }

  fun verify(code: String) = runOperation {
    clerk.signUp.verifications.verifyPhoneCode(SignUpPhoneCodeVerifyParams(code))
    if (clerk.signUp.status == SignUpStatus.Complete) clerk.signUp.finalize()
    else feedback.continuation.value = AuthMode.SignUp
  }

  sealed interface UiState {
    data object Loading : UiState

    data object Unverified : UiState

    data object Verifying : UiState

    data object Verified : UiState
  }
}
