package com.clerk.ui.userprofile.security.delete

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clerk.api.log.ClerkLog
import com.clerk.api.network.serialization.errorMessage
import com.clerk.api.network.serialization.onFailure
import com.clerk.api.network.serialization.onSuccess
import com.clerk.api.user.delete
import com.clerk.ui.core.common.guardUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class DeleteAccountViewModel : ViewModel() {

  private val _state = MutableStateFlow<State>(State.Idle)
  val state = _state.asStateFlow()

  fun deleteAccount() {
    guardUser({}) { user ->
      _state.value = State.Loading
      viewModelScope.launch {
        user
          .delete()
          .onFailure {
            ClerkLog.e("Failed to delete account: ${it.errorMessage}")
            _state.value = State.Error(it.errorMessage)
          }
          .onSuccess { _state.value = State.Success }
      }
    }
  }

  sealed interface State {
    object Idle : State

    object Loading : State

    object Success : State

    data class Error(val message: String) : State
  }
}
