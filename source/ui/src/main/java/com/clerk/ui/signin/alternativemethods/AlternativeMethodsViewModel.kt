package com.clerk.ui.signin.alternativemethods

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clerk.api.network.serialization.longErrorMessageOrNull
import com.clerk.api.network.serialization.onFailure
import com.clerk.api.network.serialization.onSuccess
import com.clerk.api.signin.SignIn
import com.clerk.api.sso.OAuthProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

internal class AlternativeMethodsViewModel : ViewModel() {

  private val _state = MutableStateFlow<AuthenticationState>(AuthenticationState.Idle)
  val state = _state.asStateFlow()

  fun signInWithProvider(provider: OAuthProvider) {
    _state.value = AuthenticationState.Loading
    viewModelScope.launch {
      SignIn.authenticateWithRedirect(SignIn.AuthenticateWithRedirectParams.OAuth(provider))
        .onSuccess { _state.value = AuthenticationState.Success }
        .onFailure { _state.value = AuthenticationState.Error(it.longErrorMessageOrNull) }
    }
  }

  sealed interface AuthenticationState {
    data object Idle : AuthenticationState

    data object Loading : AuthenticationState

    data object Success : AuthenticationState

    data class Error(val message: String?) : AuthenticationState
  }
}
