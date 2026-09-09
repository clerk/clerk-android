package com.clerk.ui.signin.password.forgot

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clerk.api.*
import com.clerk.ui.auth.*
import com.clerk.ui.core.common.displayMessage
import com.clerk.ui.core.common.runUiOperation
import com.clerk.ui.signin.authenticateWithRedirect
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

internal class ForgotPasswordViewModel(private val clerk: Clerk) : ViewModel() {
  private val _state = MutableStateFlow<ResetPasswordViewState>(ResetPasswordViewState.Idle)
  val state = _state.asStateFlow()

  fun signInWithProvider(
    provider: OAuthProvider,
    transferable: Boolean = true,
    signIn: SignIn? = clerk.signIn.takeIf { it.id != null },
  ) {
    if (signIn == null) {
      _state.value = ResetPasswordViewState.NotStarted
      return
    }
    _state.value = ResetPasswordViewState.Loading
    viewModelScope.launch {
      runUiOperation { authenticateWithRedirect(clerk, provider, transferable) }
        .onSuccess { result ->
          _state.value =
            when (result) {
              is MobileAuthenticationResult.Case1 ->
                ResetPasswordViewState.Success.SignIn(result.value.signIn)
              is MobileAuthenticationResult.Case2 ->
                ResetPasswordViewState.Success.SignUp(result.value.signUp)
            }
        }
        .onFailure {
          _state.value =
            if (it.isSSOCancellation) ResetPasswordViewState.Idle
            else ResetPasswordViewState.Error(it.displayMessage)
        }
    }
  }

  fun resetPassword() {
    val factor = clerk.signIn.resetPasswordFactor
    _state.value =
      if (clerk.signIn.id == null || factor == null) ResetPasswordViewState.NotStarted
      else ResetPasswordViewState.ResetFactor(factor)
  }

  fun resetState() {
    _state.value = ResetPasswordViewState.Idle
  }
}

internal sealed interface ResetPasswordViewState {
  data object NotStarted : ResetPasswordViewState

  data object Idle : ResetPasswordViewState

  data object Loading : ResetPasswordViewState

  sealed interface Success : ResetPasswordViewState {
    data class SignIn(val signIn: com.clerk.api.SignIn) : Success

    data class SignUp(val signUp: com.clerk.api.SignUp) : Success
  }

  data class Error(val message: String?) : ResetPasswordViewState

  data class ResetFactor(val factor: FactorSelection) : ResetPasswordViewState
}
