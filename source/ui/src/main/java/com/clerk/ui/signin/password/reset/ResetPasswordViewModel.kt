package com.clerk.ui.signin.password.reset

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clerk.api.log.ClerkLog
import com.clerk.api.network.serialization.longErrorMessageOrNull
import com.clerk.api.network.serialization.onFailure
import com.clerk.api.network.serialization.onSuccess
import com.clerk.api.signin.SignIn
import com.clerk.api.signin.resetPassword
import com.clerk.ui.auth.AuthenticationViewState
import com.clerk.ui.auth.guardSignIn
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
          .resetPassword(
            SignIn.ResetPasswordParams(
              password = newPassword,
              signOutOfOtherSessions = signOutOtherSessions,
            )
          )
          .onSuccess {
            withContext(Dispatchers.Main) {
              _state.value = AuthenticationViewState.Success.SignIn(it)
            }
          }
          .onFailure {
            ClerkLog.e("ResetPasswordViewModel, ${it.longErrorMessageOrNull}")
            withContext(Dispatchers.Main) {
              _state.value =
                AuthenticationViewState.Error(it.longErrorMessageOrNull ?: "Something went wrong")
            }
          }
      }
    }

  fun resetState() {
    _state.value = AuthenticationViewState.Idle
  }
}
