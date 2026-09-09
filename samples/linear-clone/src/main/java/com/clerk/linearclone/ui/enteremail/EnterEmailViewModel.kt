package com.clerk.linearclone.ui.enteremail

import com.clerk.api.Clerk
import com.clerk.api.SignInEmailCodeSendCodeParamsCase1
import com.clerk.api.SignInEmailCodeSendParams
import com.clerk.linearclone.LinearFeedback
import com.clerk.linearclone.LinearViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class EnterEmailViewModel(clerk: Clerk, feedback: LinearFeedback) :
  LinearViewModel(clerk, feedback) {
  private val _uiState = MutableStateFlow<UiState>(UiState.SignedOut)
  val uiState = _uiState.asStateFlow()

  fun prepareEmailVerification(email: String) = runOperation {
    clerk.signIn.emailCode.sendCode(
      SignInEmailCodeSendParams.Case1(SignInEmailCodeSendCodeParamsCase1(emailAddress = email))
    )
    _uiState.value = UiState.NeedsEmailCode(email)
  }

  fun didNavigate() {
    _uiState.value = UiState.SignedOut
  }

  sealed interface UiState {
    data object SignedOut : UiState

    data class NeedsEmailCode(val email: String) : UiState
  }
}
