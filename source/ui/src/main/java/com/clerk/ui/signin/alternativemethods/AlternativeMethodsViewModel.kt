package com.clerk.ui.signin.alternativemethods

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clerk.api.Clerk
import com.clerk.api.network.serialization.errorMessage
import com.clerk.api.network.serialization.onFailure
import com.clerk.api.network.serialization.onSuccess
import com.clerk.api.sso.OAuthProvider
import com.clerk.api.sso.ResultType
import com.clerk.ui.auth.AuthenticationViewState
import com.clerk.ui.auth.isSSOCancellation
import com.clerk.ui.signin.authenticateWithRedirect
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

internal class AlternativeMethodsViewModel : ViewModel() {

  private val _state = MutableStateFlow<AuthenticationViewState>(AuthenticationViewState.Idle)
  val state = _state.asStateFlow()

  fun signInWithProvider(provider: OAuthProvider, transferable: Boolean = true) {
    _state.value = AuthenticationViewState.Loading
    viewModelScope.launch {
      authenticateWithRedirect(
          signIn = Clerk.auth.currentSignIn,
          provider = provider,
          transferable = transferable,
        )
        .onSuccess {
          _state.value =
            when (it.resultType) {
              ResultType.SIGN_IN -> AuthenticationViewState.Success.SignIn(it.signIn!!)
              ResultType.SIGN_UP -> AuthenticationViewState.Success.SignUp(it.signUp!!)
              ResultType.UNKNOWN -> AuthenticationViewState.Error("Unknown result type")
            }
        }
        .onFailure {
          _state.value =
            if (it.isSSOCancellation) {
              AuthenticationViewState.Idle
            } else {
              AuthenticationViewState.Error(it.errorMessage)
            }
        }
    }
  }

  fun resetState() {
    _state.value = AuthenticationViewState.Idle
  }
}
