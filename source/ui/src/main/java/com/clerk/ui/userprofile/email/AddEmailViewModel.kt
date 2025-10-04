package com.clerk.ui.userprofile.email

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clerk.api.network.serialization.longErrorMessageOrNull
import com.clerk.api.network.serialization.onFailure
import com.clerk.api.network.serialization.onSuccess
import com.clerk.api.user.createEmailAddress
import com.clerk.ui.core.common.guardUser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

internal class AddEmailViewModel : ViewModel() {

  private val _state = MutableStateFlow<State>(State.Idle)
  val state = _state.asStateFlow()

  fun addEmail(email: String) {
    _state.value = State.Loading
    guardUser(userDoesNotExist = { _state.value = State.Error("No current user found") }) { user ->
      viewModelScope.launch(Dispatchers.IO) {
        user
          .createEmailAddress(email)
          .onSuccess { _state.value = State.Success }
          .onFailure { _state.value = State.Error(it.longErrorMessageOrNull) }
      }
    }
  }

  sealed interface State {
    data object Idle : State

    data object Loading : State

    data object Success : State

    data class Error(val message: String?) : State
  }
}
