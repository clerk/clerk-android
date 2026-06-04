package com.clerk.ui.userprofile.security.device

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clerk.api.network.serialization.errorMessage
import com.clerk.api.network.serialization.onFailure
import com.clerk.api.network.serialization.onSuccess
import com.clerk.api.session.Session
import com.clerk.api.user.activeSessions
import com.clerk.ui.core.common.guardUser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal class AllDevicesViewModel : ViewModel() {

  private val _state = MutableStateFlow<State>(State.Idle)
  val state = _state.asStateFlow()

  init {
    activeSessions()
  }

  fun activeSessions() {
    guardUser(userDoesNotExist = { _state.value = State.Error("User does not exist") }) { user ->
      viewModelScope.launch(Dispatchers.IO) {
        _state.value = State.Loading
        user
          .activeSessions()
          .onSuccess {
            val sortedSessions = it.sortedForDeviceDisplay()
            withContext(Dispatchers.Main) { _state.value = State.Success(sortedSessions) }
          }
          .onFailure {
            withContext(Dispatchers.Main) { _state.value = State.Error(it.errorMessage) }
          }
      }
    }
  }

  sealed interface State {
    data object Idle : State

    data object Loading : State

    data class Error(val message: String) : State

    data class Success(val devices: List<Session>) : State
  }
}
