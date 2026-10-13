package com.clerk.ui.userprofile.security

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clerk.api.network.serialization.errorMessage
import com.clerk.api.network.serialization.onFailure
import com.clerk.api.network.serialization.onSuccess
import com.clerk.api.session.Session
import com.clerk.api.session.revoke
import com.clerk.api.user.activeSessions
import com.clerk.ui.core.common.guardUser
import com.clerk.ui.userprofile.security.device.sortedForDeviceDisplay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

internal class UserProfileSecurityViewModel : ViewModel() {

  private val _state = MutableStateFlow<State>(State.Idle)
  val state = _state.asStateFlow()

  init {
    loadSessions()
  }

  fun loadSessions() {
    _state.value = State.Loading
    guardUser({}) { user ->
      viewModelScope.launch {
        user
          .activeSessions()
          .onSuccess { _state.value = State.Success(it.sortedForDeviceDisplay()) }
          .onFailure { _state.value = State.Error(it.errorMessage) }
      }
    }
  }

  fun signOut(session: Session, onError: (String?) -> Unit) {
    viewModelScope.launch {
      session
        .revoke()
        .onSuccess {
          val current = _state.value
          if (current is State.Success) {
            _state.value = State.Success(current.sessions.filterNot { it.id == session.id })
          }
        }
        .onFailure { onError(it.errorMessage) }
    }
  }

  sealed interface State {
    data object Idle : State

    data object Loading : State

    data class Error(val message: String) : State

    data class Success(val sessions: List<Session>) : State
  }
}
