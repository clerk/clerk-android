package com.clerk.ui.signin.password.reset

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clerk.api.Clerk
import com.clerk.api.network.model.client.Client
import com.clerk.api.network.serialization.ClerkResult
import com.clerk.api.network.serialization.errorMessage
import com.clerk.api.network.serialization.onFailure
import com.clerk.api.network.serialization.onSuccess
import com.clerk.api.session.Session
import com.clerk.api.session.SessionTaskKey
import com.clerk.api.session.parsedKey
import com.clerk.api.signin.resetPassword
import com.clerk.api.user.User
import com.clerk.api.user.updatePassword
import com.clerk.ui.auth.AuthenticationViewState
import com.clerk.ui.auth.guardSignIn
import com.clerk.ui.core.common.guardUser
import com.clerk.ui.core.log.ClerkLog
import com.clerk.ui.reverification.isReverificationCancelled
import com.clerk.ui.reverification.reverify
import com.clerk.ui.reverification.withRetryFlag
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
        val params =
          User.UpdatePasswordParams(
            currentPassword = null,
            newPassword = newPassword,
            signOutOfOtherSessions = signOutOtherSessions,
          )
        updatePasswordForResetTask(user, params)
          .onSuccess { refreshClientForSessionTaskCompletion() }
          .onFailure {
            if (it.isReverificationCancelled) {
              withContext(Dispatchers.Main) { _state.value = AuthenticationViewState.Idle }
              return@onFailure
            }
            ClerkLog.e("ResetPasswordViewModel, ${it.errorMessage}")
            withContext(Dispatchers.Main) {
              _state.value = AuthenticationViewState.Error(it.errorMessage)
            }
          }
      }
    }
  }

  private suspend fun updatePasswordForResetTask(user: User, params: User.UpdatePasswordParams) =
    Clerk.session.hasResetPasswordTask.let { taskWasPending ->
      Clerk.reverify(
        withRetryFlag { isRetry ->
          if (isRetry && taskWasPending && !Clerk.session.hasResetPasswordTask) {
            ClerkResult.success(user)
          } else {
            user.updatePassword(params)
          }
        }
      )
    }

  private val Session?.hasResetPasswordTask: Boolean
    get() =
      this != null &&
        (currentTask?.parsedKey == SessionTaskKey.RESET_PASSWORD ||
          tasks.any { it.parsedKey == SessionTaskKey.RESET_PASSWORD })

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
