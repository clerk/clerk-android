package com.clerk.ui.signin.password.set

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clerk.api.network.model.error.ClerkErrorResponse
import com.clerk.api.network.serialization.ClerkResult
import com.clerk.api.network.serialization.errorMessage
import com.clerk.api.network.serialization.onFailure
import com.clerk.api.network.serialization.onSuccess
import com.clerk.api.resetPasswordFactor
import com.clerk.api.signin.SignIn
import com.clerk.api.signin.verifyWithPassword
import com.clerk.ui.auth.AuthenticationViewState
import com.clerk.ui.auth.guardSignIn
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val PASSWORD_RESET_REQUIRED = "password_reset_required"

internal class SetPasswordViewModel(
  private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : ViewModel() {

  private val _state = MutableStateFlow<AuthenticationViewState>(AuthenticationViewState.Idle)
  val state = _state.asStateFlow()

  fun submitPassword(password: String) =
    guardSignIn(state = _state) { signIn ->
      _state.value = AuthenticationViewState.Loading
      viewModelScope.launch(ioDispatcher) {
        signIn
          .verifyWithPassword(password)
          .onSuccess {
            withContext(Dispatchers.Main) {
              _state.value = AuthenticationViewState.Success.SignIn(it)
            }
          }
          .onFailure {
            withContext(Dispatchers.Main) { _state.value = stateAfterFailure(it, signIn) }
          }
      }
    }

  fun resetState() {
    _state.value = AuthenticationViewState.Idle
  }

  private fun stateAfterFailure(
    failure: ClerkResult.Failure<ClerkErrorResponse>,
    signIn: SignIn,
  ): AuthenticationViewState {
    val requiresPasswordReset =
      failure.error?.errors.orEmpty().any { it.code == PASSWORD_RESET_REQUIRED }
    return if (requiresPasswordReset && signIn.resetPasswordFactor != null) {
      AuthenticationViewState.PasswordResetRequired
    } else {
      AuthenticationViewState.Error(failure.errorMessage)
    }
  }
}
