package com.clerk.customflows.emailpassword.signup

import com.clerk.api.Clerk
import com.clerk.api.SignUp
import com.clerk.api.SignUpCreateParams
import com.clerk.api.SignUpEmailCodeVerifyParams
import com.clerk.api.SignUpStatus
import com.clerk.customflows.CustomFlowFeedback
import com.clerk.customflows.CustomFlowViewModel
import com.clerk.ui.auth.AuthMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class EmailPasswordSignUpViewModel(clerk: Clerk, feedback: CustomFlowFeedback) :
  CustomFlowViewModel(clerk, feedback) {
  private val _uiState =
    MutableStateFlow<EmailPasswordSignUpUiState>(EmailPasswordSignUpUiState.Loading)
  val uiState = _uiState.asStateFlow()

  init {
    observeSession { signedIn ->
      _uiState.value =
        if (signedIn) EmailPasswordSignUpUiState.Verified else EmailPasswordSignUpUiState.Unverified
    }
  }

  fun submit(email: String, password: String) = runOperation {
    clerk.signUp.create(SignUpCreateParams(emailAddress = email, password = password))
    clerk.signUp.verifications.sendEmailCode()
    _uiState.value = EmailPasswordSignUpUiState.Verifying
  }

  fun verify(code: String) = runOperation {
    clerk.signUp.verifications.verifyEmailCode(SignUpEmailCodeVerifyParams(code))
    if (clerk.signUp.status == SignUpStatus.Complete) clerk.signUp.finalize()
    else feedback.continuation.value = AuthMode.SignUp
  }

  sealed interface EmailPasswordSignUpUiState {
    data object Loading : EmailPasswordSignUpUiState

    data object Unverified : EmailPasswordSignUpUiState

    data object Verifying : EmailPasswordSignUpUiState

    data object Verified : EmailPasswordSignUpUiState
  }
}
