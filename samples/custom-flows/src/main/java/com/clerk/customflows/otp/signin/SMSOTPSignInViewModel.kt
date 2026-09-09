package com.clerk.customflows.otp.signin

import com.clerk.api.Clerk
import com.clerk.api.SignIn
import com.clerk.api.SignInPhoneCodeSendCodeParamsCase1
import com.clerk.api.SignInPhoneCodeSendParams
import com.clerk.api.SignInPhoneCodeVerifyParams
import com.clerk.api.SignInStatus
import com.clerk.customflows.CustomFlowFeedback
import com.clerk.customflows.CustomFlowViewModel
import com.clerk.ui.auth.AuthMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class SMSOTPSignInViewModel(clerk: Clerk, feedback: CustomFlowFeedback) :
  CustomFlowViewModel(clerk, feedback) {
  private val _uiState = MutableStateFlow<UiState>(UiState.Unverified)
  val uiState = _uiState.asStateFlow()

  init {
    observeSession { signedIn ->
      _uiState.value = if (signedIn) UiState.Verified else UiState.Unverified
    }
  }

  fun submit(phoneNumber: String) = runOperation {
    clerk.signIn.phoneCode.sendCode(
      SignInPhoneCodeSendParams.Case1(SignInPhoneCodeSendCodeParamsCase1(phoneNumber = phoneNumber))
    )
    _uiState.value = UiState.Verifying
  }

  fun verify(code: String) = runOperation {
    clerk.signIn.phoneCode.verifyCode(SignInPhoneCodeVerifyParams(code))
    if (clerk.signIn.status == SignInStatus.Complete) clerk.signIn.finalize()
    else feedback.continuation.value = AuthMode.SignIn
  }

  sealed interface UiState {
    data object Loading : UiState

    data object Unverified : UiState

    data object Verifying : UiState

    data object Verified : UiState
  }
}
