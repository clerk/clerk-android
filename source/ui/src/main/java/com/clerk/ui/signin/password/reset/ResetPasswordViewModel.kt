package com.clerk.ui.signin.password.reset

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clerk.api.Clerk
import com.clerk.api.log.ClerkLog
import com.clerk.api.network.serialization.longErrorMessageOrNull
import com.clerk.api.network.serialization.onFailure
import com.clerk.api.network.serialization.onSuccess
import com.clerk.api.signin.SignIn
import com.clerk.api.signin.resetPassword
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ResetPasswordViewModel : ViewModel() {

  private val _state = MutableStateFlow<State>(State.Idle)
  val state = _state.asStateFlow()

  fun setNewPassword(newPassword: String, signOutOtherSessions: Boolean) {
    val signIn = Clerk.signIn ?: return
    _state.value = State.Loading
    viewModelScope.launch {
      signIn
        .resetPassword(
          SignIn.ResetPasswordParams(
            password = newPassword,
            signOutOfOtherSessions = signOutOtherSessions,
          )
        )
        .onSuccess { _state.value = State.Success }
        .onFailure {
          ClerkLog.e("ResetPasswordViewModel, ${it.longErrorMessageOrNull}")
          _state.value = State.Error(it.longErrorMessageOrNull ?: "Something went wrong")
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
