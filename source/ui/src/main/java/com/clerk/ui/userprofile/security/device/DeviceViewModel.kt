package com.clerk.ui.userprofile.security.device

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clerk.api.Clerk
import com.clerk.api.network.serialization.longErrorMessageOrNull
import com.clerk.api.network.serialization.onFailure
import com.clerk.api.network.serialization.onSuccess
import com.clerk.api.session.Session
import com.clerk.api.session.revoke
import com.clerk.api.user.allSessions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal class DeviceViewModel : ViewModel() {
  private val _state = MutableStateFlow<State>(State.Idle)
  val state = _state.asStateFlow()

  fun signOut(session: Session) {
    _state.value = State.Loading
    viewModelScope.launch(Dispatchers.IO) {
      session
        .revoke()
        .onSuccess {
          Clerk.user?.allSessions()
          withContext(Dispatchers.Main) { _state.value = State.Success }
        }
        .onFailure {
          withContext(Dispatchers.Main) { _state.value = State.Error(it.longErrorMessageOrNull) }
        }
    }
  }

  internal sealed interface State {
    data object Idle : State

    data object Loading : State

    data object Success : State

    data class Error(val message: String?) : State
  }
}
