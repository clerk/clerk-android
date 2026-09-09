package com.clerk.customflows.emailpassword.signin

import com.clerk.api.Clerk
import com.clerk.api.SignIn
import com.clerk.api.SignInPasswordParams
import com.clerk.api.SignInPasswordParamsCase1
import com.clerk.api.SignInStatus
import com.clerk.customflows.CustomFlowFeedback
import com.clerk.customflows.CustomFlowViewModel
import com.clerk.ui.auth.AuthMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class EmailPasswordSignInViewModel(clerk: Clerk, feedback: CustomFlowFeedback) :
  CustomFlowViewModel(clerk, feedback) {
  private val _uiState =
    MutableStateFlow<EmailPasswordSignInUiState>(EmailPasswordSignInUiState.Loading)
  val uiState = _uiState.asStateFlow()

  init {
    observeSession { signedIn ->
      _uiState.value =
        if (signedIn) EmailPasswordSignInUiState.SignedIn else EmailPasswordSignInUiState.SignedOut
    }
  }

  fun submit(email: String, password: String) = runOperation {
    clerk.signIn.password(SignInPasswordParams.Case1(SignInPasswordParamsCase1(password, email)))
    if (clerk.signIn.status == SignInStatus.Complete) clerk.signIn.finalize()
    else feedback.continuation.value = AuthMode.SignIn
  }

  sealed interface EmailPasswordSignInUiState {
    data object Loading : EmailPasswordSignInUiState

    data object SignedOut : EmailPasswordSignInUiState

    data object SignedIn : EmailPasswordSignInUiState
  }
}
