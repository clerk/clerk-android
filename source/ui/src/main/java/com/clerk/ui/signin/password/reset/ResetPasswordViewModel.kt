package com.clerk.ui.signin.password.reset

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clerk.api.*
import com.clerk.ui.auth.*
import com.clerk.ui.core.common.displayMessage
import com.clerk.ui.core.common.runUiOperation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

internal class ResetPasswordViewModel(private val clerk: Clerk) : ViewModel() {
  private val _state = MutableStateFlow<AuthenticationViewState>(AuthenticationViewState.Idle)
  val state = _state.asStateFlow()

  fun setNewPassword(newPassword: String, signOutOtherSessions: Boolean) =
    guardSignIn(clerk, _state) { signIn ->
      _state.value = AuthenticationViewState.Loading
      viewModelScope.launch {
        runUiOperation {
            val params = SignInResetPasswordSubmitParams(newPassword, signOutOtherSessions)
            if (signIn.firstFactorVerification.strategy == "reset_password_phone_code")
              signIn.resetPasswordPhoneCode.submitPassword(params)
            else signIn.resetPasswordEmailCode.submitPassword(params)
          }
          .onSuccess { _state.value = AuthenticationViewState.Success.SignIn(signIn) }
          .onFailure { _state.value = AuthenticationViewState.Error(it.displayMessage) }
      }
    }

  fun completeSessionTask(newPassword: String, signOutOtherSessions: Boolean) {
    val user =
      clerk.user
        ?: run {
          _state.value = AuthenticationViewState.Error("User does not exist")
          return
        }
    val session = clerk.session ?: return
    _state.value = AuthenticationViewState.Loading
    viewModelScope.launch {
      runUiOperation {
          user.updatePassword(
            UpdateUserPasswordParams(
              newPassword = newPassword,
              signOutOfOtherSessions = signOutOtherSessions,
            )
          )
          session.reload()
        }
        .onSuccess {
          if (clerk.session?.id == session.id)
            _state.value = AuthenticationViewState.Success.SessionTaskComplete(session)
        }
        .onFailure { _state.value = AuthenticationViewState.Error(it.displayMessage) }
    }
  }

  fun resetState() {
    _state.value = AuthenticationViewState.Idle
  }
}
