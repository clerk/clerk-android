package com.clerk.ui.userprofile.security.passkey

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clerk.api.Clerk
import com.clerk.api.credentials.resolvedCredentialFlowMessage
import com.clerk.api.credentials.shouldSuppressCredentialFlowError
import com.clerk.api.network.serialization.errorMessage
import com.clerk.api.network.serialization.onFailure
import com.clerk.api.network.serialization.onSuccess
import com.clerk.api.passkeys.Passkey
import com.clerk.api.passkeys.delete
import com.clerk.api.user.createPasskey
import com.clerk.ui.core.common.guardUser
import com.clerk.ui.core.log.ClerkLog
import com.clerk.ui.reverification.isReverificationCancelled
import com.clerk.ui.reverification.reverify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

internal class UserProfilePasskeyViewModel : ViewModel() {

  private val _state = MutableStateFlow<State>(State.Idle)
  val state = _state.asStateFlow()

  fun deletePasskey(passkey: Passkey) {
    _state.value = State.Loading
    viewModelScope.launch {
      Clerk.reverify { passkey.delete() }
        .onFailure {
          _state.value =
            if (it.isReverificationCancelled) State.Idle else State.Error(it.errorMessage)
        }
        .onSuccess { _state.value = State.Success }
    }
  }

  fun createPasskey() {
    guardUser({ _state.value = State.Error("User does not exist") }) { user ->
      viewModelScope.launch {
        Clerk.reverify { user.createPasskey() }
          .onSuccess { _state.value = State.Success }
          .onFailure {
            if (it.isReverificationCancelled) return@onFailure
            ClerkLog.e("Failed to create passkey: ${it.resolvedCredentialFlowMessage}")
            _state.value =
              if (it.shouldSuppressCredentialFlowError) {
                State.Idle
              } else {
                State.Error(it.resolvedCredentialFlowMessage)
              }
          }
      }
    }
  }

  fun clearError(reported: State.Error) {
    _state.compareAndSet(reported, State.Idle)
  }

  sealed interface State {
    data object Idle : State

    data object Loading : State

    data class Error(val message: String) : State

    data object Success : State
  }
}
