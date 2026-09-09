package com.clerk.ui.signin.alternativemethods

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

internal class AlternativeMethodsViewModel(private val clerk: Clerk) : ViewModel() {
  private val _state = MutableStateFlow<AuthenticationViewState>(AuthenticationViewState.Idle)
  val state = _state.asStateFlow()

  fun signInWithProvider(
    provider: OAuthProvider,
    transferable: Boolean = true,
    signIn: SignIn? = clerk.signIn.takeIf { it.id != null },
  ) {
    if (signIn == null) {
      _state.value = AuthenticationViewState.NotStarted
      return
    }
    _state.value = AuthenticationViewState.Loading
    viewModelScope.launch {
      runUiOperation { authenticateWithRedirect(clerk, provider, transferable) }
        .onSuccess { result ->
          _state.value =
            when (result) {
              is MobileAuthenticationResult.Case1 ->
                AuthenticationViewState.Success.SignIn(result.value.signIn)
              is MobileAuthenticationResult.Case2 ->
                AuthenticationViewState.Success.SignUp(result.value.signUp)
            }
        }
        .onFailure {
          _state.value =
            if (it.isSSOCancellation) AuthenticationViewState.Idle
            else AuthenticationViewState.Error(it.displayMessage)
        }
    }
  }

  fun resetState() {
    _state.value = AuthenticationViewState.Idle
  }
}
