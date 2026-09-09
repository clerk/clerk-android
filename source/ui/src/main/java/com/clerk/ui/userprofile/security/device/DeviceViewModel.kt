package com.clerk.ui.userprofile.security.device

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clerk.api.*
import com.clerk.ui.core.common.displayMessage
import com.clerk.ui.core.common.runUiOperation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

internal class DeviceViewModel(private val clerk: Clerk) : ViewModel() {
  private val _state = MutableStateFlow<State>(State.Idle)
  val state = _state.asStateFlow()

  fun signOut(session: SessionWithActivities) {
    _state.value = State.Loading
    viewModelScope.launch {
      runUiOperation { session.revoke() }
        .onSuccess { _state.value = State.Success }
        .onFailure { _state.value = State.Error(it.displayMessage) }
    }
  }

  internal sealed interface State {
    data object Idle : State

    data object Loading : State

    data object Success : State

    data class Error(val message: String?) : State
  }
}
