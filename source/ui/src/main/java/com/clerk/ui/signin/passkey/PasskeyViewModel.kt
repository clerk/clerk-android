package com.clerk.ui.signin.passkey

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clerk.api.log.ClerkLog
import com.clerk.api.network.serialization.longErrorMessageOrNull
import com.clerk.api.network.serialization.onFailure
import com.clerk.api.network.serialization.onSuccess
import com.clerk.api.signin.SignIn
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

internal class PasskeyViewModel : ViewModel() {

  private val _state = MutableStateFlow<AuthenticateState>(AuthenticateState.Idle)
  val state = _state.asStateFlow()

  fun authenticate() {
    _state.value = AuthenticateState.Verifying
    viewModelScope.launch {
      SignIn.authenticateWithGoogleCredential(listOf(SignIn.CredentialType.PASSKEY))
        .onSuccess { _state.value = AuthenticateState.Success }
        .onFailure {
          ClerkLog.e("Passkey authentication failed: $it")
          _state.value = AuthenticateState.Failed(it.longErrorMessageOrNull)
        }
    }
  }

  sealed interface AuthenticateState {
    data object Idle : AuthenticateState

    data object Verifying : AuthenticateState

    data object Success : AuthenticateState

    data class Failed(val message: String?) : AuthenticateState
  }
}
