package com.clerk.ui.signin.password.forgot

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clerk.api.network.serialization.longErrorMessageOrNull
import com.clerk.api.network.serialization.onFailure
import com.clerk.api.network.serialization.onSuccess
import com.clerk.api.signin.SignIn
import com.clerk.api.sso.OAuthProvider
import com.clerk.api.sso.ResultType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

internal class ForgotPasswordViewModel : ViewModel() {

  private val _state = MutableStateFlow<State>(State.Idle)
  val state = _state.asStateFlow()

  fun signInWithProvider(provider: OAuthProvider) {
    _state.value = State.Loading
    viewModelScope.launch {
      SignIn.authenticateWithRedirect(SignIn.AuthenticateWithRedirectParams.OAuth(provider))
        .onSuccess {
          if (it.resultType == ResultType.SIGN_IN) {
            _state.value = State.SignInSuccess
          } else {
            _state.value = State.SignUpSuccess
          }
        }
        .onFailure { _state.value = State.Error(it.longErrorMessageOrNull) }
    }
  }

  sealed interface State {
    data object Idle : State

    data object Loading : State

    data object SignInSuccess : State

    data object SignUpSuccess : State

    data class Error(val message: String?) : State
  }
}
