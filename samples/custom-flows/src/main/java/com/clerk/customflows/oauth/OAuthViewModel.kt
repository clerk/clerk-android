package com.clerk.customflows.oauth

import com.clerk.api.Clerk
import com.clerk.api.MobileAuthenticationResult
import com.clerk.api.MobileSSOParams
import com.clerk.api.MobileSSOParamsStart
import com.clerk.api.SignIn
import com.clerk.api.SignInSSOParamsStrategy
import com.clerk.api.SignInStatus
import com.clerk.api.SignUp
import com.clerk.api.SignUpStatus
import com.clerk.customflows.CustomFlowFeedback
import com.clerk.customflows.CustomFlowViewModel
import com.clerk.ui.auth.AuthMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class OAuthViewModel(clerk: Clerk, feedback: CustomFlowFeedback) :
  CustomFlowViewModel(clerk, feedback) {
  private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
  val uiState = _uiState.asStateFlow()

  init {
    observeSession { signedIn ->
      _uiState.value = if (signedIn) UiState.Authenticated else UiState.SignedOut
    }
  }

  fun signInWithOAuth(provider: SignInSSOParamsStrategy) = runOperation {
    when (
      val result =
        clerk.authenticateWithSSO(
          MobileSSOParams(
            strategy = provider,
            start = MobileSSOParamsStart.SignIn,
            transferable = true,
          )
        )
    ) {
      is MobileAuthenticationResult.Case1 -> {
        val signIn = result.value.signIn
        if (signIn.status == SignInStatus.Complete) signIn.finalize()
        else feedback.continuation.value = AuthMode.SignIn
      }
      is MobileAuthenticationResult.Case2 -> {
        val signUp = result.value.signUp
        if (signUp.status == SignUpStatus.Complete) signUp.finalize()
        else feedback.continuation.value = AuthMode.SignUp
      }
    }
  }

  sealed interface UiState {
    data object Loading : UiState

    data object SignedOut : UiState

    data object Authenticated : UiState
  }
}
