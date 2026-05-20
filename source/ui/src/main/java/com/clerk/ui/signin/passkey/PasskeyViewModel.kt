package com.clerk.ui.signin.passkey

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clerk.api.credentials.resolvedCredentialFlowMessage
import com.clerk.api.credentials.shouldSuppressCredentialFlowError
import com.clerk.api.log.ClerkLog
import com.clerk.api.network.serialization.onFailure
import com.clerk.api.network.serialization.onSuccess
import com.clerk.api.signin.SignIn
import com.clerk.ui.auth.AuthenticationViewState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

internal class PasskeyViewModel : ViewModel() {

  private val _state = MutableStateFlow<AuthenticationViewState>(AuthenticationViewState.Idle)
  val state = _state.asStateFlow()

  fun authenticate() {
    _state.value = AuthenticationViewState.Loading
    viewModelScope.launch {
      SignIn.authenticateWithGoogleCredential(listOf(SignIn.CredentialType.PASSKEY))
        .onSuccess { _state.value = AuthenticationViewState.Success.SignIn(it) }
        .onFailure {
          ClerkLog.e("Passkey authentication failed: $it")
          _state.value =
            if (it.shouldSuppressCredentialFlowError) {
              AuthenticationViewState.Idle
            } else {
              AuthenticationViewState.Error(it.resolvedCredentialFlowMessage)
            }
        }
    }
  }

  fun resetState() {
    _state.value = AuthenticationViewState.Idle
  }
}
