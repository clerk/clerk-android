package com.clerk.ui.userprofile.totp

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clerk.api.*
import com.clerk.ui.core.common.displayMessage
import com.clerk.ui.core.common.runUiOperation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

internal class UserProfileMfaTotpViewModel(private val clerk: Clerk) : ViewModel() {

  private val _state = MutableStateFlow<State>(State.Loading)
  val state = _state.asStateFlow()

  init {
    createTOTP()
  }

  fun createTOTP() {
    _state.value = State.Loading
    viewModelScope.launch {
      runUiOperation {
          val user = clerk.user ?: throw CoreException("no_user", "User does not exist")
          user.createTOTP()
        }
        .onSuccess { _state.value = State.Success(it) }
        .onFailure { _state.value = State.Error(it.displayMessage) }
    }
  }

  sealed interface State {
    data object Idle : State

    data object Loading : State

    data class Success(val totpResource: TOTP) : State

    data class Error(val message: String?) : State
  }
}
