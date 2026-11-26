package com.clerk.ui.userprofile.totp

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clerk.api.Clerk
import com.clerk.api.network.model.totp.TOTPResource
import com.clerk.api.network.serialization.errorMessage
import com.clerk.api.network.serialization.onFailure
import com.clerk.api.network.serialization.onSuccess
import com.clerk.api.user.createTotp
import com.clerk.ui.core.common.guardUser
import com.clerk.ui.reverification.isReverificationCancelled
import com.clerk.ui.reverification.reverify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

internal class UserProfileMfaTotpViewModel : ViewModel() {

  private val _state = MutableStateFlow<State>(State.Loading)
  val state = _state.asStateFlow()

  init {
    createTOTPResource()
  }

  fun createTOTPResource() {
    _state.value = State.Loading
    guardUser(userDoesNotExist = { _state.value = State.Error("User does not exist") }) { user ->
      viewModelScope.launch {
        Clerk.reverify { user.createTotp() }
          .onSuccess { _state.value = State.Success(totpResource = it) }
          .onFailure {
            _state.value =
              if (it.isReverificationCancelled) State.Cancelled
              else State.Error(message = it.errorMessage)
          }
      }
    }
  }

  fun createTotpIfIdle() {
    if (_state.value == State.Idle) createTOTPResource()
  }

  fun acknowledgeCancellation() {
    _state.compareAndSet(State.Cancelled, State.Idle)
  }

  sealed interface State {
    data object Idle : State

    data object Cancelled : State

    data object Loading : State

    data class Success(val totpResource: TOTPResource) : State

    data class Error(val message: String?) : State
  }
}
