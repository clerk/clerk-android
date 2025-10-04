package com.clerk.ui.signin.alternativemethods

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clerk.api.network.serialization.longErrorMessageOrNull
import com.clerk.api.network.serialization.onFailure
import com.clerk.api.network.serialization.onSuccess
import com.clerk.api.signin.SignIn
import com.clerk.api.sso.OAuthProvider
import com.clerk.api.sso.ResultType
import com.clerk.ui.auth.AuthenticationViewState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

internal class AlternativeMethodsViewModel : ViewModel() {

  private val _state = MutableStateFlow<AuthenticationViewState>(AuthenticationViewState.Idle)
  val state = _state.asStateFlow()

  fun signInWithProvider(provider: OAuthProvider) {
    _state.value = AuthenticationViewState.Loading
    viewModelScope.launch {
      SignIn.authenticateWithRedirect(SignIn.AuthenticateWithRedirectParams.OAuth(provider))
        .onSuccess {
          _state.value =
            when (it.resultType) {
              ResultType.SIGN_IN -> AuthenticationViewState.Success.SignIn(it.signIn!!)
              ResultType.SIGN_UP -> AuthenticationViewState.Success.SignUp(it.signUp!!)
              ResultType.UNKNOWN -> AuthenticationViewState.Error("Unknown result type")
            }
        }
        .onFailure { _state.value = AuthenticationViewState.Error(it.longErrorMessageOrNull) }
    }
  }

  fun resetState() {
    _state.value = AuthenticationViewState.Idle
  }
}
