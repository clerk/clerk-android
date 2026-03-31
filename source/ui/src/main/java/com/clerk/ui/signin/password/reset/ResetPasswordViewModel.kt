package com.clerk.ui.signin.password.reset

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clerk.api.log.ClerkLog
import com.clerk.api.network.model.client.Client
import com.clerk.api.network.serialization.errorMessage
import com.clerk.api.network.serialization.onFailure
import com.clerk.api.network.serialization.onSuccess
import com.clerk.api.signin.resetPassword
import com.clerk.api.user.User
import com.clerk.api.user.updatePassword
import com.clerk.ui.auth.AuthenticationViewState
import com.clerk.ui.auth.guardSignIn
import com.clerk.ui.core.common.guardUser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal class ResetPasswordViewModel : ViewModel() {

  private val _state = MutableStateFlow<AuthenticationViewState>(AuthenticationViewState.Idle)
  val state = _state.asStateFlow()

  fun setNewPassword(newPassword: String, signOutOtherSessions: Boolean) =
    guardSignIn(state = _state) { signIn ->
      _state.value = AuthenticationViewState.Loading
      viewModelScope.launch(Dispatchers.IO) {
        signIn
          .resetPassword(newPassword = newPassword, signOutOfOtherSessions = signOutOtherSessions)
          .onSuccess {
            withContext(Dispatchers.Main) {
              _state.value = AuthenticationViewState.Success.SignIn(it)
            }
          }
          .onFailure {
            ClerkLog.e("ResetPasswordViewModel, ${it.errorMessage}")
            withContext(Dispatchers.Main) {
              _state.value = AuthenticationViewState.Error(it.errorMessage)
            }
          }
      }
    }

  fun completeSessionTask(newPassword: String, signOutOtherSessions: Boolean) {
    guardUser(
      userDoesNotExist = { _state.value = AuthenticationViewState.Error("User does not exist") }
    ) { user ->
      _state.value = AuthenticationViewState.Loading
      viewModelScope.launch(Dispatchers.IO) {
        user
          .updatePassword(
            User.UpdatePasswordParams(
              currentPassword = null,
              newPassword = newPassword,
              signOutOfOtherSessions = signOutOtherSessions,
            )
          )
          .onSuccess { refreshClientForSessionTaskCompletion() }
          .onFailure {
            ClerkLog.e("ResetPasswordViewModel, ${it.errorMessage}")
            withContext(Dispatchers.Main) {
              _state.value = AuthenticationViewState.Error(it.errorMessage)
            }
          }
      }
    }
  }

  private suspend fun refreshClientForSessionTaskCompletion() {
    Client.get()
      .onSuccess {
        withContext(Dispatchers.Main) {
          _state.value =
            AuthenticationViewState.Success.SessionTaskComplete(
              it.sessions.firstOrNull { session -> session.id == it.lastActiveSessionId }
            )
        }
      }
      .onFailure {
        ClerkLog.e("ResetPasswordViewModel, ${it.errorMessage}")
        withContext(Dispatchers.Main) {
          _state.value = AuthenticationViewState.Error(it.errorMessage)
        }
      }
  }

  fun resetState() {
    _state.value = AuthenticationViewState.Idle
  }
}
