package com.clerk.ui.userprofile.security

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clerk.api.*
import com.clerk.ui.core.common.displayMessage
import com.clerk.ui.core.common.runUiOperation
import com.clerk.ui.userprofile.security.device.sortedForDeviceDisplay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

internal class UserProfileSecurityViewModel(private val clerk: Clerk) : ViewModel() {

  private val _state = MutableStateFlow<State>(State.Idle)
  val state = _state.asStateFlow()

  init {
    loadSessions()
  }

  fun loadSessions() {
    _state.value = State.Loading
    viewModelScope.launch {
      runUiOperation {
          val user = clerk.user ?: throw CoreException("no_user", "User does not exist")
          user.getSessions().sortedForDeviceDisplay(clerk.session?.id)
        }
        .onSuccess { _state.value = State.Success(it) }
        .onFailure { _state.value = State.Error(it.displayMessage) }
    }
  }

  sealed interface State {
    data object Idle : State

    data object Loading : State

    data class Error(val message: String) : State

    data class Success(val sessions: List<SessionWithActivities>) : State
  }
}
